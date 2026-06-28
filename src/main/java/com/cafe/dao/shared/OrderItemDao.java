package com.cafe.dao.shared;

import com.cafe.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDao {

    private static final String SELECT =
        "SELECT oi.OrderItemId, oi.OrderId, oi.ProductId, oi.Quantity, oi.UnitPrice, oi.Note, oi.Status, " +
        "       oi.StartedAt, oi.DoneAt, p.Name AS ProductName, o.BranchId AS OrderBranchId, dt.TableNumber " +
        "FROM sales.OrderItem oi " +
        "JOIN catalog.Product p ON p.ProductId=oi.ProductId " +
        "JOIN sales.Orders o    ON o.OrderId=oi.OrderId " +
        "LEFT JOIN sales.TableSession ts ON ts.TableSessionId=o.TableSessionId " +
        "LEFT JOIN sales.DiningTable  dt ON dt.DiningTableId=ts.DiningTableId ";

    public int insert(Connection conn, OrderItem it) throws SQLException {
        final String sql = "INSERT INTO sales.OrderItem(OrderId, ProductId, Quantity, UnitPrice, Note, Status) " +
                "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, it.getOrderId());
            ps.setInt(2, it.getProductId());
            ps.setInt(3, it.getQuantity());
            ps.setBigDecimal(4, it.getUnitPrice());
            if (it.getNote() == null) ps.setNull(5, java.sql.Types.NVARCHAR); else ps.setString(5, it.getNote());
            ps.setString(6, it.getStatus() == null ? "WAITING" : it.getStatus());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public OrderItem findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE oi.OrderItemId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<OrderItem> findByOrder(Connection conn, int orderId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE oi.OrderId=? ORDER BY oi.OrderItemId")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Hàng chờ KDS: món WAITING/MAKING của chi nhánh, đơn ACTIVE — cũ nhất trước. */
    public List<OrderItem> findKdsQueue(Connection conn, int branchId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status IN ('WAITING','MAKING') " +
            "ORDER BY CASE oi.Status WHEN 'MAKING' THEN 0 ELSE 1 END, oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Bảng lấy món: READY của chi nhánh. */
    public List<OrderItem> findReady(Connection conn, int branchId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT + "WHERE o.BranchId=? AND oi.Status='READY' ORDER BY oi.DoneAt";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các món của 1 phiên bàn (cho khách theo dõi). */
    public List<OrderItem> findBySession(Connection conn, int sessionId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT + "WHERE o.TableSessionId=? ORDER BY oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Đổi trạng thái + đóng dấu thời gian theo trạng thái mới. */
    public void updateStatus(Connection conn, int orderItemId, String status, boolean stampStarted, boolean stampDone) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE sales.OrderItem SET Status=?");
        if (stampStarted) sql.append(", StartedAt=SYSUTCDATETIME()");
        if (stampDone)    sql.append(", DoneAt=SYSUTCDATETIME()");
        sql.append(" WHERE OrderItemId=?");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, status);
            ps.setInt(2, orderItemId);
            ps.executeUpdate();
        }
    }

    private OrderItem map(ResultSet rs) throws SQLException {
        OrderItem it = new OrderItem();
        it.setOrderItemId(rs.getInt("OrderItemId"));
        it.setOrderId(rs.getInt("OrderId"));
        it.setProductId(rs.getInt("ProductId"));
        it.setQuantity(rs.getInt("Quantity"));
        it.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        it.setNote(rs.getString("Note"));
        it.setStatus(rs.getString("Status"));
        Timestamp sa = rs.getTimestamp("StartedAt");
        if (sa != null) it.setStartedAt(sa.toLocalDateTime());
        Timestamp da = rs.getTimestamp("DoneAt");
        if (da != null) it.setDoneAt(da.toLocalDateTime());
        it.setProductName(rs.getString("ProductName"));
        it.setOrderBranchId(rs.getInt("OrderBranchId"));
        it.setTableNumber(rs.getString("TableNumber"));
        return it;
    }
}
