package com.cafe.integration;

import com.cafe.service.shared.OrderService;
import com.cafe.service.shared.InventoryService;
import com.cafe.common.BusinessException;
import com.cafe.common.TxnType;
import com.cafe.model.PrepBatch;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contract transaction quan trọng nhất của quầy pha chế, chạy với SQL Server thật. */
public class BaristaTransactionIT extends SqlServerIntegrationSupport {

    @Test
    void simultaneous_claim_allows_exactly_one_barista() throws Exception {
        Fixture f = fixture(false);
        List<Boolean> results = concurrently(() -> new OrderService().startItem(f.orderItemId, f.baristaOneId, f.branchId),
                () -> new OrderService().startItem(f.orderItemId, f.baristaTwoId, f.branchId));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals("MAKING", scalarString("SELECT Status FROM sales.OrderItem WHERE OrderItemId=?", f.orderItemId));
        assertEquals(1, scalarInt("SELECT COUNT(*) FROM ops.ActivityLog WHERE EntityType='ORDER_ITEM' AND EntityId=? AND ActionType='CLAIM'", f.orderItemId));
    }

    @Test
    void simultaneous_ready_deducts_recipe_and_modifier_once() throws Exception {
        Fixture f = fixture(true);
        OrderService service = new OrderService();
        assertTrue(service.startItem(f.orderItemId, f.baristaOneId, f.branchId));

        List<Boolean> results = concurrently(() -> new OrderService().markItemReady(f.orderItemId, f.baristaOneId, f.branchId),
                () -> new OrderService().markItemReady(f.orderItemId, f.baristaOneId, f.branchId));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals("READY", scalarString("SELECT Status FROM sales.OrderItem WHERE OrderItemId=?", f.orderItemId));
        // 2 ly × (10 RAW theo công thức + 2 RAW từ modifier) = 24; ledger phải chỉ có một lần trừ.
        assertEquals(new BigDecimal("-24.000"), scalarDecimal(
                "SELECT SUM(ChangeQty) FROM inventory.InventoryTransaction WHERE ReferenceType='ORDER_ITEM' AND ReferenceId=? AND TxnType='DEDUCT'", f.orderItemId));
        assertEquals(1, scalarInt("SELECT COUNT(*) FROM ops.ActivityLog WHERE EntityType='ORDER_ITEM' AND EntityId=? AND ActionType='COMPLETE'", f.orderItemId));
    }

    @Test
    void simultaneous_remake_creates_one_reservation_and_one_waste_event() throws Exception {
        Fixture f = fixture(false);
        OrderService service = new OrderService();
        assertTrue(service.startItem(f.orderItemId, f.baristaOneId, f.branchId));
        assertTrue(service.markItemReady(f.orderItemId, f.baristaOneId, f.branchId));

        List<Boolean> results = concurrently(() -> new OrderService().remakeItem(f.orderItemId, "Sai ly", f.baristaOneId, f.branchId),
                () -> new OrderService().remakeItem(f.orderItemId, "Sai ly", f.baristaOneId, f.branchId));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals("WAITING", scalarString("SELECT Status FROM sales.OrderItem WHERE OrderItemId=?", f.orderItemId));
        assertEquals(1, scalarInt("SELECT COUNT(*) FROM inventory.WasteEntry WHERE OrderItemId=? AND EventKind='REMAKE'", f.orderItemId));
    }

    @Test
    void voiding_manual_waste_keeps_audit_and_nets_ledger_to_zero() throws Exception {
        Fixture f = fixture(false);
        InventoryService inventory = new InventoryService();
        long wasteEntryId = inventory.logWaste(f.branchId, f.rawIngredientId, new BigDecimal("5"), "SPILL", "IT spill", f.baristaOneId);

        inventory.voidWaste(f.branchId, wasteEntryId, f.baristaOneId);

        assertEquals("VOIDED", scalarString("SELECT Status FROM inventory.WasteEntry WHERE WasteEntryId=?", wasteEntryId));
        assertEquals(new BigDecimal("0.000"), scalarDecimal(
                "SELECT SUM(ChangeQty) FROM inventory.InventoryTransaction WHERE ReferenceType='WASTE_ENTRY' AND ReferenceId=?", wasteEntryId));
        assertEquals(2, scalarInt("SELECT COUNT(*) FROM inventory.InventoryTransaction WHERE ReferenceType='WASTE_ENTRY' AND ReferenceId=?", wasteEntryId));
    }

