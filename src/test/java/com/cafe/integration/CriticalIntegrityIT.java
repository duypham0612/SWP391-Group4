package com.cafe.integration;

import com.cafe.common.BusinessException;
import com.cafe.model.Payroll;
import com.cafe.service.cashier.BillingService;
import com.cafe.service.cashier.TableSessionService;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.manager.PayrollService;
import com.cafe.service.manager.ShiftService;
import com.cafe.service.manager.StockReceiptService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests cho các lỗi integrity/IDOR mức nghiêm trọng đã phát hiện khi audit. */
public class CriticalIntegrityIT extends SqlServerIntegrationSupport {

    @Test
    void fifty_concurrent_orders_receive_unique_pickup_codes() throws Exception {
        int branchId = createBranch();
        int cashierId = createUser(branchId, "CASHIER");
        String categoryName = unique("pickup-category");
        execute("INSERT catalog.Category(Name) VALUES (?)", categoryName);
        int categoryId = scalarInt("SELECT CategoryId FROM catalog.Category WHERE Name=?", categoryName);
        String productName = unique("pickup-product");
        execute("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (?,?,25000)",
                categoryId, productName);
        int productId = scalarInt("SELECT ProductId FROM catalog.Product WHERE Name=?", productName);
        execute("INSERT catalog.BranchMenu(BranchId,ProductId,IsListed,IsTemporarilyUnavailable) " +
                "VALUES (?,?,1,0)", branchId, productId);

        int workerCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < workerCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    com.cafe.service.shared.OrderService.CartLine line =
                            new com.cafe.service.shared.OrderService.CartLine();
                    line.productId = productId;
                    line.quantity = 1;
                    return new com.cafe.service.shared.OrderService().placeOrder(
                            branchId, null, "COUNTER", "TAKEAWAY", cashierId, List.of(line));
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            Set<Integer> orderIds = new HashSet<>();
            for (Future<Integer> future : futures) {
                orderIds.add(future.get(45, TimeUnit.SECONDS));
            }
            assertEquals(workerCount, orderIds.size());
            assertEquals(workerCount, scalarInt(
                    "SELECT COUNT(DISTINCT PickupCode) FROM sales.SalesOrder " +
                            "WHERE BranchId=? AND OrderId IN (SELECT OrderId FROM sales.OrderItem WHERE ProductId=?)",
                    branchId, productId));
            assertEquals(0, scalarInt(
                    "SELECT COUNT(*) FROM (SELECT PickupCode FROM sales.SalesOrder WHERE BranchId=? " +
                            "GROUP BY BusinessDate,PickupCode HAVING COUNT(*)>1) duplicate", branchId));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrent_receipt_confirmation_posts_inventory_exactly_once() throws Exception {
        int branchId = createBranch();
        int managerId = createUser(branchId, "BRANCH_MANAGER");
        String ingredientName = unique("ingredient");
        int ingredientId = insertAndId(
                "INSERT catalog.Ingredient(Name,Unit,IngredientType) VALUES (?,N'g','RAW')",
                "SELECT IngredientId FROM catalog.Ingredient WHERE Name=?",
                ingredientName, ingredientName);
        execute("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold) VALUES (?,?,100,0)",
                branchId, ingredientId);
        int receiptId = insertAndId(
                "INSERT inventory.StockReceipt(BranchId,ReceivedBy,Status) VALUES (?,?,'DRAFT')",
                "SELECT MAX(StockReceiptId) FROM inventory.StockReceipt WHERE BranchId=?",
                branchId, managerId, branchId);
        int conversionId = scalarInt("SELECT IngredientUnitConversionId FROM catalog.IngredientUnitConversion " +
                "WHERE IngredientId=? AND IsBaseUnit=1", ingredientId);
        new StockReceiptService().addReceiptLine(receiptId, branchId, ingredientId,
                new BigDecimal("20"), new BigDecimal("2"), conversionId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = confirmAsync(executor, ready, start, receiptId, branchId, managerId);
            Future<Boolean> second = confirmAsync(executor, ready, start, receiptId, branchId, managerId);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, scalarInt(
                    "SELECT COUNT(*) FROM inventory.InventoryTransaction " +
                            "WHERE BranchId=? AND IngredientId=? AND TxnType='RECEIPT' AND ReferenceId=?",
                    branchId, ingredientId, receiptId));
            assertEquals(new BigDecimal("120.000"), scalarDecimal(
                    "SELECT QuantityOnHand FROM inventory.BranchInventory WHERE BranchId=? AND IngredientId=?",
                    branchId, ingredientId));
            assertEquals("CONFIRMED", scalarString(
                    "SELECT Status FROM inventory.StockReceipt WHERE StockReceiptId=?", receiptId));

            int otherBranch = createBranch();
            assertNull(new StockReceiptService().getReceipt(receiptId, otherBranch));
            assertThrows(BusinessException.class, () -> new StockReceiptService().addReceiptLine(
                    receiptId, otherBranch, ingredientId, BigDecimal.ONE, BigDecimal.ONE, 0));
            String secondIngredient = unique("ingredient");
            execute("INSERT catalog.Ingredient(Name,Unit,IngredientType) VALUES (?,N'g','RAW')",
                    secondIngredient);
            int secondIngredientId = scalarInt(
                    "SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", secondIngredient);
            assertThrows(SQLException.class, () -> execute(
                    "INSERT inventory.StockReceiptDetail(StockReceiptId,IngredientId,EnteredQuantity,UnitCost," +
                            "IngredientUnitConversionId,UnitNameAtEntry,FactorToBaseAtEntry) " +
                            "SELECT ?,?,1,1,c.IngredientUnitConversionId,c.UnitName,c.FactorToBase " +
                            "FROM catalog.IngredientUnitConversion c WHERE c.IngredientId=? AND c.IsBaseUnit=1",
                    receiptId, secondIngredientId, secondIngredientId));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void voucher_hard_limit_allows_only_one_discounted_payment() throws Exception {
        int branchId = createBranch();
        int cashierId = createUser(branchId, "CASHIER");
        execute("INSERT payment.CashierShift(BranchId,CashierId,OpeningCash) VALUES (?,?,0)",
                branchId, cashierId);
        int shiftId = scalarInt(
                "SELECT CashierShiftId FROM payment.CashierShift WHERE BranchId=? AND ClosedAt IS NULL",
                branchId);

        String voucherCode = unique("voucher");
        execute("INSERT payment.Voucher(Code,DiscountType,DiscountValue,MinOrderAmount,Scope,UsageLimit) " +
                        "VALUES (?,'PERCENT',10,0,'CHAIN',1)", voucherCode);
        int voucherId = scalarInt("SELECT VoucherId FROM payment.Voucher WHERE Code=?", voucherCode);
        int[] billIds = {createDiscountedBill(branchId, cashierId, voucherId),
                createDiscountedBill(branchId, cashierId, voucherId)};

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = payAsync(executor, ready, start, billIds[0], shiftId);
            Future<Boolean> second = payAsync(executor, ready, start, billIds[1], shiftId);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int paid = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, paid);
            assertEquals(1, scalarInt("SELECT UsedCount FROM payment.Voucher WHERE VoucherId=?", voucherId));
            assertEquals(1, scalarInt(
                    "SELECT COUNT(*) FROM payment.Bill WHERE BillId IN (?,?) AND Status='PAID'",
                    billIds[0], billIds[1]));
            assertEquals(1, scalarInt(
                    "SELECT COUNT(*) FROM payment.Bill WHERE BillId IN (?,?) AND Status='UNPAID'",
                    billIds[0], billIds[1]));

            assertThrows(SQLException.class, () -> execute(
                    "INSERT payment.Voucher(Code,DiscountType,DiscountValue,MinOrderAmount,Scope) " +
                            "VALUES (?,'PERCENT',999,0,'CHAIN')", unique("bad-voucher")));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void branch_scoped_services_and_composite_keys_reject_foreign_ids() throws Exception {
        int branchA = createBranch();
        int branchB = createBranch();
        int managerA = createUser(branchA, "BRANCH_MANAGER");
        int employeeB = createUser(branchB, "BARISTA");

        String tableNumber = unique("table");
        execute("INSERT sales.DiningTable(BranchId,TableNumber) VALUES (?,?)", branchB, tableNumber);
        int tableB = scalarInt(
                "SELECT DiningTableId FROM sales.DiningTable WHERE BranchId=? AND TableNumber=?",
                branchB, tableNumber);
        assertThrows(BusinessException.class,
                () -> new TableSessionService().openSession(branchA, tableB, managerA));
        assertThrows(SQLException.class, () -> execute(
                "INSERT sales.TableSession(BranchId,DiningTableId,OpenedBy) VALUES (?,?,?)",
                branchA, tableB, managerA));

        String templateName = unique("shift");
        execute("INSERT hr.ShiftTemplate(BranchId,Name,StartTime,EndTime) VALUES (?,?,'08:00','12:00')",
                branchA, templateName);
        int templateA = scalarInt(
                "SELECT ShiftTemplateId FROM hr.ShiftTemplate WHERE BranchId=? AND Name=?",
                branchA, templateName);
        assertThrows(BusinessException.class, () -> new ShiftService().assignShift(
                templateA, employeeB, LocalDate.now().plusDays(1), branchA));

        Payroll line = new Payroll();
        line.setUserId(employeeB);
        line.setWorkedHours(new BigDecimal("10"));
        line.setHourlyRate(new BigDecimal("25000"));
        YearMonth payMonth = YearMonth.now();
        assertThrows(BusinessException.class, () -> new PayrollService().savePayroll(
                branchA, payMonth, List.of(line), managerA));
        assertEquals(0, scalarInt(
                "SELECT COUNT(*) FROM hr.Payroll WHERE BranchId=? AND UserId=? AND PayrollMonth=?",
                branchA, employeeB, java.sql.Date.valueOf(payMonth.atDay(1))));
    }

    @Test
    void manager_cannot_mutate_attendance_of_another_branch() throws Exception {
        int branchA = createBranch();
        int branchB = createBranch();
        int managerA = createUser(branchA, "BRANCH_MANAGER");
        int employeeB = createUser(branchB, "BARISTA");
        String templateName = unique("attendance-shift");
        execute("INSERT hr.ShiftTemplate(BranchId,Name,StartTime,EndTime) VALUES (?,?,'08:00','12:00')",
                branchB, templateName);
        int templateId = scalarInt(
                "SELECT ShiftTemplateId FROM hr.ShiftTemplate WHERE BranchId=? AND Name=?",
                branchB, templateName);
        execute("INSERT hr.ShiftAssignment(ShiftTemplateId,BranchId,UserId,WorkDate) VALUES (?,?,?,?)",
                templateId, branchB, employeeB, java.sql.Date.valueOf(LocalDate.now()));
        int assignmentId = scalarInt(
                "SELECT MAX(ShiftAssignmentId) FROM hr.ShiftAssignment WHERE ShiftTemplateId=? AND UserId=?",
                templateId, employeeB);
        execute("INSERT hr.Attendance(ShiftAssignmentId,Status) VALUES (?,'PENDING')", assignmentId);
        int attendanceId = scalarInt(
                "SELECT AttendanceId FROM hr.Attendance WHERE ShiftAssignmentId=?", assignmentId);

        assertThrows(BusinessException.class,
                () -> new AttendanceService().rejectAttendance(attendanceId, managerA, branchA));
        assertEquals("PENDING", scalarString(
                "SELECT Status FROM hr.Attendance WHERE AttendanceId=?", attendanceId));
    }

    private Future<Boolean> confirmAsync(ExecutorService executor, CountDownLatch ready,
                                         CountDownLatch start, int receiptId,
                                         int branchId, int managerId) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                new StockReceiptService().confirmReceipt(receiptId, branchId, managerId);
                return true;
            } catch (BusinessException expected) {
                return false;
            }
        });
    }

    private Future<Boolean> payAsync(ExecutorService executor, CountDownLatch ready,
                                     CountDownLatch start, int billId, int shiftId) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            return new BillingService().payBill(billId, "TRANSFER", shiftId, null).paid();
        });
    }

    private int createDiscountedBill(int branchId, int cashierId, int voucherId) throws Exception {
        String categoryName = unique("category");
        execute("INSERT catalog.Category(Name) VALUES (?)", categoryName);
        int categoryId = scalarInt("SELECT CategoryId FROM catalog.Category WHERE Name=?", categoryName);
        String productName = unique("product");
        execute("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (?,?,100)",
                categoryId, productName);
        int productId = scalarInt("SELECT ProductId FROM catalog.Product WHERE Name=?", productName);
        execute("INSERT sales.SalesOrder(BranchId,Source,OrderType,Status,CreatedBy,BusinessDate) " +
                "VALUES (?,'COUNTER','TAKEAWAY','COMPLETED',?,CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME())))",
                branchId, cashierId);
        int orderId = scalarInt("SELECT MAX(OrderId) FROM sales.SalesOrder WHERE BranchId=?", branchId);
        execute("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder," +
                "StartedAt,DoneAt,PickedUpAt,ServedAt) " +
                "VALUES (?,?,?,1,100,'SERVED',?,SYSUTCDATETIME(),SYSUTCDATETIME(),SYSUTCDATETIME(),SYSUTCDATETIME())",
                orderId, branchId, productId, productName);
        int itemId = scalarInt("SELECT MAX(OrderItemId) FROM sales.OrderItem WHERE OrderId=?", orderId);
        execute("INSERT payment.Bill(BranchId,Subtotal,VatAmount,DiscountAmount,TotalAmount,VoucherId,Status) " +
                "VALUES (?,100,7.20,10,97.20,?,'UNPAID')", branchId, voucherId);
        int billId = scalarInt("SELECT MAX(BillId) FROM payment.Bill WHERE BranchId=?", branchId);
        execute("INSERT payment.BillItem(BillId,BranchId,OrderItemId,Amount) VALUES (?,?,?,100)",
                billId, branchId, itemId);
        return billId;
    }

    private int createBranch() throws Exception {
        String code = unique("B").substring(0, 12);
        execute("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES (?,N'Critical IT','00:00','23:59')",
                code);
        return scalarInt("SELECT BranchId FROM org.Branch WHERE Code=?", code);
    }

    private int createUser(int branchId, String roleCode) throws Exception {
        execute("IF NOT EXISTS (SELECT 1 FROM iam.Role WHERE Code=?) " +
                "INSERT iam.Role(Code,Name) VALUES (?,?)", roleCode, roleCode, roleCode);
        int roleId = scalarInt("SELECT RoleId FROM iam.Role WHERE Code=?", roleCode);
        String username = unique("u");
        execute("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleId,BranchId) " +
                "VALUES (?,'x',N'Critical IT',?,?)", username, roleId, branchId);
        return scalarInt("SELECT UserId FROM iam.UserAccount WHERE Username=?", username);
    }

    private int insertAndId(String insertSql, String selectSql, Object... args) throws Exception {
        // Args của hai câu lệnh được truyền nối tiếp: insert dùng số placeholder của nó,
        // select dùng phần còn lại.
        int insertParams = (int) insertSql.chars().filter(ch -> ch == '?').count();
        Object[] insertArgs = java.util.Arrays.copyOfRange(args, 0, insertParams);
        Object[] selectArgs = java.util.Arrays.copyOfRange(args, insertParams, args.length);
        execute(insertSql, insertArgs);
        return scalarInt(selectSql, selectArgs);
    }

    private void execute(String sql, Object... args) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            statement.executeUpdate();
        }
    }

    private int scalarInt(String sql, Object... args) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private BigDecimal scalarDecimal(String sql, Object... args) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getBigDecimal(1) : null; }
        }
    }

    private String scalarString(String sql, Object... args) throws SQLException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
