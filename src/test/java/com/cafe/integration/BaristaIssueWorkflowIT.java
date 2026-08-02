package com.cafe.integration;

import com.cafe.common.BusinessException;
import com.cafe.model.StockAdjustment;
import com.cafe.service.shared.KdsOrderWorkflowService;
import com.cafe.service.shared.OrderIssueService;
import com.cafe.service.shared.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Luồng SỰ CỐ và THAO TÁC CẢ ĐƠN của quầy pha chế, chạy với SQL Server thật.
 *
 * <p>Bổ sung cho {@link BaristaTransactionIT} — file đó phủ ba hợp đồng đồng thời (nhận trùng,
 * trừ kho hai lần, đặt chỗ tồn khi làm lại). Ở đây phủ phần còn lại của
 * {@code OrderIssueService} và {@code KdsOrderWorkflowService}: chặn/bỏ chặn, báo sự cố, thao tác
 * cả đơn, thu hồi món.
 *
 * <p>Trọng tâm là hai CHỐT AN TOÀN mà đọc code mới thấy, không lộ ra ở giao diện:
 * <ul>
 *   <li>báo hết nguyên liệu chỉ được nhận nguyên liệu THUỘC CÔNG THỨC của chính món đó — nếu không,
 *       một POST tự soạn ép được tồn của nguyên liệu bất kỳ về 0, kéo mọi món dùng nó biến mất
 *       khỏi POS/QR;</li>
 *   <li>kiểm kê lúc bỏ chặn cũng bị chốt y hệt.</li>
 * </ul>
 */
public class BaristaIssueWorkflowIT extends SqlServerIntegrationSupport {

    // ── Báo sự cố: chỉ gắn cờ, KHÔNG đẩy món khỏi hàng chờ ───────────────────────────────

    @Test
    void report_issue_flags_the_item_but_keeps_it_in_the_queue() throws Exception {
        Fixture f = fixture();
        assertTrue(new OrderService().reportItemIssue(f.itemOneId, "Không đáp ứng được ghi chú",
                f.baristaOneId, f.branchId));

        // Đây là điểm phân biệt với blockItem: món vẫn nằm trong hàng chờ để người khác pha tiếp.
        assertEquals("WAITING", itemStatus(f.itemOneId));
        assertEquals(1, scalarInt("SELECT CAST(HasIssue AS int) FROM sales.OrderItem WHERE OrderItemId=?", f.itemOneId));
        assertEquals("Không đáp ứng được ghi chú",
                scalarString("SELECT IssueReason FROM sales.OrderItem WHERE OrderItemId=?", f.itemOneId));
    }

    @Test
    void report_issue_rejects_an_empty_reason() throws Exception {
        Fixture f = fixture();
        assertThrows(IllegalArgumentException.class,
                () -> new OrderService().reportItemIssue(f.itemOneId, "   ", f.baristaOneId, f.branchId));
        assertEquals(0, scalarInt("SELECT CAST(HasIssue AS int) FROM sales.OrderItem WHERE OrderItemId=?", f.itemOneId));
    }

    // ── Chặn / bỏ chặn ───────────────────────────────────────────────────────────────────

    @Test
    void block_takes_the_item_out_of_the_queue_and_unblock_puts_it_back() throws Exception {
        Fixture f = fixture();
        OrderService service = new OrderService();
        assertTrue(service.startItem(f.itemOneId, f.baristaOneId, f.branchId));

        assertTrue(service.blockItem(f.itemOneId, "Máy móc gặp sự cố", f.baristaOneId, f.branchId));
        assertEquals("BLOCKED", itemStatus(f.itemOneId));
        // Nhả người pha ra: món bị chặn không được khoá dưới tên ai cả.
        assertNull(scalarObject("SELECT BaristaId FROM sales.OrderItem WHERE OrderItemId=?", f.itemOneId));

        assertTrue(service.unblockItem(f.itemOneId, f.baristaOneId, f.branchId));
        assertEquals("WAITING", itemStatus(f.itemOneId));
        assertEquals(0, scalarInt("SELECT CAST(HasIssue AS int) FROM sales.OrderItem WHERE OrderItemId=?", f.itemOneId));
        assertNull(scalarObject("SELECT IssueReason FROM sales.OrderItem WHERE OrderItemId=?", f.itemOneId));
    }

    @Test
    void unblock_does_nothing_when_the_item_is_not_blocked() throws Exception {
        Fixture f = fixture();
        assertFalse(new OrderService().unblockItem(f.itemOneId, f.baristaOneId, f.branchId));
        assertEquals("WAITING", itemStatus(f.itemOneId));
    }

