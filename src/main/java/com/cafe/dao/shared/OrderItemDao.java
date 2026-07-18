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
        "       oi.StartedAt, oi.DoneAt, " +
        "       DATEDIFF(SECOND, o.CreatedAt, SYSUTCDATETIME()) AS WaitedSeconds, " +
        "       CASE WHEN oi.StartedAt IS NULL THEN NULL " +
        "            ELSE DATEDIFF(SECOND, oi.StartedAt, SYSUTCDATETIME()) END AS MakingSeconds, " +
        "       p.Name AS ProductName, o.BranchId AS OrderBranchId, dt.TableNumber " +
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

    /** Hàng chờ KDS: món WAITING/MAKING của chi nhánh, đơn ACTIVE — FIFO theo giờ vào đơn (đơn cũ nhất trước),
     *  ưu tiên bump (Priority) chèn lên đầu. Không đẩy MAKING lên đầu (2 cột đã tách trạng thái). */
    public List<OrderItem> findKdsQueue(Connection conn, int branchId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status IN ('WAITING','MAKING') " +
            "ORDER BY oi.Priority DESC, o.CreatedAt ASC, oi.OrderItemId";
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

    /**
     * KPI bàn giao ca (B7) trong khoảng [fromUtc, toUtc) tại chi nhánh (mốc do Service tính theo giờ VN):
     *  - Cups   = số ly PHA XONG (DoneAt trong khoảng) — KHÔNG đòi StartedAt, nên đếm cả món pha nhanh
     *             bấm READY thẳng từ WAITING (throughput không bị thiếu).
     *  - AvgSec = lead-time TB = AVG(DoneAt − StartedAt), CHỈ trên món có cả StartedAt & DoneAt
     *             (món chưa từng "bắt đầu pha" không làm loãng tốc độ pha thực).
     * Trả về {avgLeadSeconds (null → -1), cupCount}.
     */
    public long[] leadTimeStats(Connection conn, int branchId,
                                java.time.LocalDateTime fromUtc, java.time.LocalDateTime toUtc) throws SQLException {
        return leadTimeStats(conn, branchId, fromUtc, toUtc, null);
    }

    public long[] leadTimeStats(Connection conn, int branchId,
                                java.time.LocalDateTime fromUtc, java.time.LocalDateTime toUtc,
                                Integer userId) throws SQLException {
        boolean filterUser = userId != null && userId > 0;
        String userWhere = filterUser ? " AND oi.PreparedBy=? " : " ";
        final String sql =
            "SELECT " +
            "  (SELECT COUNT(*) FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId " +
            "     WHERE o.BranchId=? AND oi.DoneAt >= ? AND oi.DoneAt < ?" + userWhere + ") AS Cups, " +
            "  (SELECT AVG(CAST(DATEDIFF(SECOND, oi.StartedAt, oi.DoneAt) AS BIGINT)) " +
            "     FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId " +
            "     WHERE o.BranchId=? AND oi.StartedAt IS NOT NULL AND oi.DoneAt IS NOT NULL " +
            "       AND oi.DoneAt >= ? AND oi.DoneAt < ?" + userWhere + ") AS AvgSec";
        Timestamp from = Timestamp.valueOf(fromUtc);
        Timestamp to = Timestamp.valueOf(toUtc);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, branchId); ps.setTimestamp(idx++, from); ps.setTimestamp(idx++, to);
            if (filterUser) ps.setInt(idx++, userId);
            ps.setInt(idx++, branchId); ps.setTimestamp(idx++, from); ps.setTimestamp(idx++, to);
            if (filterUser) ps.setInt(idx++, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long cups = rs.getLong("Cups");
                    long avg = rs.getLong("AvgSec");
                    boolean avgNull = rs.wasNull();
                    return new long[]{ avgNull ? -1L : avg, cups };
                }
            }
        }
        return new long[]{ -1L, 0L };
    }

    /** Bump (B1): đẩy món lên đầu hàng chờ — Priority = max hiện tại CỦA CHI NHÁNH + 1 (không nhảy chéo chi nhánh). */
    public void bump(Connection conn, int orderItemId, int branchId) throws SQLException {
        final String sql =
            "UPDATE oi SET oi.Priority = (" +
            "    SELECT ISNULL(MAX(oi2.Priority),0)+1 FROM sales.OrderItem oi2 " +
            "    JOIN sales.Orders o2 ON o2.OrderId=oi2.OrderId WHERE o2.BranchId=?) " +
            "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId " +
            "WHERE oi.OrderItemId=? AND o.BranchId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, orderItemId);
            ps.setInt(3, branchId);
            ps.executeUpdate();
        }
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

    /**
     * ★ Đổi trạng thái CÓ ĐIỀU KIỆN + scope chi nhánh, NGUYÊN TỬ (một câu UPDATE ở DB).
     * Chỉ đổi nếu món đang ở một trong {@code expectedStatuses} VÀ thuộc {@code branchId}.
     * Trả số dòng đổi (0 hoặc 1). Dùng làm hàng rào chống double-deduct khi 2 barista thao tác
     * song song: chỉ request thắng cuộc "claim" được món (rows==1) mới trừ kho.
     */
    public int updateStatusIf(Connection conn, int orderItemId, String newStatus,
                              String[] expectedStatuses, int branchId,
                              boolean stampStarted, boolean stampDone) throws SQLException {
        return updateStatusIf(conn, orderItemId, newStatus, expectedStatuses, branchId, stampStarted, stampDone, null);
    }

    public int updateStatusIf(Connection conn, int orderItemId, String newStatus,
                              String[] expectedStatuses, int branchId,
                              boolean stampStarted, boolean stampDone,
                              Integer preparedBy) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE oi SET oi.Status=?");
        if (stampStarted) sql.append(", oi.StartedAt=SYSUTCDATETIME()");
        if (stampDone)    sql.append(", oi.DoneAt=SYSUTCDATETIME()");
        if (stampDone && preparedBy != null && preparedBy > 0) sql.append(", oi.PreparedBy=?");
        sql.append(" FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId ")
           .append("WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status IN (");
        for (int i = 0; i < expectedStatuses.length; i++) sql.append(i == 0 ? "?" : ",?");
        sql.append(")");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, newStatus);
            if (stampDone && preparedBy != null && preparedBy > 0) ps.setInt(idx++, preparedBy);
            ps.setInt(idx++, orderItemId);
            ps.setInt(idx++, branchId);
            for (String s : expectedStatuses) ps.setString(idx++, s);
            return ps.executeUpdate();
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
        it.setWaitedSeconds(rs.getInt("WaitedSeconds"));
        int makingSeconds = rs.getInt("MakingSeconds");
        it.setMakingSeconds(rs.wasNull() ? null : makingSeconds);
        it.setProductName(rs.getString("ProductName"));
        it.setOrderBranchId(rs.getInt("OrderBranchId"));
        it.setTableNumber(rs.getString("TableNumber"));
        return it;
    }
}