    @Test
    void cancelling_prep_reverses_the_exact_ledger_entries() throws Exception {
        Fixture f = fixture(false);
        InventoryService inventory = new InventoryService();
        int prepBatchId = inventory.createPrepBatch(f.branchId, f.preppedIngredientId,
                new BigDecimal("100"), java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusHours(2), f.baristaOneId);

        assertTrue(inventory.cancelPrepBatch(f.branchId, prepBatchId, f.baristaOneId));
        assertFalse(inventory.cancelPrepBatch(f.branchId, prepBatchId, f.baristaOneId));
        assertEquals("CANCELLED", scalarString("SELECT Status FROM inventory.PrepBatch WHERE PrepBatchId=?", prepBatchId));
        assertEquals(new BigDecimal("0.000"), scalarDecimal(
                "SELECT SUM(ChangeQty) FROM inventory.InventoryTransaction WHERE ReferenceType='PREP_BATCH' AND ReferenceId=?", prepBatchId));
    }

    @Test
    void expired_prep_batch_can_be_written_off_only_once() throws Exception {
        Fixture f = fixture(false);
        InventoryService inventory = new InventoryService();
        int prepBatchId = inventory.createPrepBatch(f.branchId, f.preppedIngredientId,
                new BigDecimal("100"), java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusHours(1), f.baristaOneId);
        // Keep the historical batch lifecycle valid (MadeAt <= ExpiresAt), while making it expired now.
        execute("UPDATE inventory.PrepBatch "
                        + "SET MadeAt=DATEADD(hour,-2,SYSUTCDATETIME()), "
                        + "ExpiresAt=DATEADD(hour,-1,SYSUTCDATETIME()) WHERE PrepBatchId=?",
                prepBatchId);

        long wasteEntryId = inventory.writeOffExpiredPrepBatch(f.branchId, prepBatchId, new BigDecimal("100"), f.baristaOneId);

        assertTrue(wasteEntryId > 0);
        assertThrows(BusinessException.class, () -> inventory.writeOffExpiredPrepBatch(
                f.branchId, prepBatchId, new BigDecimal("100"), f.baristaOneId));
        assertEquals(1, scalarInt("SELECT COUNT(*) FROM inventory.WasteEntry WHERE WasteEntryId=?", wasteEntryId));
    }

    @Test
    void suggested_prep_is_idempotent_and_derives_expiry_from_ingredient() throws Exception {
        Fixture f = fixture(false);
        InventoryService inventory = new InventoryService();
        String requestId = UUID.randomUUID().toString();

        PrepBatch first = inventory.createSuggestedPrepBatch(f.branchId, f.preppedIngredientId,
                new BigDecimal("100"), f.baristaOneId, requestId);
        PrepBatch retry = inventory.createSuggestedPrepBatch(f.branchId, f.preppedIngredientId,
                new BigDecimal("100"), f.baristaOneId, requestId);

        assertEquals(first.getPrepBatchId(), retry.getPrepBatchId());
        assertEquals(1, scalarInt(
                "SELECT COUNT(*) FROM inventory.PrepBatch WHERE PrepBatchId=?", first.getPrepBatchId()));
        assertEquals(new BigDecimal("100.000"), scalarDecimal(
                "SELECT SUM(ChangeQty) FROM inventory.InventoryTransaction "
                        + "WHERE ReferenceType='PREP_BATCH' AND ReferenceId=? AND TxnType='PREP_IN'", first.getPrepBatchId()));
        assertEquals(new BigDecimal("-10.000"), scalarDecimal(
                "SELECT SUM(ChangeQty) FROM inventory.InventoryTransaction "
                        + "WHERE ReferenceType='PREP_BATCH' AND ReferenceId=? AND TxnType='PREP_OUT'", first.getPrepBatchId()));
        assertEquals(1, scalarInt(
                "SELECT CASE WHEN ExpiresAt IS NULL THEN 0 ELSE 1 END FROM inventory.PrepBatch WHERE PrepBatchId=?",
                first.getPrepBatchId()));
    }

