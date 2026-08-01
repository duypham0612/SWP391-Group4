package com.cafe.dao.shared;

import com.cafe.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private static final String SELECT =
        "SELECT o.OrderId, o.BranchId, o.TableSessionId, o.Source, o.OrderType, o.Status, " +
        "       o.CreatedBy, o.CreatedAt, o.BusinessDate, o.PickupCode, dt.TableNumber " +
        "FROM sales.SalesOrder o " +
        "LEFT JOIN sales.TableSession ts ON ts.TableSessionId=o.TableSessionId " +
        "LEFT JOIN sales.DiningTable  dt ON dt.DiningTableId=ts.DiningTableId ";

    public int insert(Connection conn, Order o) throws SQLException {
        final String sql = "INSERT INTO sales.SalesOrder(BranchId,TableSessionId,Source,OrderType,Status,CreatedBy,BusinessDate,PickupCode) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getBranchId());
            if (o.getTableSessionId() == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, o.getTableSessionId());
            ps.setString(3, o.getSource());
            ps.setString(4, o.getOrderType() == null ? "DINE_IN" : o.getOrderType());
            ps.setString(5, o.getStatus() == null ? "ACTIVE" : o.getStatus());
            if (o.getCreatedBy() == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, o.getCreatedBy());
            ps.setDate(7, Date.valueOf(o.getBusinessDate()));
            ps.setString(8, o.getPickupCode());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public Order findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE o.OrderId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<Order> findBySession(Connection conn, int sessionId) throws SQLException {
        List<Order> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE o.TableSessionId=? ORDER BY o.CreatedAt")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Đơn mang đi chưa thanh toán: chưa có bill hoặc đang có bill UNPAID.
     * Giữ đơn trong danh sách checkout cả khi Cashier đã mở bill rồi rời màn hình.
     */
    public List<Order> findTakeawayAwaitingPaymentByBranch(Connection conn, int branchId) throws SQLException {
        List<Order> out = new ArrayList<>();
        final String sql = SELECT +
                "WHERE o.BranchId=? AND o.OrderType='TAKEAWAY' AND o.Status<>'CANCELLED' " +
                "AND (NOT EXISTS (SELECT 1 FROM sales.OrderItem oi " +
                "JOIN payment.BillItem bi ON bi.OrderItemId=oi.OrderItemId WHERE oi.OrderId=o.OrderId) " +
                "OR EXISTS (SELECT 1 FROM sales.OrderItem oi " +
                "JOIN payment.BillItem bi ON bi.OrderItemId=oi.OrderItemId " +
                "JOIN payment.Bill b ON b.BillId=bi.BillId " +
                "WHERE oi.OrderId=o.OrderId AND b.Status='UNPAID')) " +
                "ORDER BY o.CreatedAt DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Đơn đang xử lý (ACTIVE) của chi nhánh — cho Order Inbox (Cashier monitor).
     *
     * <p>Đơn TREO (tạo trước mốc đầu ngày kinh doanh) xếp lên ĐẦU, phần còn lại giữ mới-trước như cũ.
     * Sắp thuần "mới nhất trước" đẩy đơn càng cũ càng xuống đáy — đúng chỗ không ai nhìn, trong khi
     * đơn treo lại là loại duy nhất chỉ Thu ngân xử lý được (huỷ &amp; hoàn tiền cho khách đã về).
     *
     * @param businessDayStartUtc mốc đầu ngày kinh doanh; null = không tách nhóm treo.
     */
    public List<Order> findActiveByBranch(Connection conn, int branchId,
                                          java.time.LocalDateTime businessDayStartUtc) throws SQLException {
        List<Order> out = new ArrayList<>();
        String order = businessDayStartUtc == null
                ? "ORDER BY o.CreatedAt DESC"
                : "ORDER BY CASE WHEN o.CreatedAt < ? THEN 0 ELSE 1 END, o.CreatedAt DESC";
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE o.BranchId=? AND o.Status='ACTIVE' " + order)) {
            ps.setInt(1, branchId);
            if (businessDayStartUtc != null) ps.setTimestamp(2, Timestamp.valueOf(businessDayStartUtc));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Giữ và tăng số pickup dưới key-range lock. Caller phải đang ở trong transaction tạo order.
     * HOLDLOCK khóa cả khoảng key khi ngày mới chưa có dòng, tránh hai transaction cùng INSERT số 1.
     */
    public int reservePickupSequence(Connection conn, int branchId, LocalDate businessDate)
            throws SQLException {
        String select = "SELECT NextValue FROM sales.PickupSequence WITH (UPDLOCK,HOLDLOCK) "
                + "WHERE BranchId=? AND BusinessDate=?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(businessDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int reserved = rs.getInt(1);
                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE sales.PickupSequence SET NextValue=? WHERE BranchId=? AND BusinessDate=?")) {
                        update.setInt(1, reserved + 1);
                        update.setInt(2, branchId);
                        update.setDate(3, Date.valueOf(businessDate));
                        if (update.executeUpdate() != 1) throw new SQLException("Không thể tăng PickupSequence.");
                    }
                    return reserved;
                }
            }
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO sales.PickupSequence(BranchId,BusinessDate,NextValue) VALUES (?,?,2)")) {
            insert.setInt(1, branchId);
            insert.setDate(2, Date.valueOf(businessDate));
            insert.executeUpdate();
            return 1;
        }
    }

    public void updateStatus(Connection conn, int orderId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sales.SalesOrder SET Status=? WHERE OrderId=?")) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    /**
     * ACTIVE → COMPLETED khi MỌI món của đơn đã kết thúc (SERVED/CANCELLED). NGUYÊN TỬ (1 câu UPDATE,
     * WHERE-guard NOT EXISTS chống race khi 2 barista giao món cuối song song). Trả số dòng đổi (0/1):
     * ==1 nghĩa là đơn vừa hoàn tất → caller publish order.status_changed. Không đổi nếu còn món chưa xong.
     */
    public int completeIfAllItemsFinal(Connection conn, int orderId) throws SQLException {
        final String sql =
            "UPDATE sales.SalesOrder SET Status='COMPLETED' " +
            "WHERE OrderId=? AND Status='ACTIVE' AND NOT EXISTS (" +
            "  SELECT 1 FROM sales.OrderItem oi WHERE oi.OrderId=? AND oi.Status NOT IN ('SERVED','CANCELLED'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, orderId);
            return ps.executeUpdate();
        }
    }

    /** COMPLETED → ACTIVE khi hoàn tác giao (món SERVED quay lại READY). Trả số dòng đổi (0/1). */
    public int reopenIfCompleted(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sales.SalesOrder SET Status='ACTIVE' WHERE OrderId=? AND Status='COMPLETED'")) {
            ps.setInt(1, orderId);
            return ps.executeUpdate();
        }
    }

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("OrderId"));
        o.setBranchId(rs.getInt("BranchId"));
        int ts = rs.getInt("TableSessionId");
        if (!rs.wasNull()) o.setTableSessionId(ts);
        o.setSource(rs.getString("Source"));
        o.setOrderType(rs.getString("OrderType"));
        o.setStatus(rs.getString("Status"));
        int cb = rs.getInt("CreatedBy");
        if (!rs.wasNull()) o.setCreatedBy(cb);
        Timestamp ca = rs.getTimestamp("CreatedAt");
        if (ca != null) o.setCreatedAt(ca.toLocalDateTime());
        Date businessDate = rs.getDate("BusinessDate");
        if (businessDate != null) o.setBusinessDate(businessDate.toLocalDate());
        o.setPickupCode(rs.getString("PickupCode"));
        o.setTableNumber(rs.getString("TableNumber"));
        return o;
    }
}
