package com.cafe.integration;

import com.cafe.common.BusinessDay;
import com.cafe.dao.cashier.BillDao;
import com.cafe.service.cashier.BillingService;
import com.cafe.service.cashier.CashierShiftService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Các invariant transaction quan trọng của két và doanh thu Cashier trên SQL Server thật. */
public class CashierShiftTransactionIT extends SqlServerIntegrationSupport {

    @Test
    void database_allows_only_one_open_till_per_branch() throws Exception {
        Fixture fixture = fixture(null);

        assertThrows(SQLException.class, () -> execute(
                "INSERT payment.CashierShift(BranchId,CashierId,OpeningCash) VALUES (?,?,0)",
                fixture.branchId, fixture.cashierId));
    }

    @Test
    void current_shift_and_report_are_scoped_to_branch() throws Exception {
        Fixture fixture = fixture(null);
        int anotherBranch = createBranch();
        CashierShiftService service = new CashierShiftService();

        assertNotNull(service.getCurrentShift(fixture.cashierId, fixture.branchId));
        assertNull(service.getCurrentShift(fixture.cashierId, anotherBranch));
        assertNotNull(service.getShiftReport(fixture.shiftId, fixture.branchId));
        assertNull(service.getShiftReport(fixture.shiftId, anotherBranch));
    }

    @Test
    void closing_with_unpaid_orders_requires_and_audits_handover() throws Exception {
        Fixture fixture = fixture("ACTIVE");
        CashierShiftService service = new CashierShiftService();

        assertThrows(IllegalArgumentException.class, () -> service.closeShift(
                fixture.shiftId, fixture.cashierId, fixture.branchId,
                new BigDecimal("500000"), false));
        assertEquals(0, scalarInt(
                "SELECT COUNT(*) FROM payment.CashierShift WHERE CashierShiftId=? AND ClosedAt IS NOT NULL",
                fixture.shiftId));

        service.closeShift(fixture.shiftId, fixture.cashierId, fixture.branchId,
                new BigDecimal("500000"), true);

        assertEquals(1, scalarInt(
                "SELECT COUNT(*) FROM payment.CashierShift WHERE CashierShiftId=? AND ClosedAt IS NOT NULL",
                fixture.shiftId));
        assertEquals(1, scalarInt(
                "SELECT COUNT(*) FROM ops.OutboxEvent "
                        + "WHERE EventType='cashier.shift_handover' AND AggregateId=?",
                String.valueOf(fixture.shiftId)));
    }

    @Test
    void cashier_today_uses_vietnam_calendar_boundaries() throws Exception {
        Fixture fixture = fixture(null);
        LocalDate todayVn = BusinessDay.todayVn();
        LocalDateTime fromUtc = BusinessDay.vnDayStartUtc(todayVn);
        LocalDateTime toUtc = BusinessDay.vnDayEndExclusiveUtc(todayVn);

        insertPaidBill(fixture, new BigDecimal("100"), fromUtc);
        insertPaidBill(fixture, new BigDecimal("200"), toUtc.minusSeconds(1));
        insertPaidBill(fixture, new BigDecimal("400"), toUtc);

        assertEquals(new BigDecimal("300.00"),
                new CashierShiftService().getTodayRevenue(fixture.branchId));
        try (Connection connection = connection()) {
            assertEquals(2, new BillDao().countPaidBetween(
                    connection, fixture.branchId, fromUtc, toUtc));
        }
    }

    @Test
    void payment_and_close_never_both_succeed_or_deadlock() throws Exception {
        Fixture fixture = fixture("COMPLETED");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> payment = executor.submit(() -> {
                ready.countDown();
                start.await();
                return new BillingService().payBill(
                        fixture.billId, "CASH", fixture.shiftId, new BigDecimal("10000")).paid();
            });
            Future<Boolean> close = executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    new CashierShiftService().closeShift(
                            fixture.shiftId, fixture.cashierId, fixture.branchId,
                            new BigDecimal("500000"), true);
                    return true;
                } catch (IllegalArgumentException | IllegalStateException expected) {
                    return false;
                }
            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            boolean paid = payment.get(10, TimeUnit.SECONDS);
            boolean closed = close.get(10, TimeUnit.SECONDS);