    // ── Hết nguyên liệu: sửa sổ kho + chặn món trong CÙNG một transaction ─────────────────

    @Test
    void depleted_ingredient_zeroes_the_stock_and_blocks_the_item() throws Exception {
        Fixture f = fixture();
        assertEquals(0, new BigDecimal("1000").compareTo(onHand(f.branchId, f.rawIngredientId)));

        assertTrue(new OrderService().blockItemForDepletedIngredients(f.itemOneId,
                List.of(f.rawIngredientId), "Hết nguyên liệu", f.baristaOneId, f.branchId));

        assertEquals("BLOCKED", itemStatus(f.itemOneId));
        assertEquals(0, BigDecimal.ZERO.compareTo(onHand(f.branchId, f.rawIngredientId)));
    }

    /**
     * CHỐT AN TOÀN: nguyên liệu báo hết phải nằm trong công thức của chính món đó.
     * Thiếu chốt này thì một POST tự soạn ép được tồn của nguyên liệu bất kỳ ở chi nhánh về 0.
     */
    @Test
    void depleted_ingredient_rejects_an_ingredient_outside_the_product_recipe() throws Exception {
        Fixture f = fixture();

        assertThrows(BusinessException.class, () -> new OrderService().blockItemForDepletedIngredients(
                f.itemOneId, List.of(f.foreignIngredientId), "Hết nguyên liệu", f.baristaOneId, f.branchId));

        // Cả hai vế phải nguyên vẹn: món không bị chặn VÀ tồn của nguyên liệu lạ không bị đụng.
        assertEquals("WAITING", itemStatus(f.itemOneId));
        assertEquals(0, new BigDecimal("500").compareTo(onHand(f.branchId, f.foreignIngredientId)));
    }

    @Test
    void depleted_ingredient_requires_at_least_one_ingredient() throws Exception {
        Fixture f = fixture();
        assertThrows(IllegalArgumentException.class, () -> new OrderService().blockItemForDepletedIngredients(
                f.itemOneId, List.of(), "Hết nguyên liệu", f.baristaOneId, f.branchId));
        assertEquals("WAITING", itemStatus(f.itemOneId));
    }

    // ── Bỏ chặn kèm kiểm kê ──────────────────────────────────────────────────────────────

    @Test
    void unblock_with_recount_writes_actual_stock_and_counts_remaining_blocked() throws Exception {
        Fixture f = fixture();
        OrderService service = new OrderService();
        assertTrue(service.blockItem(f.itemOneId, "Máy móc gặp sự cố", f.baristaOneId, f.branchId));
        assertTrue(service.blockItem(f.itemTwoId, "Máy móc gặp sự cố", f.baristaOneId, f.branchId));

        OrderIssueService.UnblockResult result = service.unblockItem(f.itemOneId,
                List.of(recount(f.rawIngredientId, "250")), f.baristaOneId, f.branchId);

        assertTrue(result.isSuccess());
        assertEquals("WAITING", itemStatus(f.itemOneId));
        assertEquals(0, new BigDecimal("250").compareTo(onHand(f.branchId, f.rawIngredientId)));
        // Món thứ hai vẫn bị chặn và cũng dùng nguyên liệu vừa kiểm lại → nhắc barista xử lý nốt.
        assertEquals(1, result.getRemainingBlockedWithRecountedIngredients());
    }

    /** CHỐT AN TOÀN đối xứng với báo hết nguyên liệu — cùng lý do. */
    @Test
    void unblock_with_recount_rejects_an_ingredient_outside_the_product_recipe() throws Exception {
        Fixture f = fixture();
        assertTrue(new OrderService().blockItem(f.itemOneId, "Máy móc gặp sự cố", f.baristaOneId, f.branchId));

        assertThrows(BusinessException.class, () -> new OrderService().unblockItem(f.itemOneId,
                List.of(recount(f.foreignIngredientId, "77")), f.baristaOneId, f.branchId));

        assertEquals("BLOCKED", itemStatus(f.itemOneId));
        assertEquals(0, new BigDecimal("500").compareTo(onHand(f.branchId, f.foreignIngredientId)));
    }

    // ── Thao tác cả đơn ──────────────────────────────────────────────────────────────────

    @Test
    void start_all_in_order_claims_every_waiting_item() throws Exception {
        Fixture f = fixture();
        assertEquals(2, new OrderService().startAllInOrder(f.orderId, f.baristaOneId, f.branchId));

        assertEquals("MAKING", itemStatus(f.itemOneId));
        assertEquals("MAKING", itemStatus(f.itemTwoId));
        assertEquals(2, scalarInt(
                "SELECT COUNT(*) FROM sales.OrderItem WHERE OrderId=? AND BaristaId IS NOT NULL", f.orderId));
    }

