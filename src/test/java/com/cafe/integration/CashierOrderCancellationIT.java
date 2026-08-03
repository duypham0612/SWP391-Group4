package com.cafe.integration;

import com.cafe.service.shared.OrderService;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Huỷ món lẻ và huỷ cả đơn — hai thao tác phía THU NGÂN, chạy với SQL Server thật.
 *
 * <p>Nốt cuối của độ phủ {@code OrderIssueService}: {@link BaristaIssueWorkflowIT} phủ 12/14 method
 * nhưng bỏ lại {@code cancelItem} và {@code voidOrder} vì cả hai thuộc quầy thu ngân chứ không phải
 * quầy pha chế.
 *
 * <p>Điểm đáng kiểm nhất không phải "huỷ được hay không" mà là ba chốt dễ vỡ khi sửa code:
 * <ul>
 *   <li>{@code cancelItem} trả về CHUỖI mã, không phải boolean — mỗi mã ứng với một cách xử lý khác
 *       nhau ở tầng trên ({@code ALREADY_BILLED} phải đẩy sang nghiệp vụ hoàn tiền, không được im
 *       lặng coi như thất bại);</li>
 *   <li>{@code BLOCKED} nằm trong danh sách huỷ được — thiếu nó thì món bị chặn vì hết nguyên liệu
 *       kẹt vĩnh viễn, không lối thoát;</li>
 *   <li>{@code voidOrder} chặn huỷ khi đã có món vào pha (guard R5) — nguyên liệu đã tiêu rồi thì
 *       huỷ cả đơn là mất hàng không có sổ.</li>
 * </ul>
 */
public class CashierOrderCancellationIT extends SqlServerIntegrationSupport {

    // ── Huỷ món lẻ: các mã trả về ────────────────────────────────────────────────────────

    @Test
    void cancel_item_moves_a_waiting_item_to_cancelled() throws Exception {
        Fixture f = fixture();
        assertEquals("OK", new OrderService().cancelItem(f.itemOneId, "Khách đổi ý", f.cashierId, f.branchId));
        assertEquals("CANCELLED", itemStatus(f.itemOneId));
        assertEquals("WAITING", itemStatus(f.itemTwoId));   // món còn lại không bị đụng
    }

    @Test
    void cancel_item_also_accepts_a_blocked_item() throws Exception {
        Fixture f = fixture();
        setItemStatus(f.itemOneId, "BLOCKED");

        // Chốt quan trọng: BLOCKED phải huỷ được. Món bị chặn vì hết nguyên liệu thường kết thúc
        // bằng thu ngân huỷ + hoàn tiền; loại BLOCKED khỏi danh sách là món kẹt vĩnh viễn.
        assertEquals("OK", new OrderService().cancelItem(f.itemOneId, "Hết nguyên liệu", f.cashierId, f.branchId));
        assertEquals("CANCELLED", itemStatus(f.itemOneId));
    }

    @Test
    void cancel_item_accepts_an_item_already_being_made() throws Exception {
        Fixture f = fixture();
        setItemStatus(f.itemOneId, "MAKING");
        assertEquals("OK", new OrderService().cancelItem(f.itemOneId, "Khách huỷ", f.cashierId, f.branchId));
        assertEquals("CANCELLED", itemStatus(f.itemOneId));
    }

    @Test
    void cancel_item_reports_not_found_for_an_unknown_id() throws Exception {
        Fixture f = fixture();
        assertEquals("NOT_FOUND",
                new OrderService().cancelItem(f.itemTwoId + 1_000_000, "x", f.cashierId, f.branchId));
    }

    @Test
    void cancel_item_reports_conflict_once_the_item_is_ready() throws Exception {
        Fixture f = fixture();
        setItemStatus(f.itemOneId, "READY");
        assertEquals("CONFLICT", new OrderService().cancelItem(f.itemOneId, "x", f.cashierId, f.branchId));
        assertEquals("READY", itemStatus(f.itemOneId));
    }

    @Test
    void cancel_item_reports_conflict_for_an_item_already_served() throws Exception {
        Fixture f = fixture();
        setItemStatus(f.itemOneId, "SERVED");
        assertEquals("CONFLICT", new OrderService().cancelItem(f.itemOneId, "x", f.cashierId, f.branchId));
        assertEquals("SERVED", itemStatus(f.itemOneId));
    }

    @Test
    void cancel_item_reports_already_billed_when_the_item_sits_on_a_bill() throws Exception {
        Fixture f = fixture();
        attachToBill(f.branchId, f.itemOneId);

        // Mã riêng, không gộp vào CONFLICT: tầng trên phải đẩy sang nghiệp vụ hoàn tiền chứ không
        // được coi như thao tác hỏng rồi im lặng.
        assertEquals("ALREADY_BILLED",
                new OrderService().cancelItem(f.itemOneId, "x", f.cashierId, f.branchId));
        assertEquals("WAITING", itemStatus(f.itemOneId));
    }