            assertTrue(paid ^ closed);
            assertEquals(paid ? "PAID" : "UNPAID",
                    scalarString("SELECT Status FROM payment.Bill WHERE BillId=?", fixture.billId));
            assertEquals(closed ? 1 : 0, scalarInt(
                    "SELECT COUNT(*) FROM payment.CashierShift "
                            + "WHERE CashierShiftId=? AND ClosedAt IS NOT NULL",
                    fixture.shiftId));
        } finally {
            executor.shutdownNow();
        }
    }

    private Fixture fixture(String orderStatus) throws Exception {
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        int branchId = createBranch();
        int cashierId;
        int shiftId;
        int billId = 0;
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleCode,BranchId) "
                    + "VALUES ('cash" + key + "','x',N'IT Cashier','CASHIER'," + branchId + ")");
            cashierId = id(connection,
                    "SELECT UserId FROM iam.UserAccount WHERE Username=?", "cash" + key);
            statement.executeUpdate("INSERT payment.CashierShift(BranchId,CashierId,OpeningCash) "
                    + "VALUES (" + branchId + "," + cashierId + ",500000)");
            shiftId = id(connection,
                    "SELECT CashierShiftId FROM payment.CashierShift "
                            + "WHERE BranchId=? AND ClosedAt IS NULL", branchId);

            if (orderStatus != null) {
                statement.executeUpdate("INSERT catalog.Category(Name) VALUES (N'IT Cashier Category " + key + "')");
                int categoryId = id(connection, "SELECT MAX(CategoryId) FROM catalog.Category");
                statement.executeUpdate("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES ("
                        + categoryId + ",N'IT Cashier Drink " + key + "',10000)");
                int productId = id(connection, "SELECT MAX(ProductId) FROM catalog.Product");
                statement.executeUpdate("INSERT sales.SalesOrder(BranchId,Source,OrderType,Status,CreatedBy,BusinessDate) VALUES ("
                        + branchId + ",'COUNTER','TAKEAWAY','" + orderStatus + "'," + cashierId
                        + ",CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME())))");
                int orderId = id(connection, "SELECT MAX(OrderId) FROM sales.SalesOrder");
                String itemStatus = "COMPLETED".equals(orderStatus) ? "SERVED" : "WAITING";
                String lifecycle = "SERVED".equals(itemStatus)
                        ? ",ProductNameAtOrder,StartedAt,DoneAt,PickedUpAt,ServedAt) VALUES (" + orderId + "," + branchId + ","
                          + productId + ",1,10000,'SERVED',N'IT Cashier Drink " + key + "',SYSUTCDATETIME(),SYSUTCDATETIME(),SYSUTCDATETIME(),SYSUTCDATETIME())"
                        : ",ProductNameAtOrder) VALUES (" + orderId + "," + branchId + "," + productId + ",1,10000,'WAITING',N'IT Cashier Drink " + key + "')";
                statement.executeUpdate("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status" + lifecycle);
                int orderItemId = id(connection, "SELECT MAX(OrderItemId) FROM sales.OrderItem");
                if ("COMPLETED".equals(orderStatus)) {
                    statement.executeUpdate("INSERT payment.Bill("
                            + "BranchId,CashierShiftId,Subtotal,VatAmount,TotalAmount,Status) VALUES ("
                            + branchId + "," + shiftId + ",10000,0,10000,'UNPAID')");
                    billId = id(connection, "SELECT MAX(BillId) FROM payment.Bill");
                    statement.executeUpdate("UPDATE sales.OrderItem SET BillId=" + billId
                            + ",BilledAmount=10000 WHERE OrderItemId=" + orderItemId);
                }
            }
        }
        return new Fixture(branchId, cashierId, shiftId, billId);
    }

    private int createBranch() throws Exception {
        String code = "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        execute("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES (?,N'IT Cashier Branch','00:00','23:59')",
                code);
        return scalarInt("SELECT BranchId FROM org.Branch WHERE Code=?", code);
    }

    private void insertPaidBill(Fixture fixture, BigDecimal total, LocalDateTime paidAt)
            throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT payment.Bill(BranchId,CashierShiftId,Subtotal,VatAmount,TotalAmount,"
                             + "PaidAmount,PaymentMethod,Status,PaidAt) "
                             + "VALUES (?,?,?,0,?,?, 'TRANSFER','PAID',?)")) {
            statement.setInt(1, fixture.branchId);
            statement.setInt(2, fixture.shiftId);
            statement.setBigDecimal(3, total);
            statement.setBigDecimal(4, total);
            statement.setBigDecimal(5, total);
            statement.setTimestamp(6, Timestamp.valueOf(paidAt));
            statement.executeUpdate();
        }
    }

    private static void execute(String sql, Object... args) throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            statement.executeUpdate();
        }
    }

    private static int scalarInt(String sql, Object... args) throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    private static String scalarString(String sql, Object... args) throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static int id(Connection connection, String sql, Object... args) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("Không tạo được fixture Cashier IT.");
                return result.getInt(1);
            }
        }
    }

    private static void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int index = 0; index < args.length; index++) {
            statement.setObject(index + 1, args[index]);
        }
    }

    private record Fixture(int branchId, int cashierId, int shiftId, int billId) {
    }
}
