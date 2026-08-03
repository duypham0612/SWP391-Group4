package com.cafe.dao.catalog;

import com.cafe.model.MenuBlockRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Adapter vòng đời yêu cầu chặn món được lưu trực tiếp trên catalog.BranchMenu. */
public class MenuBlockRequestDao {
    private static final String SELECT_OPEN =
            "SELECT bm.BranchId,bm.ProductId,bm.BlockReason AS Reason,bm.BlockNote AS Note,bm.BackInEta,"
            + "bm.BlockRequestedBy AS RequestedBy,bm.BlockRequestedAt AS RequestedAt,"
            + "bm.BlockReopenRequestedAt AS ReopenRequestedAt,bm.BlockStatus AS Status,"
            + "bm.BlockReviewedBy AS ReviewedBy,bm.BlockReviewedAt AS ReviewedAt,"
            + "bm.BlockReviewNote AS ReviewNote,p.Name AS ProductName,"
            + "req.FullName AS RequesterName,rev.FullName AS ReviewerName "
            + "FROM catalog.BranchMenu bm JOIN catalog.Product p ON p.ProductId=bm.ProductId "
            + "JOIN iam.UserAccount req ON req.UserId=bm.BlockRequestedBy "
            + "LEFT JOIN iam.UserAccount rev ON rev.UserId=bm.BlockReviewedBy ";

