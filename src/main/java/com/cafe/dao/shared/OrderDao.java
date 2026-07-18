package com.cafe.dao.shared;

import com.cafe.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    private static final String SELECT =
        "SELECT o.OrderId, o.BranchId, o.TableSessionId, o.CustomerId, o.Source, o.OrderType, o.Status, " +
        "       o.CreatedBy, o.CreatedAt, o.PickupCode, dt.TableNumber " +
        "FROM sales.Orders o " +
        "LEFT JOIN sales.TableSession ts ON ts.TableSessionId=o.TableSessionId " +
        "LEFT JOIN sales.DiningTable  dt ON dt.DiningTableId=ts.DiningTableId ";

    public int insert(Connection conn, Order o) throws SQLException {
        final String sql = "INSERT INTO sales.Orders(BranchId, TableSessionId, CustomerId, Source, OrderType, Status, CreatedBy) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getBranchId());
            if (o.getTableSessionId() == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, o.getTableSessionId());
            if (o.getCustomerId() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, o.getCustomerId());
            ps.setString(4, o.getSource());
            ps.setString(5, o.getOrderType() == null ? "DINE_IN" : o.getOrderType());
            ps.setString(6, o.getStatus() == null ? "ACTIVE" : o.getStatus());
            if (o.getCreatedBy() == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, o.getCreatedBy());
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

    /** Đơn đang xử lý (ACTIVE) của chi nhánh — cho Order Inbox (Cashier monitor). Mới nhất trước. */
    public List<Order> findActiveByBranch(Connection conn, int branchId) throws SQLException {
        List<Order> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE o.BranchId=? AND o.Status='ACTIVE' ORDER BY o.CreatedAt DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Số đơn đã tạo ở chi nhánh kể từ mốc (ngày kinh doanh) — để đánh số thứ tự mã gọi món.
     * Gọi ngay sau insert trong CÙNG transaction nên đã tính cả đơn vừa tạo.
     */
    public int countOrdersSince(Connection conn, int branchId, java.time.LocalDateTime sinceUtc) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM sales.Orders WHERE BranchId=? AND CreatedAt >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setTimestamp(2, Timestamp.valueOf(sinceUtc));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    /** Gán mã gọi món cho đơn (sinh lúc tạo đơn). */
    public void updatePickupCode(Connection conn, int orderId, String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sales.Orders SET PickupCode=? WHERE OrderId=?")) {
            ps.setString(1, code);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection conn, int orderId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sales.Orders SET Status=? WHERE OrderId=?")) {
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
            "UPDATE sales.Orders SET Status='COMPLETED' " +
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
                "UPDATE sales.Orders SET Status='ACTIVE' WHERE OrderId=? AND Status='COMPLETED'")) {
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
        int cu = rs.getInt("CustomerId");
        if (!rs.wasNull()) o.setCustomerId(cu);
        o.setSource(rs.getString("Source"));
        o.setOrderType(rs.getString("OrderType"));
        o.setStatus(rs.getString("Status"));
        int cb = rs.getInt("CreatedBy");
        if (!rs.wasNull()) o.setCreatedBy(cb);
        Timestamp ca = rs.getTimestamp("CreatedAt");
        if (ca != null) o.setCreatedAt(ca.toLocalDateTime());
        o.setPickupCode(rs.getString("PickupCode"));
        o.setTableNumber(rs.getString("TableNumber"));
        return o;
    }
}
