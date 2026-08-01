package com.cafe.integration;

import com.cafe.common.BusinessDay;
import com.cafe.common.BusinessException;
import com.cafe.config.SchemaVersionGuard;
import com.cafe.dao.admin.HomeSettingDao;
import com.cafe.dao.cashier.BillItemDao;
import com.cafe.dao.shared.OrderItemModifierDao;
import com.cafe.model.BillItem;
import com.cafe.model.HomeSetting;
import com.cafe.model.OrderItemModifier;
import com.cafe.model.Payroll;
import com.cafe.model.Product;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.admin.ProductService;
import com.cafe.service.admin.ReportService;
import com.cafe.service.manager.PayrollService;
import com.cafe.service.manager.StockReceiptService;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.shared.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Nghiệm thu các release Flyway/unit snapshot/payroll/UTC đã khóa trong database plan. */
public class DatabaseNormalizationIT extends SqlServerIntegrationSupport {

    @Test
    void home_setting_reads_updates_and_inserts_singleton_with_canonical_id() throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                HomeSettingDao dao = new HomeSettingDao();
                assertNotNull(dao.find(connection));

                HomeSetting updated = home("eyebrow-update", "title-update");
                dao.update(connection, updated);
                assertEquals("title-update", dao.find(connection).getHeroTitle());
                assertEquals(1, scalarInt(connection,
                        "SELECT COUNT(*) FROM catalog.HomeSetting WHERE HomeSettingId=1"));