    /** ProductId chính là khóa nghiệp vụ thay cho MenuBlockRequestId đã bị xoá. */
    public int insert(Connection conn, MenuBlockRequest request) throws SQLException {
        String sql = "UPDATE catalog.BranchMenu SET BlockReason=?,BlockNote=?,BlockRequestedBy=?,"
                + "BlockRequestedAt=SYSUTCDATETIME(),BlockReopenRequestedAt=NULL,BlockStatus='PENDING',"
                + "BlockReviewedBy=NULL,BlockReviewedAt=NULL,BlockReviewNote=NULL "
                + "WHERE BranchId=? AND ProductId=? AND BlockStatus IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.getReason());
            if (request.getNote() == null) ps.setNull(2, Types.NVARCHAR);
            else ps.setString(2, request.getNote());
            ps.setInt(3, request.getRequestedBy());
            ps.setInt(4, request.getBranchId());
            ps.setInt(5, request.getProductId());
            if (ps.executeUpdate() != 1) return 0;
            return request.getProductId();
        }
    }

    public MenuBlockRequest findOpen(Connection conn, int branchId, int productId) throws SQLException {
        return find(conn, "bm.BranchId=? AND bm.ProductId=? AND bm.BlockStatus IS NOT NULL",
                branchId, productId);
    }

    public MenuBlockRequest findOpenById(Connection conn, int productId, int branchId) throws SQLException {
        return findOpen(conn, branchId, productId);
    }

    private MenuBlockRequest find(Connection conn, String predicate, int first, int second)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_OPEN + "WHERE " + predicate)) {
            ps.setInt(1, first);
            ps.setInt(2, second);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapOpen(rs) : null; }
        }
    }

    public List<MenuBlockRequest> findOpenByBranch(Connection conn, int branchId) throws SQLException {
        List<MenuBlockRequest> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_OPEN
                + "WHERE bm.BranchId=? AND bm.BlockStatus IS NOT NULL "
                + "ORDER BY bm.BlockRequestedAt DESC,bm.ProductId")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(mapOpen(rs)); }
        }
        return out;
    }

    public List<MenuBlockRequest> findHistoryByBranch(Connection conn, int branchId, int limit)
            throws SQLException {
        String sql = "SELECT TOP (?) al.EntityId AS ProductId,al.BranchId,al.FromValue,al.ToValue,"
                + "al.Reason,al.PerformedAt,p.Name AS ProductName,u.FullName AS ReviewerName "
                + "FROM ops.ActivityLog al JOIN catalog.Product p ON p.ProductId=al.EntityId "
                + "LEFT JOIN iam.UserAccount u ON u.UserId=al.PerformedBy "
                + "WHERE al.EntityType='MENU_BLOCK' AND al.BranchId=? "
                + "ORDER BY al.PerformedAt DESC,al.ActivityLogId DESC";
        List<MenuBlockRequest> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MenuBlockRequest row = new MenuBlockRequest();
                    row.setMenuBlockRequestId(rs.getInt("ProductId"));
                    row.setProductId(rs.getInt("ProductId"));
                    row.setBranchId(rs.getInt("BranchId"));
                    row.setStatus(rs.getString("ToValue"));
                    row.setReviewNote(rs.getString("Reason"));
                    Timestamp closed = rs.getTimestamp("PerformedAt");
                    if (closed != null) row.setClosedAt(closed.toLocalDateTime());
                    row.setProductName(rs.getString("ProductName"));
                    row.setReviewerName(rs.getString("ReviewerName"));
                    out.add(row);
                }
            }
        }
        return out;
    }

    public int markReopenRequested(Connection conn, int productId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE catalog.BranchMenu SET BlockReopenRequestedAt=SYSUTCDATETIME() "
                        + "WHERE ProductId=? AND BranchId=? AND BlockStatus IS NOT NULL "
                        + "AND BlockReopenRequestedAt IS NULL")) {
            ps.setInt(1, productId);
            ps.setInt(2, branchId);
            return ps.executeUpdate();
        }
    }

    /** Ghi lịch sử và reset Block*; hai thao tác chạy trong transaction của service. */
    public int review(Connection conn, int productId, int branchId, String newStatus,
                      int reviewerId, String reviewNote, boolean close) throws SQLException {
        if (!close) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE catalog.BranchMenu SET BlockStatus=?,BlockReviewedBy=?,"
                            + "BlockReviewedAt=SYSUTCDATETIME(),BlockReviewNote=? "
                            + "WHERE ProductId=? AND BranchId=? AND BlockStatus IS NOT NULL")) {
                ps.setString(1, newStatus);
                ps.setInt(2, reviewerId);
                if (reviewNote == null) ps.setNull(3, Types.NVARCHAR); else ps.setString(3, reviewNote);
                ps.setInt(4, productId);
                ps.setInt(5, branchId);
                return ps.executeUpdate();
            }
        }
        String fromStatus = findStatusForUpdate(conn, branchId, productId);
        if (fromStatus == null) return 0;
        // Với yêu cầu PENDING bị đóng ngay, ghi reviewer qua trạng thái APPROVED tạm trong cùng tx
        // để TR_BranchMenu_BlockActor kiểm tra manager/chi nhánh trước khi reset Block*.
        if ("PENDING".equals(fromStatus)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE catalog.BranchMenu SET BlockStatus='APPROVED',BlockReviewedBy=?,"
                            + "BlockReviewedAt=SYSUTCDATETIME(),BlockReviewNote=? "
                            + "WHERE ProductId=? AND BranchId=? AND BlockStatus='PENDING'")) {
                ps.setInt(1, reviewerId);
                if (reviewNote == null) ps.setNull(2, Types.NVARCHAR); else ps.setString(2, reviewNote);
                ps.setInt(3, productId);
                ps.setInt(4, branchId);
                if (ps.executeUpdate() != 1) return 0;
            }
        }
        String action = "REJECTED".equals(newStatus) ? "BLOCK_REJECTED" : "BLOCK_RESOLVED";
        if (insertActivity(conn, branchId, productId, action, fromStatus,
                newStatus, reviewNote, reviewerId) != 1)
            return 0;
        return clearBlock(conn, branchId, productId);
    }

    public int closeOpenByProduct(Connection conn, int branchId, int productId, String note)
            throws SQLException {
        String fromStatus = findStatusForUpdate(conn, branchId, productId);
        if (fromStatus == null) return 0;
        if (insertActivity(conn, branchId, productId, "BLOCK_MENU_REMOVED", fromStatus,
                "REMOVED", note, null) != 1)
            return 0;
        return clearBlock(conn, branchId, productId);
    }

    private int insertActivity(Connection conn, int branchId, int productId, String action,
                               String fromStatus, String toStatus, String reason,
                               Integer performedBy) throws SQLException {
        String sql = "INSERT INTO ops.ActivityLog(EntityType,EntityId,BranchId,ActionType,FromValue,"
                + "ToValue,Reason,PerformedBy) SELECT 'MENU_BLOCK',ProductId,BranchId,?,?,?,?,? "
                + "FROM catalog.BranchMenu WHERE BranchId=? AND ProductId=? AND BlockStatus IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, fromStatus);
            ps.setString(3, toStatus);
            if (reason == null || reason.isBlank()) ps.setNull(4, Types.NVARCHAR); else ps.setString(4, reason.trim());
            if (performedBy == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, performedBy);
            ps.setInt(6, branchId);
            ps.setInt(7, productId);
            return ps.executeUpdate();
        }
    }

    private String findStatusForUpdate(Connection conn, int branchId, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT BlockStatus FROM catalog.BranchMenu WITH (UPDLOCK,HOLDLOCK) "
                        + "WHERE BranchId=? AND ProductId=? AND BlockStatus IS NOT NULL")) {
            ps.setInt(1, branchId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    private int clearBlock(Connection conn, int branchId, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE catalog.BranchMenu SET BlockReason=NULL,BlockNote=NULL,BlockRequestedBy=NULL,"
                        + "BlockRequestedAt=NULL,BlockReopenRequestedAt=NULL,BlockStatus=NULL,"
                        + "BlockReviewedBy=NULL,BlockReviewedAt=NULL,BlockReviewNote=NULL "
                        + "WHERE BranchId=? AND ProductId=? AND BlockStatus IS NOT NULL")) {
            ps.setInt(1, branchId);
            ps.setInt(2, productId);
            return ps.executeUpdate();
        }
    }

    private MenuBlockRequest mapOpen(ResultSet rs) throws SQLException {
        MenuBlockRequest row = new MenuBlockRequest();
        row.setMenuBlockRequestId(rs.getInt("ProductId"));
        row.setBranchId(rs.getInt("BranchId"));
        row.setProductId(rs.getInt("ProductId"));
        row.setReason(rs.getString("Reason"));
        row.setNote(rs.getString("Note"));
        Timestamp eta = rs.getTimestamp("BackInEta");
        if (eta != null) row.setBackInEta(eta.toLocalDateTime());
        row.setRequestedBy(rs.getInt("RequestedBy"));
        Timestamp requested = rs.getTimestamp("RequestedAt");
        if (requested != null) row.setRequestedAt(requested.toLocalDateTime());
        Timestamp reopen = rs.getTimestamp("ReopenRequestedAt");
        if (reopen != null) row.setReopenRequestedAt(reopen.toLocalDateTime());
        row.setStatus(rs.getString("Status"));
        int reviewedBy = rs.getInt("ReviewedBy");
        row.setReviewedBy(rs.wasNull() ? null : reviewedBy);
        Timestamp reviewed = rs.getTimestamp("ReviewedAt");
        if (reviewed != null) row.setReviewedAt(reviewed.toLocalDateTime());
        row.setReviewNote(rs.getString("ReviewNote"));
        row.setProductName(rs.getString("ProductName"));
        row.setRequesterName(rs.getString("RequesterName"));
        row.setReviewerName(rs.getString("ReviewerName"));
        return row;
    }
}
