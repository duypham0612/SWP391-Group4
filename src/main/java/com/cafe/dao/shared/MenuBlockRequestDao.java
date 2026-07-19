package com.cafe.dao.shared;

import com.cafe.model.MenuBlockRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MenuBlockRequestDao {

    private static final String SELECT_JOIN =
        "SELECT mbr.RequestId, mbr.BranchId, mbr.ProductId, mbr.Reason, mbr.Note, mbr.BackInEta, " +
        "       mbr.RequestedBy, mbr.RequestedAt, mbr.ReopenRequestedAt, mbr.Status, " +
        "       mbr.ReviewedBy, mbr.ReviewedAt, mbr.ReviewNote, mbr.ClosedAt, " +
        "       p.Name AS ProductName, req.FullName AS RequesterName, rev.FullName AS ReviewerName " +
        "FROM catalog.MenuBlockRequest mbr " +
        "JOIN catalog.Product p ON p.ProductId = mbr.ProductId " +
        "JOIN iam.[User] req ON req.UserId = mbr.RequestedBy " +
        "LEFT JOIN iam.[User] rev ON rev.UserId = mbr.ReviewedBy ";

    public int insert(Connection conn, MenuBlockRequest r) throws SQLException {
        final String sql =
            "INSERT INTO catalog.MenuBlockRequest(BranchId, ProductId, Reason, Note, BackInEta, RequestedBy, Status) " +
            "VALUES (?,?,?,?,?,?,'PENDING')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getBranchId());
            ps.setInt(2, r.getProductId());
            ps.setString(3, r.getReason());
            if (r.getNote() == null) ps.setNull(4, Types.NVARCHAR); else ps.setString(4, r.getNote());
            ps.setTimestamp(5, Timestamp.valueOf(r.getBackInEta()));
            ps.setInt(6, r.getRequestedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public MenuBlockRequest findOpen(Connection conn, int branchId, int productId) throws SQLException {
        final String sql = SELECT_JOIN +
            "WHERE mbr.BranchId=? AND mbr.ProductId=? AND mbr.ClosedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public MenuBlockRequest findOpenById(Connection conn, int requestId, int branchId) throws SQLException {
        final String sql = SELECT_JOIN +
            "WHERE mbr.RequestId=? AND mbr.BranchId=? AND mbr.ClosedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<MenuBlockRequest> findOpenByBranch(Connection conn, int branchId) throws SQLException {
        final String sql = SELECT_JOIN +
            "WHERE mbr.BranchId=? AND mbr.ClosedAt IS NULL " +
            "ORDER BY CASE WHEN mbr.BackInEta < SYSDATETIME() THEN 0 ELSE 1 END, mbr.BackInEta";
        List<MenuBlockRequest> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<MenuBlockRequest> findHistoryByBranch(Connection conn, int branchId, int limit) throws SQLException {
        final String sql = SELECT_JOIN +
            "WHERE mbr.BranchId=? AND mbr.ClosedAt IS NOT NULL " +
            "ORDER BY mbr.ClosedAt DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        List<MenuBlockRequest> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public int markReopenRequested(Connection conn, int requestId, int branchId) throws SQLException {
        final String sql =
            "UPDATE catalog.MenuBlockRequest SET ReopenRequestedAt = SYSDATETIME() " +
            "WHERE RequestId=? AND BranchId=? AND ClosedAt IS NULL AND ReopenRequestedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.setInt(2, branchId);
            return ps.executeUpdate();
        }
    }

    public int review(Connection conn, int requestId, int branchId, String newStatus,
                      int reviewerId, String reviewNote, boolean close) throws SQLException {
        final String sql =
            "UPDATE catalog.MenuBlockRequest SET Status=?, ReviewedBy=?, ReviewedAt=SYSDATETIME(), ReviewNote=?, " +
            "       ClosedAt = CASE WHEN ? = 1 THEN SYSDATETIME() ELSE NULL END " +
            "WHERE RequestId=? AND BranchId=? AND ClosedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, reviewerId);
            if (reviewNote == null || reviewNote.isBlank()) ps.setNull(3, Types.NVARCHAR);
            else ps.setString(3, reviewNote.trim());
            ps.setBoolean(4, close);
            ps.setInt(5, requestId);
            ps.setInt(6, branchId);
            return ps.executeUpdate();
        }
    }

    private MenuBlockRequest map(ResultSet rs) throws SQLException {
        MenuBlockRequest r = new MenuBlockRequest();
        r.setRequestId(rs.getInt("RequestId"));
        r.setBranchId(rs.getInt("BranchId"));
        r.setProductId(rs.getInt("ProductId"));
        r.setReason(rs.getString("Reason"));
        r.setNote(rs.getString("Note"));
        r.setBackInEta(toLocal(rs.getTimestamp("BackInEta")));
        r.setRequestedBy(rs.getInt("RequestedBy"));
        r.setRequestedAt(toLocal(rs.getTimestamp("RequestedAt")));
        r.setReopenRequestedAt(toLocal(rs.getTimestamp("ReopenRequestedAt")));
        r.setStatus(rs.getString("Status"));
        int reviewedBy = rs.getInt("ReviewedBy");
        if (!rs.wasNull()) r.setReviewedBy(reviewedBy);
        r.setReviewedAt(toLocal(rs.getTimestamp("ReviewedAt")));
        r.setReviewNote(rs.getString("ReviewNote"));
        r.setClosedAt(toLocal(rs.getTimestamp("ClosedAt")));
        r.setProductName(rs.getString("ProductName"));
        r.setRequesterName(rs.getString("RequesterName"));
        r.setReviewerName(rs.getString("ReviewerName"));
        return r;
    }

    private LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