    @Test
    void cancel_item_refuses_an_item_from_another_branch_session() throws Exception {
        Fixture f = fixture();

        // sessionBranchId (chi nhánh của phiên đăng nhập) khác branchId của món → guard chéo chi
        // nhánh phải chặn. Trả CONFLICT vì updateStatusIf không khớp dòng nào.
        assertEquals("CONFLICT",
                new OrderService().cancelItem(f.itemOneId, "x", f.cashierId, f.otherBranchId));
        assertEquals("WAITING", itemStatus(f.itemOneId));
    }

    // ── Huỷ món lẻ: hệ quả kèm theo ──────────────────────────────────────────────────────

    @Test
    void cancel_item_writes_an_audit_row_with_the_status_transition() throws Exception {
        Fixture f = fixture();
        new OrderService().cancelItem(f.itemOneId, "Khách đổi ý", f.cashierId, f.branchId);

        assertEquals(1, scalarInt("SELECT COUNT(*) FROM ops.ActivityLog WHERE EntityType='ORDER_ITEM' "
                + "AND EntityId=? AND ActionType='CANCEL' AND FromValue='WAITING' AND ToValue='CANCELLED'",
                f.itemOneId));
        assertEquals("Khách đổi ý", scalarString("SELECT Reason FROM ops.ActivityLog WHERE EntityType='ORDER_ITEM' "
                + "AND EntityId=? AND ActionType='CANCEL'", f.itemOneId));
    }

    @Test
    void cancel_item_strips_quotes_and_backslashes_from_the_reason() throws Exception {
        Fixture f = fixture();

        // Lý do được nhét thẳng vào JSON của outbox bằng nối chuỗi, nên sanitizeReason là thứ duy
        // nhất giữ cho payload không vỡ cú pháp.
        new OrderService().cancelItem(f.itemOneId, "Khách \"đổi\" ý\\ngay", f.cashierId, f.branchId);

        String logged = scalarString("SELECT Reason FROM ops.ActivityLog WHERE EntityType='ORDER_ITEM' "
                + "AND EntityId=? AND ActionType='CANCEL'", f.itemOneId);
        assertFalse(logged.contains("\""), "Dấu nháy kép phải bị thay: " + logged);
        assertFalse(logged.contains("\\"), "Dấu chéo ngược phải bị thay: " + logged);
    }

    @Test
    void cancelling_the_last_unfinished_item_completes_the_whole_order() throws Exception {
        Fixture f = fixture();
        setItemStatus(f.itemTwoId, "SERVED");
        assertEquals("ACTIVE", orderStatus(f.orderId));

        new OrderService().cancelItem(f.itemOneId, "Khách đổi ý", f.cashierId, f.branchId);

        // Mọi món đã SERVED/CANCELLED → đơn tự đóng. Thiếu bước này thì đơn treo ở ACTIVE mãi.
        assertEquals("COMPLETED", orderStatus(f.orderId));
    }

    @Test
    void cancelling_one_of_two_unfinished_items_leaves_the_order_active() throws Exception {
        Fixture f = fixture();
        new OrderService().cancelItem(f.itemOneId, "Khách đổi ý", f.cashierId, f.branchId);
        assertEquals("ACTIVE", orderStatus(f.orderId));
    }

    // ── Huỷ cả đơn ───────────────────────────────────────────────────────────────────────

    @Test
    void void_order_cancels_every_waiting_item_and_the_order_itself() throws Exception {
        Fixture f = fixture();
        assertTrue(new OrderService().voidOrder(f.orderId, f.cashierId, f.branchId));

        assertEquals("CANCELLED", itemStatus(f.itemOneId));
        assertEquals("CANCELLED", itemStatus(f.itemTwoId));
        assertEquals("CANCELLED", orderStatus(f.orderId));
    }

    @Test
    void void_order_is_refused_once_any_item_is_being_made() throws Exception {
        Fixture f = fixture();
        setItemStatus(f.itemOneId, "MAKING");

        // Guard R5: nguyên liệu đã tiêu cho món đang pha, huỷ cả đơn là mất hàng không có sổ.
        assertFalse(new OrderService().voidOrder(f.orderId, f.cashierId, f.branchId));

        // Và phải huỷ NGUYÊN VẸN: món WAITING còn lại cũng không được đụng tới.
        assertEquals("MAKING", itemStatus(f.itemOneId));
        assertEquals("WAITING", itemStatus(f.itemTwoId));
        assertEquals("ACTIVE", orderStatus(f.orderId));
    }