    @Test
    void mark_order_ready_completes_only_the_items_claimed_by_this_barista() throws Exception {
        Fixture f = fixture();
        OrderService service = new OrderService();
        assertTrue(service.startItem(f.itemOneId, f.baristaOneId, f.branchId));
        assertTrue(service.startItem(f.itemTwoId, f.baristaTwoId, f.branchId));

        KdsOrderWorkflowService.BulkReadyResult result =
                service.markOrderReady(f.orderId, f.baristaOneId, f.branchId);

        assertEquals(1, result.getCompleted());
        assertEquals(0, result.getSkippedNoRecipe());
        assertEquals("READY", itemStatus(f.itemOneId));
        assertEquals("MAKING", itemStatus(f.itemTwoId));   // món của người khác không bị đụng
    }

    @Test
    void count_my_making_items_counts_only_this_barista() throws Exception {
        Fixture f = fixture();
        OrderService service = new OrderService();
        assertTrue(service.startItem(f.itemOneId, f.baristaOneId, f.branchId));
        assertTrue(service.startItem(f.itemTwoId, f.baristaTwoId, f.branchId));

        assertEquals(1, service.countMyMakingItems(f.branchId, f.baristaOneId));
        assertEquals(1, service.countMyMakingItems(f.branchId, f.baristaTwoId));
    }

    // ── Trả lại chờ / thu hồi ────────────────────────────────────────────────────────────

    @Test
    void return_to_queue_works_only_for_the_barista_holding_the_item() throws Exception {
        Fixture f = fixture();
        OrderService service = new OrderService();
        assertTrue(service.startItem(f.itemOneId, f.baristaOneId, f.branchId));

        assertFalse(service.returnItemToQueue(f.itemOneId, f.baristaTwoId, f.branchId));
        assertEquals("MAKING", itemStatus(f.itemOneId));

        assertTrue(service.returnItemToQueue(f.itemOneId, f.baristaOneId, f.branchId));
        assertEquals("WAITING", itemStatus(f.itemOneId));
    }

    /** Chủ món còn trực thì KHÔNG được giật món khỏi tay họ — phải nhờ họ tự trả lại. */
    @Test
    void reclaim_refuses_while_the_owner_is_still_on_duty() throws Exception {
        Fixture f = fixture();
        OrderService service = new OrderService();
        assertTrue(service.startItem(f.itemOneId, f.baristaOneId, f.branchId));

        assertThrows(BusinessException.class, () -> new OrderService().reclaimItem(
                f.itemOneId, f.baristaTwoId, f.branchId, "Barista Two", Set.of(f.baristaOneId)));

        assertEquals("MAKING", itemStatus(f.itemOneId));
    }

    @Test
    void reclaim_returns_the_item_when_the_owner_has_left_the_shift() throws Exception {
        Fixture f = fixture();
        OrderService service = new OrderService();
        assertTrue(service.startItem(f.itemOneId, f.baristaOneId, f.branchId));

        assertTrue(service.reclaimItem(f.itemOneId, f.baristaTwoId, f.branchId, "Barista Two", Set.of()));

        assertEquals("WAITING", itemStatus(f.itemOneId));
        assertNull(scalarObject("SELECT BaristaId FROM sales.OrderItem WHERE OrderItemId=?", f.itemOneId));
    }

    /** Món của chính mình thì dùng "Trả lại chờ", không phải "Thu hồi". */
    @Test
    void reclaim_refuses_an_item_the_actor_holds_themselves() throws Exception {
        Fixture f = fixture();
        OrderService service = new OrderService();
        assertTrue(service.startItem(f.itemOneId, f.baristaOneId, f.branchId));

        assertFalse(service.reclaimItem(f.itemOneId, f.baristaOneId, f.branchId, "Barista One", Set.of()));
        assertEquals("MAKING", itemStatus(f.itemOneId));
    }

    // ── Hạ tầng ──────────────────────────────────────────────────────────────────────────

    private static StockAdjustment recount(int ingredientId, String actualBaseQty) {
        StockAdjustment adjustment = new StockAdjustment();
        adjustment.setIngredientId(ingredientId);
        adjustment.setActualBaseQty(new BigDecimal(actualBaseQty));
        return adjustment;
    }

