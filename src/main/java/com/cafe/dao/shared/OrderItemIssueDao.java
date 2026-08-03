package com.cafe.dao.shared;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Sự cố của một dòng món: gắn cờ, chặn/bỏ chặn, và vòng làm lại.
 *
 * <p>Ba mức độ, đọc từ nhẹ tới nặng và ĐỪNG lẫn vào nhau:
 * <ul>
 *   <li>{@link #reportIssue} — chỉ gắn cờ, món GIỮ NGUYÊN trạng thái và vẫn nằm trong hàng chờ;</li>
 *   <li>{@link #blockItem} — món RỜI hàng chờ sang BLOCKED, nhả luôn người nhận, nên người khác
 *       không bấm "Nhận pha" rồi lại gặp đúng vấn đề đó;</li>
 *   <li>{@link #beginRemake} / {@link #finishRemake} — món đã pha xong nhưng phải pha lại từ đầu.</li>
 * </ul>
 *
 * <p>REMAKE là trạng thái CHUYỂN TIẾP, chỉ tồn tại giữa {@code beginRemake} và {@code finishRemake}
 * trong cùng một giao dịch — giao dịch khác không bao giờ quan sát được nó. Nó ở đó để hai người
 * không cùng tạo một lượt làm lại.
 */
public class OrderItemIssueDao {

    /** Gắn cờ sự cố nhưng giữ trạng thái để card không biến mất khỏi người đang xử lý. */
    public int reportIssue(Connection conn, int orderItemId, int branchId, int userId, String reason) throws SQLException {
        final String sql = "UPDATE oi SET oi.HasIssue=1,oi.IssueReason=?,oi.IssueReportedBy=?,oi.IssueReportedAt=SYSUTCDATETIME() "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status IN ('WAITING','MAKING') "
                + "AND (oi.Status='WAITING' OR oi.BaristaId=?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason); ps.setInt(2, userId); ps.setInt(3, orderItemId);
            ps.setInt(4, branchId); ps.setInt(5, userId);
            return ps.executeUpdate();
        }
    }

    /**
     * WAITING/MAKING → BLOCKED: món không pha được (hết nguyên liệu, hỏng máy, ngừng bán).
     * Nhả luôn người nhận + mốc bắt đầu vì món đã rời khỏi luồng pha; giữ lý do để hiện ở khu "Cần xử lý".
     * Guard giống reportIssue: món đang pha thì chỉ chính chủ được chặn.
     */
    public int blockItem(Connection conn, int orderItemId, int branchId, int userId, String reason) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='BLOCKED',oi.HasIssue=1,oi.IssueReason=?,"
                + "oi.IssueReportedBy=?,oi.IssueReportedAt=SYSUTCDATETIME(),oi.BaristaId=NULL,oi.StartedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' "
                + "AND oi.Status IN ('WAITING','MAKING') AND (oi.Status='WAITING' OR oi.BaristaId=?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason); ps.setInt(2, userId); ps.setInt(3, orderItemId);
            ps.setInt(4, branchId); ps.setInt(5, userId);
            return ps.executeUpdate();
        }
    }

    /** BLOCKED → WAITING: nguyên liệu/máy đã có lại, trả món về hàng chờ và xoá sạch cờ sự cố. */
    public int unblockItem(Connection conn, int orderItemId, int branchId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.HasIssue=0,oi.IssueReason=NULL,"
                + "oi.IssueReportedBy=NULL,oi.IssueReportedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='BLOCKED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId);
            return ps.executeUpdate();
        }
    }

    /** Đếm các dòng món BLOCKED còn lại trong chi nhánh có dùng một trong các nguyên liệu vừa kiểm kê. */
    public int countBlockedUsingIngredients(Connection conn, int branchId,
                                            java.util.Collection<Integer> ingredientIds) throws SQLException {
        if (ingredientIds == null || ingredientIds.isEmpty()) return 0;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ingredientIds.size(); i++) in.append(i == 0 ? "?" : ",?");
        final String sql =
            "SELECT COUNT(DISTINCT oi.OrderItemId) " +
            "FROM sales.OrderItem oi " +
            "JOIN sales.SalesOrder o ON o.OrderId = oi.OrderId " +
            "JOIN catalog.Recipe pr ON pr.OwnerType='PRODUCT' AND pr.OwnerId=oi.ProductId " +
            "WHERE o.BranchId = ? AND o.Status = 'ACTIVE' AND oi.Status = 'BLOCKED' " +
            "AND pr.IngredientId IN (" + in + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, branchId);
            for (Integer ingredientId : ingredientIds) ps.setInt(idx++, ingredientId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    /** READY → REMAKE là claim chuyển tiếp, chống hai người tạo remake trùng. */
    public int beginRemake(Connection conn, int orderItemId, int branchId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='REMAKE' FROM sales.OrderItem oi "
                + "JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='READY'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); return ps.executeUpdate();
        }
    }

    /** MAKING → REMAKE chỉ barista đang giữ món được báo pha lỗi/làm lại. */
    public int beginRemakeClaimed(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='REMAKE' FROM sales.OrderItem oi "
                + "JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); ps.setInt(3, baristaId); return ps.executeUpdate();
        }
    }

    /**
     * REMAKE → WAITING với ưu tiên làm lại. {@code inventoryReserved} quyết định lần bấm Xong kế
     * tiếp có trừ kho nữa hay không — quy tắc ở {@link com.cafe.common.RemakeReservation}.
     */
    public void finishRemake(Connection conn, int orderItemId, int branchId, boolean inventoryReserved)
            throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.Priority=(SELECT ISNULL(MAX(x.Priority),0)+1 "
                + "FROM sales.OrderItem x JOIN sales.SalesOrder xo ON xo.OrderId=x.OrderId WHERE xo.BranchId=?),"
                + "oi.RemakeCount=oi.RemakeCount+1,oi.RemakeInventoryReserved=?,oi.BaristaId=NULL,oi.PreparedBy=NULL,"
                + "oi.StartedAt=NULL,oi.DoneAt=NULL,oi.HasIssue=0,oi.IssueReason=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='REMAKE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setBoolean(2, inventoryReserved);
            ps.setInt(3, orderItemId); ps.setInt(4, branchId); ps.executeUpdate();
        }
    }
}
