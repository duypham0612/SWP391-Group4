package com.cafe.dao.shared;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Chuyển trạng thái PHA CHẾ của một dòng món: nhận pha → pha xong, và các đường trả món về hàng chờ.
 *
 * <p>Điểm chung của cả file, và là lý do nó đứng riêng: <b>mọi câu UPDATE ở đây đều mang điều kiện
 * chống tranh chấp ngay trong mệnh đề WHERE</b> — trạng thái đang kỳ vọng, chi nhánh, và với các
 * thao tác cá nhân là cả {@code BaristaId}. Không method nào ở đây được đọc rồi mới ghi. Số dòng
 * trả về (0 hoặc 1) chính là kết quả cuộc đua: caller chỉ được đi tiếp khi nhận 1.
 *
 * <p>Nhờ vậy hai barista bấm cùng lúc thì đúng một người thắng, và bên thua không trừ kho lần hai.
 */
public class OrderItemWorkflowDao {

    /** WAITING → MAKING, lưu chủ sở hữu trong cùng câu UPDATE để khóa claim. */
    public int claim(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='MAKING',oi.BaristaId=?,oi.StartedAt=SYSUTCDATETIME() "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='WAITING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, baristaId); ps.setInt(2, orderItemId); ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }

    /** Chỉ người đã nhận món mới được hoàn thành. */
    public int completeClaimed(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='READY',oi.DoneAt=SYSUTCDATETIME(),oi.PreparedBy=?,"
                + "oi.HasIssue=0,oi.IssueReason=NULL,oi.RemakeInventoryReserved=0 "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, baristaId);
            ps.setInt(2, orderItemId); ps.setInt(3, branchId); ps.setInt(4, baristaId);
            return ps.executeUpdate();
        }
    }

    /**
     * Số dòng món barista này đang giữ ở trạng thái đang pha — cổng tan ca đọc con số này.
     * Chỉ đếm MAKING: món BLOCKED đã rời hàng chờ và không còn mang tên ai, tính vào sẽ khoá
     * barista bằng thứ chính họ không gỡ được (phải chờ nhập nguyên liệu hoặc Thu ngân huỷ).
     */
    public int countMakingByBarista(Connection conn, int branchId, int baristaId) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setInt(2, baristaId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    /**
     * Thu hồi món của barista ĐÃ RỜI CA về hàng chờ. Khác {@link #returnToQueue} ở chỗ người bấm
     * không phải chủ món; vẫn guard theo chủ món ĐANG kỳ vọng để không thắng cuộc đua với chính
     * họ vừa bấm Xong (khi đó BaristaId/Status đã đổi, affected=0 → caller báo conflict).
     */
    public int reclaim(Connection conn, int orderItemId, int branchId, int expectedBaristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.BaristaId=NULL,oi.StartedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' "
                + "  AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); ps.setInt(3, expectedBaristaId);
            return ps.executeUpdate();
        }
    }

    /** Chính chủ trả món đang pha về hàng chờ cho người khác nhận. */
    public int returnToQueue(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.BaristaId=NULL,oi.StartedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); ps.setInt(3, baristaId);
            return ps.executeUpdate();
        }
    }
}