    /**
     * Một chi nhánh sạch cho mỗi test: hai barista, một món có công thức, một đơn hai dòng, và một
     * nguyên liệu "lạ" KHÔNG thuộc công thức để kiểm hai chốt an toàn.
     */
    private Fixture fixture() throws Exception {
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection conn = connection(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES ('B" + key + "',N'IT Branch','00:00','23:59')");
            int branchId = id(conn, "SELECT BranchId FROM org.Branch WHERE Code=?", "B" + key);

            st.executeUpdate("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleCode,BranchId) VALUES "
                    + "('b1" + key + "','x',N'Barista One','BARISTA'," + branchId + "),"
                    + "('b2" + key + "','x',N'Barista Two','BARISTA'," + branchId + ")");
            int one = id(conn, "SELECT UserId FROM iam.UserAccount WHERE Username=?", "b1" + key);
            int two = id(conn, "SELECT UserId FROM iam.UserAccount WHERE Username=?", "b2" + key);

            st.executeUpdate("INSERT catalog.Category(Name) VALUES (N'IT Category " + key + "')");
            int category = id(conn, "SELECT CategoryId FROM catalog.Category WHERE Name=?", "IT Category " + key);
            st.executeUpdate("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (" + category + ",N'IT Drink " + key + "',10000)");
            int product = id(conn, "SELECT ProductId FROM catalog.Product WHERE Name=?", "IT Drink " + key);

            st.executeUpdate("INSERT catalog.Ingredient(Name,Unit,IngredientType,ShelfLifeMinutes) VALUES "
                    + "(N'IT Raw " + key + "',N'g','RAW',NULL),(N'IT Foreign " + key + "',N'g','RAW',NULL)");
            int raw = id(conn, "SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", "IT Raw " + key);
            int foreign = id(conn, "SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", "IT Foreign " + key);

            // CHỈ nguyên liệu "raw" nằm trong công thức — "foreign" cố ý đứng ngoài.
            st.executeUpdate("INSERT catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) VALUES ('PRODUCT'," + product + "," + raw + ",10)");
            st.executeUpdate("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold,PrepTargetQty) VALUES "
                    + "(" + branchId + "," + raw + ",1000,0,NULL),(" + branchId + "," + foreign + ",500,0,NULL)");

            st.executeUpdate("INSERT sales.SalesOrder(BranchId,Source,OrderType,Status,CreatedBy,BusinessDate) VALUES ("
                    + branchId + ",'QR','TAKEAWAY','ACTIVE',NULL,CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME())))");
            // Chi nhánh vừa tạo là duy nhất của test này nên MAX theo BranchId không thể lấy nhầm
            // đơn của test khác — khác với MAX toàn bảng.
            int orderId = id(conn, "SELECT MAX(OrderId) FROM sales.SalesOrder WHERE BranchId=?", branchId);

            st.executeUpdate("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder) VALUES "
                    + "(" + orderId + "," + branchId + "," + product + ",2,10000,'WAITING',N'IT Drink " + key + "'),"
                    + "(" + orderId + "," + branchId + "," + product + ",1,10000,'WAITING',N'IT Drink " + key + "')");
            int itemOne = id(conn, "SELECT MIN(OrderItemId) FROM sales.OrderItem WHERE OrderId=?", orderId);
            int itemTwo = id(conn, "SELECT MAX(OrderItemId) FROM sales.OrderItem WHERE OrderId=?", orderId);

            return new Fixture(branchId, one, two, orderId, itemOne, itemTwo, raw, foreign);
        }
    }

    private static String itemStatus(int orderItemId) throws Exception {
        return scalarString("SELECT Status FROM sales.OrderItem WHERE OrderItemId=?", orderItemId);
    }

    private static BigDecimal onHand(int branchId, int ingredientId) throws Exception {
        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT QuantityOnHand FROM inventory.BranchInventory WHERE BranchId=? AND IngredientId=?")) {
            ps.setInt(1, branchId);
            ps.setInt(2, ingredientId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getBigDecimal(1); }
        }
    }

    private static int id(Connection conn, String sql, Object... values) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) ps.setObject(i + 1, values[i]);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static int scalarInt(String sql, int value) throws Exception {
        try (Connection conn = connection()) { return id(conn, sql, value); }
    }

    private static String scalarString(String sql, int value) throws Exception {
        try (Connection conn = connection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getString(1); }
        }
    }

    /** Dùng cho cột cho phép NULL — {@code getInt} trả 0 nên không phân biệt được NULL với 0. */
    private static Object scalarObject(String sql, int value) throws Exception {
        try (Connection conn = connection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, value);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getObject(1); }
        }
    }

    private record Fixture(int branchId, int baristaOneId, int baristaTwoId, int orderId,
                           int itemOneId, int itemTwoId, int rawIngredientId, int foreignIngredientId) { }
}