    @Test
    void void_order_is_refused_once_any_item_is_ready() throws Exception {
        Fixture f = fixture();
        setItemStatus(f.itemOneId, "READY");
        assertFalse(new OrderService().voidOrder(f.orderId, f.cashierId, f.branchId));
        assertEquals("ACTIVE", orderStatus(f.orderId));
    }

    @Test
    void void_order_is_refused_once_any_item_is_served() throws Exception {
        Fixture f = fixture();
        setItemStatus(f.itemOneId, "SERVED");
        assertFalse(new OrderService().voidOrder(f.orderId, f.cashierId, f.branchId));
        assertEquals("ACTIVE", orderStatus(f.orderId));
    }

    @Test
    void void_order_cancels_a_blocked_item_too_instead_of_orphaning_it() throws Exception {
        Fixture f = fixture();
        setItemStatus(f.itemOneId, "BLOCKED");

        // BLOCKED không nằm trong guard R5 (chưa tiêu nguyên liệu) nên đơn vẫn huỷ được, và món
        // BLOCKED phải bị huỷ theo. Bỏ sót nó thì đơn về CANCELLED mà món vẫn hiện trên bảng quầy
        // pha chế, không có đường nào thoát.
        assertTrue(new OrderService().voidOrder(f.orderId, f.cashierId, f.branchId));
        assertEquals("CANCELLED", itemStatus(f.itemOneId));
        assertEquals("CANCELLED", itemStatus(f.itemTwoId));
        assertEquals("CANCELLED", orderStatus(f.orderId));
    }

    @Test
    void void_order_refuses_an_order_from_another_branch() throws Exception {
        Fixture f = fixture();
        assertFalse(new OrderService().voidOrder(f.orderId, f.cashierId, f.otherBranchId));
        assertEquals("ACTIVE", orderStatus(f.orderId));
        assertEquals("WAITING", itemStatus(f.itemOneId));
    }

    @Test
    void void_order_refuses_an_unknown_order() throws Exception {
        Fixture f = fixture();
        assertFalse(new OrderService().voidOrder(f.orderId + 1_000_000, f.cashierId, f.branchId));
    }

    @Test
    void void_order_is_not_repeatable() throws Exception {
        Fixture f = fixture();
        assertTrue(new OrderService().voidOrder(f.orderId, f.cashierId, f.branchId));

        // Lần hai phải trả false vì đơn không còn ACTIVE — nếu không, mỗi lần bấm lại sinh thêm
        // một sự kiện outbox cho cùng một lần huỷ.
        assertFalse(new OrderService().voidOrder(f.orderId, f.cashierId, f.branchId));
        // Sự kiện của MÓN cũng mang AggregateId là orderId, nên lọc theo hình dạng payload:
        // sự kiện cấp đơn bắt đầu bằng {"orderId": còn của món là {"orderItemId":.
        assertEquals(1, scalarInt("SELECT COUNT(*) FROM ops.OutboxEvent WHERE AggregateId=CAST(? AS varchar(50)) "
                + "AND Payload LIKE '{\"orderId\":%'", f.orderId));
    }

    // ── Fixture ──────────────────────────────────────────────────────────────────────────