    @Test
    void suggested_prep_rejects_incomplete_admin_or_manager_configuration() throws Exception {
        Fixture f = fixture(false);
        InventoryService inventory = new InventoryService();

        execute("UPDATE catalog.Ingredient SET ShelfLifeMinutes=NULL WHERE IngredientId=?",
                f.preppedIngredientId);
        assertThrows(BusinessException.class, () -> inventory.createSuggestedPrepBatch(
                f.branchId, f.preppedIngredientId, new BigDecimal("100"), f.baristaOneId,
                UUID.randomUUID().toString()));

        execute("UPDATE catalog.Ingredient SET ShelfLifeMinutes=1440 WHERE IngredientId=?",
                f.preppedIngredientId);
        execute("UPDATE inventory.BranchInventory SET PrepTargetQty=NULL WHERE BranchId=? AND IngredientId=?",
                f.branchId, f.preppedIngredientId);
        assertThrows(BusinessException.class, () -> inventory.createSuggestedPrepBatch(
                f.branchId, f.preppedIngredientId, new BigDecimal("100"), f.baristaOneId,
                UUID.randomUUID().toString()));

        execute("UPDATE inventory.BranchInventory SET PrepTargetQty=1000 WHERE BranchId=? AND IngredientId=?",
                f.branchId, f.preppedIngredientId);
        execute("DELETE FROM catalog.Recipe WHERE OwnerType='PREPPED' AND OwnerId=?", f.preppedIngredientId);
        assertThrows(BusinessException.class, () -> inventory.createSuggestedPrepBatch(
                f.branchId, f.preppedIngredientId, new BigDecimal("100"), f.baristaOneId,
                UUID.randomUUID().toString()));
    }

    @Test
    void simultaneous_prep_requests_cannot_make_raw_inventory_negative() throws Exception {
        Fixture f = fixture(false);
        execute("UPDATE inventory.BranchInventory SET QuantityOnHand=15 WHERE BranchId=? AND IngredientId=?",
                f.branchId, f.rawIngredientId);

        Callable<Boolean> first = () -> createPrepOrReject(f, f.baristaOneId);
        Callable<Boolean> second = () -> createPrepOrReject(f, f.baristaTwoId);
        List<Boolean> results = concurrently(first, second);

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(new BigDecimal("5.000"), scalarDecimal(
                "SELECT QuantityOnHand FROM inventory.BranchInventory WHERE BranchId="
                        + f.branchId + " AND IngredientId=?", f.rawIngredientId));
    }

    @Test
    void manager_can_cancel_unconsumed_batch_but_not_after_prepped_consumption() throws Exception {
        Fixture reversible = fixture(false);
        InventoryService inventory = new InventoryService();
        PrepBatch reversibleBatch = inventory.createSuggestedPrepBatch(
                reversible.branchId, reversible.preppedIngredientId, new BigDecimal("100"),
                reversible.baristaOneId, UUID.randomUUID().toString());
        assertTrue(inventory.cancelPrepBatchByManager(
                reversible.branchId, reversibleBatch.getPrepBatchId(), reversible.baristaOneId));
        assertEquals(new BigDecimal("0.000"), scalarDecimal(
                "SELECT SUM(ChangeQty) FROM inventory.InventoryTransaction "
                        + "WHERE ReferenceType='PREP_BATCH' AND ReferenceId=?", reversibleBatch.getPrepBatchId()));

        Fixture consumed = fixture(false);
        PrepBatch consumedBatch = inventory.createSuggestedPrepBatch(
                consumed.branchId, consumed.preppedIngredientId, new BigDecimal("100"),
                consumed.baristaOneId, UUID.randomUUID().toString());
        try (Connection conn = connection()) {
            conn.setAutoCommit(false);
            inventory.applyTxn(conn, consumed.branchId, consumed.preppedIngredientId,
                    new BigDecimal("-10"), TxnType.DEDUCT,
                    com.cafe.common.InventoryReferenceType.ORDER_ITEM,
                    (long) consumed.orderItemId, consumed.baristaOneId);
            conn.commit();
        }

        assertThrows(BusinessException.class, () -> inventory.cancelPrepBatchByManager(
                consumed.branchId, consumedBatch.getPrepBatchId(), consumed.baristaOneId));
        assertEquals("ACTIVE", scalarString(
                "SELECT Status FROM inventory.PrepBatch WHERE PrepBatchId=?", consumedBatch.getPrepBatchId()));
    }