                execute(connection, "DELETE FROM catalog.HomeSetting WHERE HomeSettingId=1");
                assertNull(dao.find(connection));
                HomeSetting inserted = home("eyebrow-insert", "title-insert");
                dao.update(connection, inserted);
                assertEquals("title-insert", dao.find(connection).getHeroTitle());
                assertEquals(1, scalarInt(connection,
                        "SELECT COUNT(*) FROM catalog.HomeSetting WHERE HomeSettingId=1"));
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void concurrent_waste_review_resolve_creates_exactly_one_event_level_audit() throws Exception {
        int branchId = createBranch();
        int managerOne = createUser(branchId, "BRANCH_MANAGER");
        int managerTwo = createUser(branchId, "BRANCH_MANAGER");
        int barista = createUser(branchId, "BARISTA");
        int ingredientId = createIngredient("review-ingredient", "g");
        String cause = unique("review");
        execute("INSERT inventory.WasteEvent(BranchId,EventKind,Source,CauseCode,CreatedBy) "
                        + "VALUES (?,'INGREDIENT_WASTE','MANUAL',?,?)",
                branchId, cause, barista);
        long eventId = scalarLong(
                "SELECT WasteEventId FROM inventory.WasteEvent WHERE BranchId=? AND CauseCode=?",
                branchId, cause);
        execute("INSERT inventory.WasteEventReview(WasteEventId,IngredientId,ReviewType,QtyBefore,QtyAfter) "
                        + "VALUES (?,?,'SOFT_NEGATIVE',1,-1)", eventId, ingredientId);
        long reviewId = scalarLong(
                "SELECT WasteEventReviewId FROM inventory.WasteEventReview WHERE WasteEventId=?", eventId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = resolveAsync(executor, ready, start, branchId, reviewId, managerOne);
            Future<Boolean> second = resolveAsync(executor, ready, start, branchId, reviewId, managerTwo);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, successes);
            assertEquals("RESOLVED", scalarString(
                    "SELECT Status FROM inventory.WasteEventReview WHERE WasteEventReviewId=?", reviewId));
            assertEquals(1, scalarInt(
                    "SELECT COUNT(*) FROM inventory.WasteEventAudit "
                            + "WHERE WasteEventId=? AND WasteEventItemId IS NULL "
                            + "AND ActionType='REVIEW' AND AfterValue='RESOLVED'", eventId));
            assertTrue(new InventoryService().getWasteCorrections(
                    branchId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), 100)
                    .stream().anyMatch(a -> a.getWasteEventId() != null
                            && a.getWasteEventId() == eventId && "REVIEW".equals(a.getActionType())));
        } finally {
            executor.shutdownNow();
        }

        execute("INSERT inventory.WasteEventReview(WasteEventId,IngredientId,ReviewType,QtyBefore,QtyAfter) "
                + "VALUES (?,?,'SOFT_NEGATIVE',1,-1)", eventId, ingredientId);
        long rollbackReviewId = scalarLong(
                "SELECT MAX(WasteEventReviewId) FROM inventory.WasteEventReview WHERE WasteEventId=?", eventId);
        execute("CREATE TRIGGER inventory.TR_IT_FailReviewAudit ON inventory.WasteEventAudit AFTER INSERT AS "
                + "BEGIN SET NOCOUNT ON; IF EXISTS(SELECT 1 FROM inserted WHERE ActionType='REVIEW') "
                + "THROW 51999,'Forced review audit failure',1; END");
        try {
            assertThrows(SQLException.class, () -> new InventoryService().resolveWasteReview(
                    branchId, rollbackReviewId, managerOne, "must roll back"));
        } finally {
            execute("DROP TRIGGER IF EXISTS inventory.TR_IT_FailReviewAudit");
        }
        assertEquals("OPEN", scalarString(
                "SELECT Status FROM inventory.WasteEventReview WHERE WasteEventReviewId=?", rollbackReviewId));
        assertEquals(0, scalarInt(
                "SELECT COUNT(*) FROM inventory.WasteEventAudit WHERE WasteEventId=? "
                        + "AND ActionType='REVIEW' AND Reason=N'must roll back'", eventId));
    }

    @Test
    void receipt_uses_factor_snapshot_and_rejects_inactive_wrong_or_imprecise_conversion() throws Exception {
        int branchId = createBranch();
        int managerId = createUser(branchId, "BRANCH_MANAGER");
        int ingredientId = createIngredient("receipt-unit", "g");
        execute("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold) "
                + "VALUES (?,?,0,0)", branchId, ingredientId);
        IngredientService ingredients = new IngredientService();
        int bagId = ingredients.addUnitConversion(
                ingredientId, "Bag", new BigDecimal("1000"), managerId);
        int receiptId = createReceipt(branchId, managerId);
        StockReceiptService receipts = new StockReceiptService();
        receipts.addReceiptLine(receiptId, branchId, ingredientId,
                new BigDecimal("2"), new BigDecimal("5000"), bagId);

        ingredients.updateUnitConversion(
                bagId, ingredientId, "Bag", new BigDecimal("900"), true, managerId);
        ingredients.deactivateUnitConversion(bagId, ingredientId, managerId);
        assertEquals(new BigDecimal("1000.000000"), scalarDecimal(
                "SELECT FactorToBaseAtEntry FROM inventory.StockReceiptDetail WHERE StockReceiptId=?",
                receiptId));
        assertEquals(new BigDecimal("2000.000"), scalarDecimal(
                "SELECT BaseQuantity FROM inventory.StockReceiptDetail WHERE StockReceiptId=?", receiptId));

        receipts.confirmReceipt(receiptId, branchId, managerId);
        assertEquals(new BigDecimal("2000.000"), scalarDecimal(
                "SELECT QuantityOnHand FROM inventory.BranchInventory WHERE BranchId=? AND IngredientId=?",
                branchId, ingredientId));
        assertEquals(new BigDecimal("2000.000"), scalarDecimal(
                "SELECT ChangeQty FROM inventory.InventoryTransaction "
                        + "WHERE ReferenceType='STOCK_RECEIPT' AND ReferenceId=?", receiptId));
        assertEquals(new BigDecimal("10000.00"), scalarDecimal(
                "SELECT TotalCost FROM inventory.StockReceipt WHERE StockReceiptId=?", receiptId));

        int inactiveDraft = createReceipt(branchId, managerId);
        assertThrows(BusinessException.class, () -> receipts.addReceiptLine(
                inactiveDraft, branchId, ingredientId, BigDecimal.ONE, BigDecimal.ONE, bagId));

        int otherIngredient = createIngredient("wrong-unit", "g");
        int wrongDraft = createReceipt(branchId, managerId);
        assertThrows(BusinessException.class, () -> receipts.addReceiptLine(
                wrongDraft, branchId, otherIngredient, BigDecimal.ONE, BigDecimal.ONE, bagId));

        int tinyId = ingredients.addUnitConversion(
                ingredientId, "Tiny", new BigDecimal("0.333333"), managerId);
        int precisionDraft = createReceipt(branchId, managerId);
        assertThrows(BusinessException.class, () -> receipts.addReceiptLine(
                precisionDraft, branchId, ingredientId,
                new BigDecimal("0.001"), BigDecimal.ONE, tinyId));
        assertEquals(0, scalarInt(
                "SELECT COUNT(*) FROM inventory.StockReceiptDetail WHERE StockReceiptId=?", precisionDraft));

        int baseId = scalarInt("SELECT IngredientUnitConversionId "
                + "FROM catalog.IngredientUnitConversion WHERE IngredientId=? AND IsBaseUnit=1", ingredientId);
        assertThrows(SQLException.class, () -> execute(
                "UPDATE catalog.IngredientUnitConversion SET IsBaseUnit=0 "
                        + "WHERE IngredientUnitConversionId=?", baseId));
    }

    @Test
    void global_modifier_group_uniqueness_keeps_product_specific_size_prices() throws Exception {
        String categoryName = unique("size-category");
        execute("INSERT catalog.Category(Name) VALUES (?)", categoryName);
        int categoryId = scalarInt("SELECT CategoryId FROM catalog.Category WHERE Name=?", categoryName);
        ProductService service = new ProductService();

        Product first = new Product();
        first.setCategoryId(categoryId);
        first.setName(unique("size-product-a"));
        first.setBasePrice(new BigDecimal("25000"));
        ProductService.ProductSizeConfig firstConfig = ProductService.ProductSizeConfig.defaults();
        firstConfig.setSizeMDelta(new BigDecimal("3000"));
        firstConfig.setSizeLDelta(new BigDecimal("7000"));
        int firstId = service.createProduct(first, firstConfig);

        Product second = new Product();
        second.setCategoryId(categoryId);
        second.setName(unique("size-product-b"));
        second.setBasePrice(new BigDecimal("30000"));
        ProductService.ProductSizeConfig secondConfig = ProductService.ProductSizeConfig.defaults();
        secondConfig.setSizeMDelta(new BigDecimal("5000"));
        secondConfig.setSizeLDelta(new BigDecimal("9000"));
        int secondId = service.createProduct(second, secondConfig);

        assertEquals(new BigDecimal("3000.00"), service.getSizeConfig(firstId).getSizeMDelta());
        assertEquals(new BigDecimal("9000.00"), service.getSizeConfig(secondId).getSizeLDelta());
        assertEquals(2, scalarInt(
                "SELECT COUNT(DISTINCT g.Name) FROM catalog.ProductModifierGroup pmg "
                        + "JOIN catalog.ModifierGroup g ON g.ModifierGroupId=pmg.ModifierGroupId "
                        + "WHERE pmg.ProductId IN(?,?) AND g.Name LIKE N'Size sản phẩm #%'",
                firstId, secondId));
    }

    @Test
    void stock_count_in_package_unit_writes_base_actual_and_base_diff() throws Exception {
        int branchId = createBranch();
        int managerId = createUser(branchId, "BRANCH_MANAGER");
        int ingredientId = createIngredient("count-unit", "g");
        execute("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold) "
                + "VALUES (?,?,2500,0)", branchId, ingredientId);
        int bagId = new IngredientService().addUnitConversion(
                ingredientId, "Bag", new BigDecimal("1000"), managerId);

        new InventoryService().createAdjustment(
                branchId, ingredientId, new BigDecimal("2"), bagId, "package count", managerId);

        assertEquals(new BigDecimal("2000.000"), scalarDecimal(
                "SELECT QuantityOnHand FROM inventory.BranchInventory WHERE BranchId=? AND IngredientId=?",
                branchId, ingredientId));
        assertEquals(new BigDecimal("2.000000"), scalarDecimal(
                "SELECT TOP (1) CountedQuantity FROM inventory.StockAdjustment "
                        + "WHERE BranchId=? AND IngredientId=? ORDER BY StockAdjustmentId DESC",
                branchId, ingredientId));
        assertEquals(new BigDecimal("2000.000"), scalarDecimal(
                "SELECT TOP (1) ActualBaseQty FROM inventory.StockAdjustment "
                        + "WHERE BranchId=? AND IngredientId=? ORDER BY StockAdjustmentId DESC",
                branchId, ingredientId));
        assertEquals(new BigDecimal("-500.000"), scalarDecimal(
                "SELECT TOP (1) ChangeQty FROM inventory.InventoryTransaction "
                        + "WHERE BranchId=? AND IngredientId=? AND ReferenceType='STOCK_ADJUSTMENT' "
                        + "ORDER BY InventoryTransactionId DESC", branchId, ingredientId));
    }

    @Test
    void branch_transfer_preserves_receipt_and_allows_two_payrolls_in_same_month() throws Exception {
        int branchA = createBranch();
        int branchB = createBranch();
        int approverA = createUser(branchA, "BRANCH_MANAGER");
        int approverB = createUser(branchB, "BRANCH_MANAGER");
        int transferredManager = createUser(branchA, "BRANCH_MANAGER");
        YearMonth month = YearMonth.of(2025, 3);

        createApprovedAttendance(branchA, transferredManager, approverA, month, "A");
        Payroll lineA = payroll(transferredManager);
        new PayrollService().savePayroll(branchA, month, List.of(lineA), approverA);
        int receiptId = createReceipt(branchA, transferredManager);

        execute("UPDATE iam.UserAccount SET BranchId=? WHERE UserId=?", branchB, transferredManager);
        execute("UPDATE inventory.StockReceipt SET Note=N'history remains editable' WHERE StockReceiptId=?",
                receiptId);
        assertEquals(branchA, scalarInt(
                "SELECT BranchId FROM inventory.StockReceipt WHERE StockReceiptId=?", receiptId));
        assertEquals(transferredManager, scalarInt(
                "SELECT ReceivedBy FROM inventory.StockReceipt WHERE StockReceiptId=?", receiptId));
        assertEquals("history remains editable", scalarString(
                "SELECT Note FROM inventory.StockReceipt WHERE StockReceiptId=?", receiptId));
        assertThrows(SQLException.class, () -> execute(
                "UPDATE inventory.StockReceipt SET ReceivedBy=? WHERE StockReceiptId=?",
                approverA, receiptId));
        assertEquals(1, scalarInt(
                "SELECT COUNT(*) FROM hr.Payroll WHERE BranchId=? AND UserId=? AND PayrollMonth=?",
                branchA, transferredManager, java.sql.Date.valueOf(month.atDay(1))));

        Payroll amended = payroll(transferredManager);
        amended.setWorkedHours(new BigDecimal("15"));
        amended.setHourlyRate(new BigDecimal("30000"));
        new PayrollService().savePayroll(branchA, month, List.of(amended), approverA);
        assertEquals(new BigDecimal("15.00"), scalarDecimal(
                "SELECT WorkedHours FROM hr.Payroll WHERE BranchId=? AND UserId=? AND PayrollMonth=?",
                branchA, transferredManager, java.sql.Date.valueOf(month.atDay(1))));

        createApprovedAttendance(branchB, transferredManager, approverB, month, "B");
        new PayrollService().savePayroll(branchB, month, List.of(payroll(transferredManager)), approverB);
        assertEquals(2, scalarInt(
                "SELECT COUNT(*) FROM hr.Payroll WHERE UserId=? AND PayrollMonth=?",
                transferredManager, java.sql.Date.valueOf(month.atDay(1))));
        assertThrows(SQLException.class, () -> execute(
                "INSERT hr.Payroll(BranchId,UserId,PayrollMonth,WorkedHours,HourlyRate,UpdatedBy) "
                        + "VALUES (?,?,?,10,25000,?)",
                branchB, transferredManager, java.sql.Date.valueOf(month.atDay(1)), approverB));
    }

    @Test
    void order_and_bill_history_keep_product_and_modifier_names_after_catalog_rename() throws Exception {
        int branchId = createBranch();
        int cashierId = createUser(branchId, "CASHIER");
        String categoryName = unique("snapshot-category");
        execute("INSERT catalog.Category(Name) VALUES (?)", categoryName);
        int categoryId = scalarInt("SELECT CategoryId FROM catalog.Category WHERE Name=?", categoryName);
        String productName = unique("Original product");
        execute("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (?,?,100)", categoryId, productName);
        int productId = scalarInt("SELECT ProductId FROM catalog.Product WHERE Name=?", productName);
        execute("INSERT catalog.BranchMenu(BranchId,ProductId,IsListed,IsTemporarilyUnavailable) "
                + "VALUES (?,?,1,0)", branchId, productId);
        String groupName = unique("snapshot-group");
        String optionName = unique("Original option");
        execute("INSERT catalog.ModifierGroup(Name,IsRequired,MinSelect,MaxSelect) VALUES (?,0,0,1)", groupName);
        int groupId = scalarInt("SELECT ModifierGroupId FROM catalog.ModifierGroup WHERE Name=?", groupName);
        execute("INSERT catalog.ModifierOption(ModifierGroupId,Name,PriceDelta) VALUES (?,?,5)",
                groupId, optionName);
        int optionId = scalarInt(
                "SELECT ModifierOptionId FROM catalog.ModifierOption WHERE ModifierGroupId=? AND Name=?",
                groupId, optionName);
        execute("INSERT catalog.ProductModifierGroup(ProductId,ModifierGroupId) VALUES (?,?)",
                productId, groupId);

        OrderService.CartLine line = new OrderService.CartLine();
        line.productId = productId;
        line.quantity = 1;
        line.optionIds = List.of(optionId);
        int orderId = new OrderService().placeOrder(
                branchId, null, "COUNTER", "TAKEAWAY", cashierId, List.of(line));
        int itemId = scalarInt("SELECT OrderItemId FROM sales.OrderItem WHERE OrderId=?", orderId);
        execute("UPDATE catalog.Product SET Name=? WHERE ProductId=?", unique("Renamed product"), productId);
        execute("UPDATE catalog.ModifierOption SET Name=? WHERE ModifierOptionId=?",
                unique("Renamed option"), optionId);

        assertEquals(productName, scalarString(
                "SELECT ProductNameAtOrder FROM sales.OrderItem WHERE OrderItemId=?", itemId));
        assertEquals(optionName, scalarString(
                "SELECT ModifierOptionNameAtOrder FROM sales.OrderItemModifier WHERE OrderItemId=?", itemId));
        try (Connection connection = connection()) {
            List<OrderItemModifier> modifiers = new OrderItemModifierDao().findByItem(connection, itemId);
            assertEquals(optionName, modifiers.get(0).getOptionName());
        }

        execute("INSERT payment.Bill(BranchId,Subtotal,TotalAmount,Status) VALUES (?,105,105,'UNPAID')",
                branchId);
        int billId = scalarInt("SELECT MAX(BillId) FROM payment.Bill WHERE BranchId=?", branchId);
        execute("INSERT payment.BillItem(BillId,BranchId,OrderItemId,Amount) VALUES (?,?,?,105)",
                billId, branchId, itemId);
        try (Connection connection = connection()) {
            List<BillItem> items = new BillItemDao().findByBill(connection, billId);
            assertEquals(productName, items.get(0).getProductName());
        }
    }

    @Test
    void reports_filter_and_group_all_required_vietnam_time_boundaries() throws Exception {
        int branchId = createBranch();
        int cashierId = createUser(branchId, "CASHIER");
        execute("INSERT payment.CashierShift(BranchId,CashierId,OpeningCash) VALUES (?,?,0)",
                branchId, cashierId);
        int shiftId = scalarInt(
                "SELECT MAX(CashierShiftId) FROM payment.CashierShift WHERE BranchId=?", branchId);
        LocalDate day = LocalDate.of(2042, 6, 15);
        execute("DELETE FROM payment.Bill WHERE PaidAt>=? AND PaidAt<?",
                java.sql.Timestamp.valueOf(BusinessDay.vnDayStartUtc(day)),
                java.sql.Timestamp.valueOf(BusinessDay.vnDayEndExclusiveUtc(day.plusDays(1))));
        List<LocalDateTime> vietnamTimes = List.of(
                day.atTime(0, 0), day.atTime(0, 30), day.atTime(6, 59),
                day.atTime(7, 0), day.atTime(23, 59));
        for (int i = 0; i < vietnamTimes.size(); i++) {
            insertPaidBill(branchId, shiftId, new BigDecimal(10 + i),
                    BusinessDay.toUtc(vietnamTimes.get(i)));
        }
        insertPaidBill(branchId, shiftId, new BigDecimal("999"),
                BusinessDay.toUtc(day.plusDays(1).atStartOfDay()));

        var rows = new ReportService().getDailyRevenue(day, day);
        assertEquals(1, rows.size());
        assertEquals(day.toString(), rows.get(0).getLabel());
        assertEquals(5, rows.get(0).getCount());
        assertEquals(new BigDecimal("60.00"), rows.get(0).getAmount());
    }

    @Test
    void database_rejects_invalid_references_multirow_waste_and_unique_name_race() throws Exception {
        int branchId = createBranch();
        int managerId = createUser(branchId, "BRANCH_MANAGER");
        int baristaId = createUser(branchId, "BARISTA");
        int ingredientId = createIngredient("integrity-ingredient", "g");
        assertThrows(SQLException.class, () -> execute(
                "INSERT inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,"
                        + "ReferenceType,ReferenceId,CreatedBy) "
                        + "VALUES (?,?,1,'ADJUST','ORDER_ITEM',999999999,?)",
                branchId, ingredientId, managerId));
        assertThrows(SQLException.class, () -> execute(
                "INSERT inventory.InventoryTransaction(BranchId,IngredientId,ChangeQty,TxnType,"
                        + "ReferenceType,ReferenceId,CreatedBy) "
                        + "VALUES (?,?,1,'ADJUST','OrderItem',1,?)",
                branchId, ingredientId, managerId));

        String cause = unique("remake");
        execute("INSERT inventory.WasteEvent(BranchId,EventKind,Source,CauseCode,CreatedBy) "
                        + "VALUES (?,'INGREDIENT_WASTE','MANUAL',?,?)", branchId, cause, baristaId);
        long eventId = scalarLong(
                "SELECT WasteEventId FROM inventory.WasteEvent WHERE BranchId=? AND CauseCode=?",
                branchId, cause);
        assertThrows(SQLException.class, () -> execute(
                "INSERT inventory.WasteEventItem(WasteEventId,BranchId,IngredientId,Quantity,WasteType,LoggedBy) "
                        + "VALUES (?,?,?,1,'SPILL',?),(?,?,?,1,'REMAKE',?)",
                eventId, branchId, ingredientId, baristaId,
                eventId, branchId, ingredientId, baristaId));
        assertEquals(0, scalarInt(
                "SELECT COUNT(*) FROM inventory.WasteEventItem WHERE WasteEventId=?", eventId));
        execute("INSERT inventory.WasteEventItem(WasteEventId,BranchId,IngredientId,Quantity,WasteType,LoggedBy) "
                        + "VALUES (?,?,?,1,'SPILL',?)",
                eventId, branchId, ingredientId, baristaId);
        assertThrows(SQLException.class, () -> execute(
                "UPDATE inventory.WasteEvent SET EventKind='REMAKE' WHERE WasteEventId=?", eventId));
        assertEquals("INGREDIENT_WASTE", scalarString(
                "SELECT EventKind FROM inventory.WasteEvent WHERE WasteEventId=?", eventId));

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String accented = "  Cà phê " + suffix + "  ";
        String plainUpper = "CA PHE " + suffix.toUpperCase();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = insertCategoryAsync(executor, ready, start, accented);
            Future<Boolean> second = insertCategoryAsync(executor, ready, start, plainUpper);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, scalarInt(
                    "SELECT COUNT(*) FROM catalog.Category WHERE NameKey="
                            + "UPPER(LTRIM(RTRIM(?))) COLLATE Latin1_General_100_CI_AI", accented));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void schema_guard_reports_current_and_missing_history() throws Exception {
        try (Connection current = connection()) {
            SchemaVersionGuard.Status status = SchemaVersionGuard.check(current);
            assertTrue(status.up());
            assertEquals("1", status.expectedVersion());
            assertEquals("1", status.actualVersion());
        }
        String masterUrl = cafeJdbcUrl().replaceFirst(
                "(?i)databaseName=[^;]+", "databaseName=master");
        try (Connection master = DriverManager.getConnection(
                masterUrl, databaseUsername(), databasePassword())) {
            SchemaVersionGuard.Status status = SchemaVersionGuard.check(master);
            assertFalse(status.up());
            assertEquals("1", status.expectedVersion());
            assertNull(status.actualVersion());
        }
    }

    private Future<Boolean> resolveAsync(ExecutorService executor, CountDownLatch ready,
                                         CountDownLatch start, int branchId,
                                         long reviewId, int managerId) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            return new InventoryService().resolveWasteReview(
                    branchId, reviewId, managerId, "resolved concurrently");
        });
    }

    private Future<Boolean> insertCategoryAsync(ExecutorService executor, CountDownLatch ready,
                                                CountDownLatch start, String name) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                    "INSERT catalog.Category(Name) VALUES (?)")) {
                statement.setString(1, name);
                statement.executeUpdate();
                return true;
            } catch (SQLException expected) {
                return false;
            }
        });
    }

    private void createApprovedAttendance(int branchId, int userId, int approvedBy,
                                          YearMonth month, String suffix) throws SQLException {
        String template = unique("payroll-shift-" + suffix);
        execute("INSERT hr.ShiftTemplate(BranchId,Name,StartTime,EndTime) VALUES (?,?,'08:00','18:00')",
                branchId, template);
        int templateId = scalarInt(
                "SELECT ShiftTemplateId FROM hr.ShiftTemplate WHERE BranchId=? AND Name=?",
                branchId, template);
        execute("INSERT hr.ShiftAssignment(ShiftTemplateId,BranchId,UserId,WorkDate) VALUES (?,?,?,?)",
                templateId, branchId, userId, java.sql.Date.valueOf(month.atDay(10)));
        int assignmentId = scalarInt(
                "SELECT MAX(ShiftAssignmentId) FROM hr.ShiftAssignment WHERE ShiftTemplateId=? AND UserId=?",
                templateId, userId);
        LocalDateTime in = BusinessDay.toUtc(month.atDay(10).atTime(8, 0));
        execute("INSERT hr.Attendance(ShiftAssignmentId,CheckInAt,CheckOutAt,Status,ApprovedBy,ApprovedAt) "
                        + "VALUES (?,?,?,'APPROVED',?,SYSUTCDATETIME())",
                assignmentId, java.sql.Timestamp.valueOf(in),
                java.sql.Timestamp.valueOf(in.plusHours(10)), approvedBy);
    }

    private void insertPaidBill(int branchId, int cashierShiftId,
                                BigDecimal amount, LocalDateTime paidAt) throws SQLException {
        execute("INSERT payment.Bill(BranchId,CashierShiftId,Subtotal,TotalAmount,PaidAmount,"
                        + "PaymentMethod,Status,PaidAt) VALUES (?,?,?,?,?,'TRANSFER','PAID',?)",
                branchId, cashierShiftId, amount, amount, amount, java.sql.Timestamp.valueOf(paidAt));
    }

    private Payroll payroll(int userId) {
        Payroll payroll = new Payroll();
        payroll.setUserId(userId);
        payroll.setWorkedHours(new BigDecimal("10"));
        payroll.setHourlyRate(new BigDecimal("25000"));
        return payroll;
    }

    private int createReceipt(int branchId, int managerId) throws SQLException {
        execute("INSERT inventory.StockReceipt(BranchId,ReceivedBy,Status) VALUES (?,?,'DRAFT')",
                branchId, managerId);
        return scalarInt(
                "SELECT MAX(StockReceiptId) FROM inventory.StockReceipt WHERE BranchId=?", branchId);
    }

    private int createIngredient(String prefix, String unit) throws SQLException {
        String name = unique(prefix);
        execute("INSERT catalog.Ingredient(Name,Unit,IngredientType) VALUES (?,?,'RAW')", name, unit);
        return scalarInt("SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", name);
    }

    private int createBranch() throws SQLException {
        String code = unique("N").substring(0, 13);
        execute("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES (?,N'Normalization IT','00:00','23:59')",
                code);
        return scalarInt("SELECT BranchId FROM org.Branch WHERE Code=?", code);
    }

    private int createUser(int branchId, String roleCode) throws SQLException {
        execute("IF NOT EXISTS(SELECT 1 FROM iam.Role WHERE Code=?) "
                        + "INSERT iam.Role(Code,Name) VALUES (?,?)", roleCode, roleCode, roleCode);
        int roleId = scalarInt("SELECT RoleId FROM iam.Role WHERE Code=?", roleCode);
        String username = unique("normal-user");
        execute("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleId,BranchId) "
                        + "VALUES (?,'x',N'Normalization IT',?,?)", username, roleId, branchId);
        return scalarInt("SELECT UserId FROM iam.UserAccount WHERE Username=?", username);
    }

    private HomeSetting home(String eyebrow, String title) {
        HomeSetting setting = new HomeSetting();
        setting.setHeroEyebrow(eyebrow);
        setting.setHeroTitle(title);
        setting.setHeroSubtitle("subtitle");
        setting.setHeroImageUrl("/image.svg");
        return setting;
    }

    private void execute(String sql, Object... args) throws SQLException {
        try (Connection connection = connection()) {
            execute(connection, sql, args);
        }
    }

    private void execute(Connection connection, String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            statement.executeUpdate();
        }
    }

    private int scalarInt(String sql, Object... args) throws SQLException {
        try (Connection connection = connection()) {
            return scalarInt(connection, sql, args);
        }
    }

    private int scalarInt(Connection connection, String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private long scalarLong(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0;
            }
        }
    }

    private BigDecimal scalarDecimal(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getBigDecimal(1) : null;
            }
        }
    }

    private String scalarString(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
