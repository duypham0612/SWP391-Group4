package com.cafe.dao.sales;

import com.cafe.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * LÕI của bảng {@code sales.OrderItem}: hình dạng một dòng món và các thao tác mà MỌI luồng đều cần.
 *
 * <p>Ba nhóm truy vấn chuyên biệt đã tách ra file riêng, đặt tên soi gương tầng service để nhìn
 * constructor là biết màn đó đụng gì:
 * <ul>
 *   <li>{@link OrderItemQueryDao} — đọc danh sách cho từng màn (hàng chờ, sẵn lấy, đã nhận…)</li>
 *   <li>{@link OrderItemWorkflowDao} — nhận pha / pha xong / trả lại hàng chờ / thu hồi</li>
 *   <li>{@link OrderItemIssueDao} — báo sự cố, chặn, bỏ chặn, làm lại</li>
 * </ul>
 *
 * <p>Ở lại đây là những thứ không thuộc riêng nhóm nào: {@link #SELECT} + {@link #map} (hình dạng
 * dòng, ba file kia đều dùng), {@code insert}, hai truy vấn tra cứu lẻ, và các hàm đổi trạng thái
 * dùng chung. {@code pickUp} cũng ở lại vì luồng giao nhận ngoài nó ra chỉ xài
 * {@link #updateStatusIf} — tách riêng một file cho đúng một method thì rời rạc hơn chứ không rõ hơn.
 */
public class OrderItemDao {

    /**
     * Danh sách cột dùng chung cho mọi truy vấn đọc món, kể cả ở ba DAO chuyên biệt cùng package.
     * Để một bản duy nhất ở đây vì {@link #map} đọc đúng bộ cột này — tách đôi là kiểu gì cũng có
     * lúc thêm cột một bên mà quên bên kia.
     */
    static final String SELECT =
        "SELECT oi.OrderItemId, oi.OrderId, oi.BranchId, oi.ProductId, oi.Quantity, oi.UnitPrice,oi.BillId,oi.BilledAmount,oi.Note,oi.Status, " +
        "       oi.StartedAt, oi.DoneAt, oi.ServedAt, oi.BaristaId, oi.PreparedBy, " +
        "       oi.HasIssue, oi.IssueReason, oi.IssueReportedBy, oi.IssueReportedAt, " +
        "       oi.RemakeCount, oi.RemakeInventoryReserved, oi.PickedUpBy, oi.PickedUpAt, " +
        "       DATEDIFF(SECOND, o.CreatedAt, SYSUTCDATETIME()) AS WaitedSeconds, " +
        "       CASE WHEN oi.StartedAt IS NULL THEN NULL " +
        "            ELSE DATEDIFF(SECOND, oi.StartedAt, SYSUTCDATETIME()) END AS MakingSeconds, " +
        "       CASE WHEN oi.DoneAt IS NULL THEN NULL " +
        "            ELSE DATEDIFF(SECOND, oi.DoneAt, SYSUTCDATETIME()) END AS ServeWaitSeconds, " +
        "       oi.ProductNameAtOrder AS ProductName, p.PrepSeconds, c.Name AS CategoryName, o.BranchId AS OrderBranchId, " +
        "       o.OrderType, o.CreatedAt AS OrderCreatedAt, o.PickupCode, dt.TableNumber,dt.Status AS TableStatus, " +
        "       bu.FullName AS BaristaName, cu.FullName AS PreparedByName " +
        "FROM sales.OrderItem oi " +
        "JOIN catalog.Product p ON p.ProductId=oi.ProductId " +
        "JOIN catalog.Category c ON c.CategoryId=p.CategoryId " +
        "JOIN sales.SalesOrder o    ON o.OrderId=oi.OrderId " +
        "LEFT JOIN sales.DiningTable dt ON dt.DiningTableId=o.DiningTableId " +
        "LEFT JOIN iam.UserAccount bu ON bu.UserId=oi.BaristaId " +
        "LEFT JOIN iam.UserAccount cu ON cu.UserId=oi.PreparedBy ";

    public int insert(Connection conn, OrderItem it) throws SQLException {
        final String sql = "INSERT INTO sales.OrderItem(OrderId,BranchId,ProductId,Quantity,UnitPrice,Note,Status,ProductNameAtOrder) " +
                "SELECT o.OrderId,o.BranchId,?,?,?,?,?,? FROM sales.SalesOrder o WHERE o.OrderId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, it.getProductId());
            ps.setInt(2, it.getQuantity());
            ps.setBigDecimal(3, it.getUnitPrice());
            if (it.getNote() == null) ps.setNull(4, java.sql.Types.NVARCHAR); else ps.setString(4, it.getNote());
            ps.setString(5, it.getStatus() == null ? "WAITING" : it.getStatus());
            ps.setString(6, it.getProductNameAtOrder());
            ps.setInt(7, it.getOrderId());
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
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE oi.OrderId=? ORDER BY oi.OrderItemId")) {
            ps.setInt(1, orderId);
            return mapAll(ps);
        }
    }

    /** READY → PICKED_UP: nhân viên nhận món khỏi quầy để mang ra cho khách. */
    public int pickUp(Connection conn, int orderItemId, int branchId, int userId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='PICKED_UP',oi.PickedUpBy=?,oi.PickedUpAt=SYSUTCDATETIME() "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='READY'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, orderItemId); ps.setInt(3, branchId); return ps.executeUpdate();
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
        return updateStatusIf(conn, orderItemId, newStatus, expectedStatuses, branchId,
                stampStarted, stampDone, false, false, null);
    }

    /** Overload ghi nhận người pha (thẻ KPI cá nhân). */
    public int updateStatusIf(Connection conn, int orderItemId, String newStatus,
                              String[] expectedStatuses, int branchId,
                              boolean stampStarted, boolean stampDone,
                              Integer preparedBy) throws SQLException {
        return updateStatusIf(conn, orderItemId, newStatus, expectedStatuses, branchId,
                stampStarted, stampDone, false, false, preparedBy);
    }

    /** Overload đóng/gỡ dấu ServedAt (giao khách & hoàn tác giao nhầm). */
    public int updateStatusIf(Connection conn, int orderItemId, String newStatus,
                              String[] expectedStatuses, int branchId,
                              boolean stampStarted, boolean stampDone,
                              boolean stampServed, boolean clearServed) throws SQLException {
        return updateStatusIf(conn, orderItemId, newStatus, expectedStatuses, branchId,
                stampStarted, stampDone, stampServed, clearServed, null);
    }

    public int updateStatusIf(Connection conn, int orderItemId, String newStatus,
                              String[] expectedStatuses, int branchId,
                              boolean stampStarted, boolean stampDone,
                              boolean stampServed, boolean clearServed,
                              Integer preparedBy) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE oi SET oi.Status=?");
        if (stampStarted) sql.append(", oi.StartedAt=SYSUTCDATETIME()");
        if (stampDone)    sql.append(", oi.DoneAt=SYSUTCDATETIME()");
        if (stampDone && preparedBy != null && preparedBy > 0) sql.append(", oi.PreparedBy=?");
        if (stampServed)  sql.append(", oi.ServedAt=SYSUTCDATETIME()");
        if (clearServed)  sql.append(", oi.ServedAt=NULL");
        sql.append(" FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId ")
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

    /** Chạy truy vấn đã nạp đủ tham số rồi ánh xạ toàn bộ dòng. Dùng chung với ba DAO cùng package. */
    static List<OrderItem> mapAll(PreparedStatement ps) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        return out;
    }

    static OrderItem map(ResultSet rs) throws SQLException {
        OrderItem it = new OrderItem();
        it.setOrderItemId(rs.getInt("OrderItemId"));
        it.setOrderId(rs.getInt("OrderId"));
        it.setBranchId(rs.getInt("BranchId"));
        it.setProductId(rs.getInt("ProductId"));
        it.setQuantity(rs.getInt("Quantity"));
        it.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        int billId = rs.getInt("BillId"); if (!rs.wasNull()) it.setBillId(billId);
        it.setBilledAmount(rs.getBigDecimal("BilledAmount"));
        it.setNote(rs.getString("Note"));
        it.setStatus(rs.getString("Status"));
        Timestamp sa = rs.getTimestamp("StartedAt");
        if (sa != null) it.setStartedAt(sa.toLocalDateTime());
        Timestamp da = rs.getTimestamp("DoneAt");
        if (da != null) it.setDoneAt(da.toLocalDateTime());
        Timestamp se = rs.getTimestamp("ServedAt");
        if (se != null) it.setServedAt(se.toLocalDateTime());
        Timestamp oc = rs.getTimestamp("OrderCreatedAt");
        if (oc != null) it.setOrderCreatedAt(oc.toLocalDateTime());
        Timestamp ir = rs.getTimestamp("IssueReportedAt");
        if (ir != null) it.setIssueReportedAt(ir.toLocalDateTime());
        Timestamp pu = rs.getTimestamp("PickedUpAt");
        if (pu != null) it.setPickedUpAt(pu.toLocalDateTime());
        int barista = rs.getInt("BaristaId"); if (!rs.wasNull()) it.setBaristaId(barista);
        int completed = rs.getInt("PreparedBy"); if (!rs.wasNull()) it.setPreparedBy(completed);
        int issueBy = rs.getInt("IssueReportedBy"); if (!rs.wasNull()) it.setIssueReportedBy(issueBy);
        int pickedBy = rs.getInt("PickedUpBy"); if (!rs.wasNull()) it.setPickedUpBy(pickedBy);
        it.setHasIssue(rs.getBoolean("HasIssue"));
        it.setIssueReason(rs.getString("IssueReason"));
        it.setRemakeCount(rs.getInt("RemakeCount"));
        it.setRemakeInventoryReserved(rs.getBoolean("RemakeInventoryReserved"));
        it.setWaitedSeconds(rs.getInt("WaitedSeconds"));
        int makingSeconds = rs.getInt("MakingSeconds");
        it.setMakingSeconds(rs.wasNull() ? null : makingSeconds);
        int serveWaitSeconds = rs.getInt("ServeWaitSeconds");
        it.setServeWaitSeconds(rs.wasNull() ? null : serveWaitSeconds);
        it.setProductName(rs.getString("ProductName"));
        it.setPrepSeconds(rs.getInt("PrepSeconds"));
        it.setCategoryName(rs.getString("CategoryName"));
        it.setOrderType(rs.getString("OrderType"));
        it.setBaristaName(rs.getString("BaristaName"));
        it.setPreparedByName(rs.getString("PreparedByName"));
        it.setOrderBranchId(rs.getInt("OrderBranchId"));
        it.setTableNumber(rs.getString("TableNumber"));
        it.setPickupCode(rs.getString("PickupCode"));
        it.setTableStatus(rs.getString("TableStatus"));
        return it;
    }
}