    private static List<Boolean> concurrently(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Boolean> wrap = () -> { ready.countDown(); start.await(); return first.call(); };
            Future<Boolean> left = executor.submit(wrap);
            Future<Boolean> right = executor.submit(() -> { ready.countDown(); start.await(); return second.call(); });
            ready.await(); start.countDown();
            return List.of(left.get(), right.get());
        } finally { executor.shutdownNow(); }
    }

    private static boolean createPrepOrReject(Fixture fixture, int userId) throws Exception {
        try {
            new InventoryService().createSuggestedPrepBatch(
                    fixture.branchId, fixture.preppedIngredientId, new BigDecimal("100"),
                    userId, UUID.randomUUID().toString());
            return true;
        } catch (BusinessException expected) {
            return false;
        }
    }

    private Fixture fixture(boolean withModifier) throws Exception {
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection conn = connection(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES ('B" + key + "',N'IT Branch','00:00','23:59')");
            int branchId = id(conn, "SELECT BranchId FROM org.Branch WHERE Code=?", "B" + key);
            st.executeUpdate("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleCode,BranchId) VALUES ('b1" + key + "','x',N'Barista One','BARISTA'," + branchId + "),('b2" + key + "','x',N'Barista Two','BARISTA'," + branchId + ")");
            int one = id(conn, "SELECT UserId FROM iam.UserAccount WHERE Username=?", "b1" + key);
            int two = id(conn, "SELECT UserId FROM iam.UserAccount WHERE Username=?", "b2" + key);
            st.executeUpdate("INSERT catalog.Category(Name) VALUES (N'IT Category " + key + "')");
            int category = id(conn, "SELECT MAX(CategoryId) FROM catalog.Category");
            st.executeUpdate("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (" + category + ",N'IT Drink " + key + "',10000)");
            int product = id(conn, "SELECT MAX(ProductId) FROM catalog.Product");
            st.executeUpdate("INSERT catalog.Ingredient(Name,Unit,IngredientType,ShelfLifeMinutes) VALUES "
                    + "(N'IT Raw " + key + "',N'g','RAW',NULL),(N'IT Prepped " + key + "',N'ml','PREPPED',1440)");
            int ingredient = id(conn, "SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", "IT Raw " + key);
            int prepped = id(conn, "SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", "IT Prepped " + key);
            st.executeUpdate("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) VALUES ('PRODUCT'," + product + "," + ingredient + ",10)");
            st.executeUpdate("UPDATE catalog.Ingredient SET PrepYieldQty=100 WHERE IngredientId=" + prepped);
            st.executeUpdate("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) VALUES ('PREPPED'," + prepped + "," + ingredient + ",10)");
            st.executeUpdate("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold,PrepTargetQty) "
                    + "VALUES (" + branchId + "," + ingredient + ",1000,0,NULL),(" + branchId + "," + prepped + ",0,300,1000)");
            st.executeUpdate("INSERT sales.SalesOrder(BranchId,Source,OrderType,Status,CreatedBy,BusinessDate) VALUES ("
                    + branchId + ",'QR','TAKEAWAY','ACTIVE',NULL"
                    + ",CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME())))");
            int orderId = id(conn, "SELECT MAX(OrderId) FROM sales.SalesOrder");
            st.executeUpdate("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder) VALUES ("
                    + orderId + "," + branchId + "," + product + ",2,10000,'WAITING',N'IT Drink " + key + "')");
            int itemId = id(conn, "SELECT MAX(OrderItemId) FROM sales.OrderItem");
            if (withModifier) {
                st.executeUpdate("INSERT catalog.ModifierGroup(ProductId,Name,SortOrder) VALUES (" + product + ",N'IT Extra " + key + "',5)");
                int groupId = id(conn, "SELECT MAX(ModifierGroupId) FROM catalog.ModifierGroup");
                st.executeUpdate("INSERT catalog.ModifierOption(ModifierGroupId,Name,PriceDelta) VALUES (" + groupId + ",N'IT Extra " + key + "',0)");
                int optionId = id(conn, "SELECT MAX(ModifierOptionId) FROM catalog.ModifierOption");
                st.executeUpdate("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) VALUES ('MODIFIER'," + optionId + "," + ingredient + ",2)");
                st.executeUpdate("INSERT sales.OrderItemModifier(OrderItemId,ModifierOptionId,PriceDelta,ModifierOptionNameAtOrder) VALUES (" + itemId + "," + optionId + ",0,N'IT Extra " + key + "')");
            }
            return new Fixture(branchId, one, two, itemId, ingredient, prepped);
        }
    }

    private static int id(Connection conn, String sql, Object... values) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static int scalarInt(String sql, long id) throws Exception {
        try (Connection conn = connection()) { return id(conn, sql, id); }
    }

    private static void execute(String sql, Object... values) throws Exception {
        try (Connection conn = connection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            ps.executeUpdate();
        }
    }
    private static String scalarString(String sql, long id) throws Exception {
        try (Connection conn = connection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getString(1); } }
    }
    private static BigDecimal scalarDecimal(String sql, long id) throws Exception {
        try (Connection conn = connection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setLong(1, id); try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); } }
    }

    private record Fixture(int branchId, int baristaOneId, int baristaTwoId, int orderItemId,
                           int rawIngredientId, int preppedIngredientId) { }
}
