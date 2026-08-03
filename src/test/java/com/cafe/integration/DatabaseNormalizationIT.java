package com.cafe.integration;

import com.cafe.config.SchemaVersionGuard;
import com.cafe.dao.cashier.BillLineDao;
import com.cafe.dao.shared.BranchDao;
import com.cafe.dao.shared.OrderItemModifierDao;
import com.cafe.model.BillLine;
import com.cafe.model.Branch;
import com.cafe.model.OrderItemModifier;
import com.cafe.model.PayrollRow;
import com.cafe.model.Product;
import com.cafe.model.StockReceipt;
import com.cafe.model.StockReceiptDetail;
import com.cafe.service.admin.ProductService;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Nghiệm thu schema chuẩn hoá, snapshot lịch sử và dữ liệu tổng hợp runtime. */
public class DatabaseNormalizationIT extends SqlServerIntegrationSupport {

    @Test
    void branch_hero_reads_and_updates_selected_branch() throws Exception {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                BranchDao dao = new BranchDao();
                Branch selected = dao.findFirstActive(connection);
                assertNotNull(selected);

                Branch updated = home(selected.getBranchId(), "eyebrow-update", "title-update");
                assertEquals(1, dao.updateHero(connection, updated));
                assertEquals("title-update", dao.findById(connection, selected.getBranchId()).getHeroTitle());
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void receipt_uses_purchase_factor_snapshot_after_ingredient_configuration_changes() throws Exception {
        int branchId = createBranch();
        int managerId = createUser(branchId, "BRANCH_MANAGER");
        int ingredientId = createIngredient("receipt-unit", "g");
        execute("UPDATE catalog.Ingredient SET PurchaseUnitName=N'Bag',PurchaseFactorToBase=1000 "
                + "WHERE IngredientId=?", ingredientId);
        execute("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold) "
                + "VALUES (?,?,0,0)", branchId, ingredientId);

        StockReceipt receipt = new StockReceipt();
        receipt.setBranchId(branchId);
        receipt.setReceivedBy(managerId);
        StockReceiptDetail line = new StockReceiptDetail();
        line.setIngredientId(ingredientId);
        line.setEnteredQuantity(new BigDecimal("2"));
        line.setUnitCost(new BigDecimal("50"));
        line.setUnitChoice(1);
        StockReceiptService service = new StockReceiptService();
        String batchId = service.createDraftReceipt(receipt, line);

        assertEquals(new BigDecimal("1000.000000"), scalarDecimal(
                "SELECT FactorToBaseAtEntry FROM inventory.StockReceiptLine WHERE ReceiptBatchId=?",
                batchId));
        assertEquals(new BigDecimal("2000.000"), scalarDecimal(
                "SELECT BaseQuantity FROM inventory.StockReceiptLine WHERE ReceiptBatchId=?", batchId));

        execute("UPDATE catalog.Ingredient SET PurchaseFactorToBase=2000 WHERE IngredientId=?", ingredientId);
        service.confirmReceipt(batchId, branchId, managerId);
        assertEquals(new BigDecimal("2000.000"), scalarDecimal(
                "SELECT QuantityOnHand FROM inventory.BranchInventory WHERE BranchId=? AND IngredientId=?",
                branchId, ingredientId));
    }

    @Test
    void products_receive_fixed_size_sugar_and_ice_choices() throws Exception {
        String categoryName = unique("size-category");
        execute("INSERT catalog.Category(Name) VALUES (?)", categoryName);
        int categoryId = scalarInt("SELECT CategoryId FROM catalog.Category WHERE Name=?", categoryName);
        ProductService service = new ProductService();

        int firstId = service.createProduct(product(categoryId, unique("size-product-a"), "25000"));
        int secondId = service.createProduct(product(categoryId, unique("size-product-b"), "30000"));

        assertEquals(6, scalarInt(
                "SELECT COUNT(*) FROM catalog.ModifierGroup WHERE ProductId IN(?,?) "
                        + "AND Name IN(N'Size',N'Đường',N'Đá') AND IsRequired=1 "
                        + "AND MinSelect=1 AND MaxSelect=1", firstId, secondId));
        assertEquals(6, scalarInt(
                "SELECT COUNT(*) FROM catalog.ModifierOption o "
                        + "JOIN catalog.ModifierGroup g ON g.ModifierGroupId=o.ModifierGroupId "
                        + "WHERE g.ProductId IN(?,?) AND g.Name=N'Size' AND o.IsActive=1 "
                        + "AND ((o.Name=N'Size S' AND o.PriceDelta=0) "
                        + "OR (o.Name=N'Size M' AND o.PriceDelta=6000) "
                        + "OR (o.Name=N'Size L' AND o.PriceDelta=10000))", firstId, secondId));
        assertEquals(16, scalarInt(
                "SELECT COUNT(*) FROM catalog.ModifierOption o "
                        + "JOIN catalog.ModifierGroup g ON g.ModifierGroupId=o.ModifierGroupId "
                        + "WHERE g.ProductId IN(?,?) AND g.Name IN(N'Đường',N'Đá') "
                        + "AND o.IsActive=1 AND o.PriceDelta=0", firstId, secondId));
    }

    @Test
    void stock_count_in_purchase_unit_writes_base_quantity_and_diff() throws Exception {
        int branchId = createBranch();
        int managerId = createUser(branchId, "BRANCH_MANAGER");
        int ingredientId = createIngredient("count-unit", "g");
        execute("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold) "
                + "VALUES (?,?,2500,0)", branchId, ingredientId);
        execute("UPDATE catalog.Ingredient SET PurchaseUnitName=N'Bag',PurchaseFactorToBase=1000 "
                + "WHERE IngredientId=?", ingredientId);

        new InventoryService().createAdjustment(
                branchId, ingredientId, new BigDecimal("2"), 1, "package count", managerId);

        assertEquals(new BigDecimal("2000.000"), scalarDecimal(
                "SELECT QuantityOnHand FROM inventory.BranchInventory WHERE BranchId=? AND IngredientId=?",
                branchId, ingredientId));
        assertEquals(new BigDecimal("2.000000"), scalarDecimal(
                "SELECT TOP (1) CountedQuantity FROM inventory.StockAdjustment "
                        + "WHERE BranchId=? AND IngredientId=? ORDER BY StockAdjustmentId DESC",
                branchId, ingredientId));
        assertEquals(new BigDecimal("-500.000"), scalarDecimal(
                "SELECT TOP (1) ChangeQty FROM inventory.InventoryTransaction WHERE BranchId=? "
                        + "AND IngredientId=? AND ReferenceType='STOCK_ADJUSTMENT' "
                        + "ORDER BY InventoryTransactionId DESC", branchId, ingredientId));
    }

    @Test
    void payroll_is_derived_from_approved_assignment_snapshot() throws Exception {
        int branchId = createBranch();
        int managerId = createUser(branchId, "BRANCH_MANAGER");
        int baristaId = createUser(branchId, "BARISTA");
        YearMonth month = YearMonth.now();
        LocalDate workDate = month.atDay(1);
        execute("INSERT hr.ShiftAssignment(ShiftName,StartTime,EndTime,UserId,WorkDate,BranchId,"
                        + "HourlyRateSnapshot,CheckInAt,CheckOutAt,AttendanceStatus,ApprovedBy,ApprovedAt) "
                        + "VALUES (N'Morning','08:00','18:00',?,?,?,25000,"
                        + "DATEADD(hour,8,CAST(? AS datetime2)),DATEADD(hour,18,CAST(? AS datetime2)),"
                        + "'APPROVED',?,SYSUTCDATETIME())",
                baristaId, java.sql.Date.valueOf(workDate), branchId,
                java.sql.Date.valueOf(workDate), java.sql.Date.valueOf(workDate), managerId);

        List<PayrollRow> rows = new PayrollService().getMonthlyPayroll(branchId, month);
        PayrollRow row = rows.stream().filter(item -> item.getUserId() == baristaId).findFirst().orElseThrow();
        assertEquals(1, row.getApprovedShifts());
        assertEquals(10.0, row.getTotalHours());
        assertEquals(new BigDecimal("250000"), row.getSalary());
    }

    @Test
    void order_and_bill_keep_product_and_modifier_snapshots_after_catalog_rename() throws Exception {
        int branchId = createBranch();
        int cashierId = createUser(branchId, "CASHIER");
        String categoryName = unique("snapshot-category");
        execute("INSERT catalog.Category(Name) VALUES (?)", categoryName);
        int categoryId = scalarInt("SELECT CategoryId FROM catalog.Category WHERE Name=?", categoryName);
        String productName = unique("Original-product");
        execute("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (?,?,100)", categoryId, productName);
        int productId = scalarInt("SELECT ProductId FROM catalog.Product WHERE Name=?", productName);
        execute("INSERT catalog.BranchMenu(BranchId,ProductId,IsListed,IsTemporarilyUnavailable) "
                + "VALUES (?,?,1,0)", branchId, productId);
        String groupName = unique("snapshot-group");
        String optionName = unique("Original-option");
        execute("INSERT catalog.ModifierGroup(ProductId,Name,IsRequired,MinSelect,MaxSelect,SortOrder) "
                + "VALUES (?,?,0,0,1,5)", productId, groupName);
        int groupId = scalarInt("SELECT ModifierGroupId FROM catalog.ModifierGroup WHERE Name=?", groupName);
        execute("INSERT catalog.ModifierOption(ModifierGroupId,Name,PriceDelta) VALUES (?,?,5)",
                groupId, optionName);
        int optionId = scalarInt(
                "SELECT ModifierOptionId FROM catalog.ModifierOption WHERE ModifierGroupId=? AND Name=?",
                groupId, optionName);

        OrderService.CartLine line = new OrderService.CartLine();
        line.productId = productId;
        line.quantity = 1;
        line.optionIds = List.of(optionId);
        int orderId = new OrderService().placeOrder(
                branchId, null, "COUNTER", "TAKEAWAY", cashierId, List.of(line));
        int itemId = scalarInt("SELECT OrderItemId FROM sales.OrderItem WHERE OrderId=?", orderId);
        execute("UPDATE catalog.Product SET Name=? WHERE ProductId=?", unique("Renamed-product"), productId);
        execute("UPDATE catalog.ModifierOption SET Name=? WHERE ModifierOptionId=?",
                unique("Renamed-option"), optionId);

        assertEquals(productName, scalarString(
                "SELECT ProductNameAtOrder FROM sales.OrderItem WHERE OrderItemId=?", itemId));
        try (Connection connection = connection()) {
            List<OrderItemModifier> modifiers = new OrderItemModifierDao().findByItem(connection, itemId);
            assertEquals(optionName, modifiers.get(0).getOptionName());
        }

        execute("INSERT payment.Bill(BranchId,Subtotal,VatAmount,DiscountAmount,TotalAmount,Status) "
                + "VALUES (?,105,0,0,105,'UNPAID')", branchId);
        int billId = scalarInt("SELECT MAX(BillId) FROM payment.Bill WHERE BranchId=?", branchId);
        try (Connection connection = connection()) {
            new BillLineDao().insert(connection, billId, itemId);
            List<BillLine> items = new BillLineDao().findByBill(connection, billId);
            assertEquals(productName, items.get(0).getProductName());
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
        String masterUrl = cafeJdbcUrl().replaceFirst("(?i)databaseName=[^;]+", "databaseName=master");
        try (Connection master = DriverManager.getConnection(
                masterUrl, databaseUsername(), databasePassword())) {
            SchemaVersionGuard.Status status = SchemaVersionGuard.check(master);
            assertFalse(status.up());
            assertEquals("1", status.expectedVersion());
            assertNull(status.actualVersion());
        }
    }

    private Product product(int categoryId, String name, String price) {
        Product product = new Product();
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setBasePrice(new BigDecimal(price));
        return product;
    }

    private int createIngredient(String prefix, String unit) throws SQLException {
        String name = unique(prefix);
        execute("INSERT catalog.Ingredient(Name,Unit,IngredientType) VALUES (?,?,'RAW')", name, unit);
        return scalarInt("SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", name);
    }

    private int createBranch() throws SQLException {
        String code = unique("B").substring(0, 12);
        execute("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES (?,N'Normalization IT','00:00','23:59')",
                code);
        return scalarInt("SELECT BranchId FROM org.Branch WHERE Code=?", code);
    }

    private int createUser(int branchId, String roleCode) throws SQLException {
        String username = unique("normalization-user");
        execute("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleCode,BranchId) "
                + "VALUES (?,'x',N'Normalization IT',?,?)", username, roleCode, branchId);
        return scalarInt("SELECT UserId FROM iam.UserAccount WHERE Username=?", username);
    }

    private Branch home(int branchId, String eyebrow, String title) {
        Branch branch = new Branch();
        branch.setBranchId(branchId);
        branch.setHeroEyebrow(eyebrow);
        branch.setHeroTitle(title);
        branch.setHeroSubtitle("subtitle");
        branch.setHeroImageUrl("/assets/img/login-hero.svg");
        return branch;
    }

    private void execute(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            statement.executeUpdate();
        }
    }

    private int scalarInt(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private BigDecimal scalarDecimal(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getBigDecimal(1) : null; }
        }
    }

    private String scalarString(String sql, Object... args) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    private void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
    }

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