    private Fixture fixture() throws Exception {
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection conn = connection(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT org.Branch(Code,Name,OpenTime,CloseTime) VALUES "
                    + "('B" + key + "',N'IT Branch','00:00','23:59'),"
                    + "('C" + key + "',N'IT Branch Khac','00:00','23:59')");
            int branchId = id(conn, "SELECT BranchId FROM org.Branch WHERE Code=?", "B" + key);
            int otherBranchId = id(conn, "SELECT BranchId FROM org.Branch WHERE Code=?", "C" + key);

            st.executeUpdate("INSERT iam.UserAccount(Username,PasswordHash,FullName,RoleCode,BranchId) VALUES "
                    + "('c1" + key + "','x',N'Cashier One','CASHIER'," + branchId + ")");
            int cashier = id(conn, "SELECT UserId FROM iam.UserAccount WHERE Username=?", "c1" + key);

            st.executeUpdate("INSERT catalog.Category(Name) VALUES (N'IT Category " + key + "')");
            int category = id(conn, "SELECT CategoryId FROM catalog.Category WHERE Name=?", "IT Category " + key);
            st.executeUpdate("INSERT catalog.Product(CategoryId,Name,BasePrice) VALUES ("
                    + category + ",N'IT Drink " + key + "',10000)");
            int product = id(conn, "SELECT ProductId FROM catalog.Product WHERE Name=?", "IT Drink " + key);

            // Source='COUNTER' + CreatedBy khác NULL là cặp duy nhất CK_SalesOrder_SourceCreator cho
            // phép với đơn tại quầy; TAKEAWAY để khỏi phải dựng thêm DiningTable
            // (CK_SalesOrder_TypeDiningTable bắt DINE_IN phải có bàn).
            st.executeUpdate("INSERT sales.SalesOrder(BranchId,Source,OrderType,Status,CreatedBy,BusinessDate) VALUES ("
                    + branchId + ",'COUNTER','TAKEAWAY','ACTIVE'," + cashier
                    + ",CONVERT(date,DATEADD(hour,7,SYSUTCDATETIME())))");
            int orderId = id(conn, "SELECT MAX(OrderId) FROM sales.SalesOrder WHERE BranchId=?", branchId);

            st.executeUpdate("INSERT sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Status,ProductNameAtOrder) VALUES "
                    + "(" + orderId + "," + branchId + "," + product + ",2,10000,'WAITING',N'IT Drink " + key + "'),"
                    + "(" + orderId + "," + branchId + "," + product + ",1,10000,'WAITING',N'IT Drink " + key + "')");
            int itemOne = id(conn, "SELECT MIN(OrderItemId) FROM sales.OrderItem WHERE OrderId=?", orderId);
            int itemTwo = id(conn, "SELECT MAX(OrderItemId) FROM sales.OrderItem WHERE OrderId=?", orderId);

            return new Fixture(branchId, otherBranchId, cashier, orderId, itemOne, itemTwo);
        }
    }

    /**
     * Đặt trạng thái thẳng bằng SQL — cố ý không đi qua service để tách biệt thứ đang kiểm.
     *
     * <p>Phải ghi kèm mốc thời gian vì {@code CK_OrderItem_StatusTimestamps} bắt buộc: MAKING cần
     * {@code StartedAt}, READY cần thêm {@code DoneAt}, PICKED_UP cần {@code PickedUpAt}, SERVED
     * cần cả {@code ServedAt}. Các mốc lùi dần về quá khứ để thoả luôn bốn ràng buộc thứ tự
     * ({@code DoneAt >= StartedAt >= ...}).
     */
    private static void setItemStatus(int orderItemId, String status) throws Exception {
        String stamps = switch (status) {
            case "MAKING" -> ", StartedAt=DATEADD(minute,-30,SYSUTCDATETIME())";
            case "READY" -> ", StartedAt=DATEADD(minute,-30,SYSUTCDATETIME())"
                    + ", DoneAt=DATEADD(minute,-20,SYSUTCDATETIME())";
            case "PICKED_UP" -> ", StartedAt=DATEADD(minute,-30,SYSUTCDATETIME())"
                    + ", DoneAt=DATEADD(minute,-20,SYSUTCDATETIME())"
                    + ", PickedUpAt=DATEADD(minute,-10,SYSUTCDATETIME())";
            case "SERVED" -> ", StartedAt=DATEADD(minute,-30,SYSUTCDATETIME())"
                    + ", DoneAt=DATEADD(minute,-20,SYSUTCDATETIME())"
                    + ", PickedUpAt=DATEADD(minute,-10,SYSUTCDATETIME())"
                    + ", ServedAt=DATEADD(minute,-5,SYSUTCDATETIME())";
            default -> "";
        };
        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE sales.OrderItem SET Status=?" + stamps + " WHERE OrderItemId=?")) {
            ps.setString(1, status);
            ps.setInt(2, orderItemId);
            ps.executeUpdate();
        }
    }

    /**
     * Gắn món vào một bill. Bảng BillLine cũ đã được gộp vào {@code sales.OrderItem.BillId} khi
     * rút schema, nên "đã lên bill" nay chỉ là cột đó khác NULL.
     */
    private static void attachToBill(int branchId, int orderItemId) throws Exception {
        try (Connection conn = connection(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT payment.Bill(BranchId,Subtotal,VatAmount,DiscountAmount,TotalAmount,"
                    + "RoundingAdjustment,Status,CreatedAt) VALUES ("
                    + branchId + ",10000,0,0,10000,0,'UNPAID',SYSUTCDATETIME())");
            int billId = id(conn, "SELECT MAX(BillId) FROM payment.Bill WHERE BranchId=?", branchId);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE sales.OrderItem SET BillId=?, BilledAmount=? WHERE OrderItemId=?")) {
                ps.setInt(1, billId);
                ps.setBigDecimal(2, new java.math.BigDecimal("10000"));
                ps.setInt(3, orderItemId);
                ps.executeUpdate();
            }
        }
    }

    private static String itemStatus(int orderItemId) throws Exception {
        return scalarString("SELECT Status FROM sales.OrderItem WHERE OrderItemId=?", orderItemId);
    }

    private static String orderStatus(int orderId) throws Exception {
        return scalarString("SELECT Status FROM sales.SalesOrder WHERE OrderId=?", orderId);
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

    private record Fixture(int branchId, int otherBranchId, int cashierId, int orderId,
                           int itemOneId, int itemTwoId) { }
}
