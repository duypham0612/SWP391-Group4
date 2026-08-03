package com.cafe.integration;

import com.cafe.common.BusinessException;
import com.cafe.model.CartLine;
import com.cafe.service.cashier.BillingService;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.manager.ShiftService;
import com.cafe.service.manager.StockReceiptService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
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
        int productId = createProduct();
        execute("INSERT catalog.BranchMenu(BranchId,ProductId,IsListed,IsTemporarilyUnavailable) "
                + "VALUES (?,?,1,0)", branchId, productId);

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
                    CartLine line = new CartLine();
                    line.productId = productId;
                    line.quantity = 1;
                    return new com.cafe.service.shared.OrderPlacementService().placeOrder(
                            branchId, null, "COUNTER", "TAKEAWAY", cashierId, List.of(line));
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            Set<Integer> orderIds = new HashSet<>();
            for (Future<Integer> future : futures) orderIds.add(future.get(45, TimeUnit.SECONDS));
            assertEquals(workerCount, orderIds.size());
            assertEquals(workerCount, scalarInt(
                    "SELECT COUNT(DISTINCT PickupCode) FROM sales.SalesOrder "
                            + "WHERE BranchId=? AND OrderId IN "
                            + "(SELECT OrderId FROM sales.OrderItem WHERE ProductId=?)",
                    branchId, productId));
            assertEquals(0, scalarInt(
                    "SELECT COUNT(*) FROM (SELECT PickupCode FROM sales.SalesOrder WHERE BranchId=? "
                            + "GROUP BY BusinessDate,PickupCode HAVING COUNT(*)>1) duplicate", branchId));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrent_receipt_confirmation_posts_inventory_exactly_once() throws Exception {
        int branchId = createBranch();
        int managerId = createUser(branchId, "BRANCH_MANAGER");
        int ingredientId = createRawIngredient();
        execute("INSERT inventory.BranchInventory(BranchId,IngredientId,QuantityOnHand,MinThreshold) "
                + "VALUES (?,?,100,0)", branchId, ingredientId);
        String batchId = UUID.randomUUID().toString();
        execute("INSERT inventory.StockReceiptLine(ReceiptBatchId,BranchId,ReceivedBy,DocumentDate,Status,"
                        + "IngredientId,UnitCost,EnteredQuantity,UnitNameAtEntry,FactorToBaseAtEntry) "
                        + "VALUES (?,?,?,CONVERT(date,SYSUTCDATETIME()),'DRAFT',?,?,?,N'g',1)",
                batchId, branchId, managerId, ingredientId, new BigDecimal("2"), new BigDecimal("20"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = confirmAsync(executor, ready, start, batchId, branchId, managerId);
            Future<Boolean> second = confirmAsync(executor, ready, start, batchId, branchId, managerId);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, scalarInt(
                    "SELECT COUNT(*) FROM inventory.InventoryTransaction WHERE BranchId=? "
                            + "AND IngredientId=? AND TxnType='RECEIPT' "
                            + "AND ReferenceType='STOCK_RECEIPT_LINE' AND ReferenceId=?",
                    branchId, ingredientId, batchId));
            assertEquals(new BigDecimal("120.000"), scalarDecimal(
                    "SELECT QuantityOnHand FROM inventory.BranchInventory WHERE BranchId=? AND IngredientId=?",
                    branchId, ingredientId));
            assertEquals("CONFIRMED", scalarString(
                    "SELECT MIN(Status) FROM inventory.StockReceiptLine WHERE ReceiptBatchId=?", batchId));

            int otherBranch = createBranch();
            assertNull(new StockReceiptService().getReceipt(batchId, otherBranch));
            assertThrows(BusinessException.class, () -> new StockReceiptService().addReceiptLine(
                    batchId, otherBranch, createRawIngredient(), BigDecimal.ONE, BigDecimal.ONE, 0));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrent_payment_can_settle_a_bill_only_once() throws Exception {
        int branchId = createBranch();
        int cashierId = createUser(branchId, "CASHIER");
        execute("INSERT payment.CashierShift(BranchId,CashierId,OpeningCash) VALUES (?,?,0)",
                branchId, cashierId);
        int shiftId = scalarInt(
                "SELECT CashierShiftId FROM payment.CashierShift WHERE BranchId=? AND ClosedAt IS NULL",
                branchId);
        int billId = createPayableBill(branchId, cashierId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = payAsync(executor, ready, start, billId, shiftId);
            Future<Boolean> second = payAsync(executor, ready, start, billId, shiftId);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int paid = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, paid);
            assertEquals("PAID", scalarString("SELECT Status FROM payment.Bill WHERE BillId=?", billId));
            assertEquals(1, scalarInt(
                    "SELECT COUNT(*) FROM ops.OutboxEvent WHERE EventType='payment.completed' "
                            + "AND AggregateId=?", String.valueOf(billId)));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shift_assignment_rejects_employee_of_another_branch() throws Exception {
        int branchA = createBranch();
        int branchB = createBranch();
        int employeeB = createUser(branchB, "BARISTA");

        assertThrows(BusinessException.class, () -> new ShiftService().assignShift(
                unique("shift"), LocalTime.of(8, 0), LocalTime.of(12, 0),
                employeeB, LocalDate.now().plusDays(1), branchA));
        assertEquals(0, scalarInt(
                "SELECT COUNT(*) FROM hr.ShiftAssignment WHERE BranchId=? AND UserId=?",
                branchA, employeeB));
    }

    @Test
    void manager_cannot_mutate_attendance_of_another_branch() throws Exception {
        int branchA = createBranch();
        int branchB = createBranch();
        int managerA = createUser(branchA, "BRANCH_MANAGER");
        int employeeB = createUser(branchB, "BARISTA");
        execute("INSERT hr.ShiftAssignment(ShiftName,StartTime,EndTime,BranchId,UserId,WorkDate,"
                        + "CheckInAt,AttendanceStatus) VALUES (?,'08:00','12:00',?,?,?,SYSUTCDATETIME(),'PENDING')",
                unique("attendance-shift"), branchB, employeeB, java.sql.Date.valueOf(LocalDate.now()));
        int assignmentId = scalarInt(
                "SELECT MAX(ShiftAssignmentId) FROM hr.ShiftAssignment WHERE BranchId=? AND UserId=?",
                branchB, employeeB);

        assertThrows(BusinessException.class,
                () -> new AttendanceService().rejectAttendance(assignmentId, managerA, branchA));
        assertEquals("PENDING", scalarString(
                "SELECT AttendanceStatus FROM hr.ShiftAssignment WHERE ShiftAssignmentId=?", assignmentId));
    }

    private Future<Boolean> confirmAsync(ExecutorService executor, CountDownLatch ready,
                                         CountDownLatch start, String batchId,
                                         int branchId, int managerId) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                new StockReceiptService().confirmReceipt(batchId, branchId, managerId);
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

    private int createPayableBill(int branchId, int cashierId) throws Exception {
        int productId = createProduct();
        execute("INSERT sales.SalesOrder(BranchId,Source,OrderType,Status,CreatedBy,BusinessDate) "
                        + "VALUES (?,'COUNTER','TAKEAWAY','COMPLETED',?,"
                        + "CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME())))",
                branchId, cashierId);
        int orderId = scalarInt("SELECT MAX(OrderId) FROM sales.SalesOrder WHERE BranchId=?", branchId);
        execute("INSERT payment.Bill(BranchId,Subtotal,VatAmount,DiscountAmount,TotalAmount,Status) "
                + "VALUES (?,100,8,0,108,'UNPAID')", branchId);
        int billId = scalarInt("SELECT MAX(BillId) FROM payment.Bill WHERE BranchId=?", branchId);
        execute("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,"
                        + "ProductNameAtOrder,StartedAt,DoneAt,PickedUpAt,ServedAt,BillId,BilledAmount) "
                        + "VALUES (?,?,?,1,100,'SERVED',N'Payable',SYSUTCDATETIME(),SYSUTCDATETIME(),"
                        + "SYSUTCDATETIME(),SYSUTCDATETIME(),?,100)",
                orderId, branchId, productId, billId);
        return billId;
    }

    private int createProduct() throws Exception {
        String categoryName = unique("category");
        execute("INSERT catalog.Category(Name) VALUES (?)", categoryName);
        int categoryId = scalarInt("SELECT CategoryId FROM catalog.Category WHERE Name=?", categoryName);
        String productName = unique("product");
        execute("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES (?,?,25000)",
                categoryId, productName);
        return scalarInt("SELECT ProductId FROM catalog.Product WHERE Name=?", productName);
    }

    private int createRawIngredient() throws Exception {
        String name = unique("ingredient");
        execute("INSERT catalog.Ingredient(Name,Unit,IngredientType) VALUES (?,N'g','RAW')", name);
        return scalarInt("SELECT IngredientId FROM catalog.Ingredient WHERE Name=?", name);
    }

    private int createBranch() throws Exception {
        String code = unique("B").substring(0, 12);
        execute("INSERT org.Branch(Code,Name,OpenTime,CloseTime) "
                + "VALUES (?,N'Critical IT','00:00','23:59')", code);
        return scalarInt("SELECT BranchId FROM org.Branch WHERE Code=?", code);
    }

    private int createUser(int branchId, String roleCode) throws Exception {
        String username = unique("u");
        execute("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleCode,BranchId) "
                + "VALUES (?,'x',N'Critical IT',?,?)", username, roleCode, branchId);
        return scalarInt("SELECT UserId FROM iam.UserAccount WHERE Username=?", username);
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
