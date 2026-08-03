package com.cafe.dao.inventory;

import com.cafe.model.WasteEventReview;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Review lifecycle nằm trực tiếp trên từng inventory.WasteEntry. */
public class WasteEventReviewDao {
    public void open(Connection conn, long wasteEntryId, String type,
                     BigDecimal before, BigDecimal after, String note) throws SQLException {
        final String sql = "UPDATE inventory.WasteEntry SET ReviewType=?,ReviewStatus='OPEN',"
                + "QtyBefore=?,QtyAfter=?,ReviewNote=? WHERE WasteEntryId=? AND ReviewStatus IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setBigDecimal(2, before);
            ps.setBigDecimal(3, after);
            if (note == null || note.isBlank()) ps.setNull(4, Types.NVARCHAR); else ps.setString(4, note);
            ps.setLong(5, wasteEntryId);
            ps.executeUpdate();
        }
    }

    public List<WasteEventReview> findOpenByBranch(Connection conn, int branchId) throws SQLException {
        final String sql = "SELECT e.WasteEntryId,e.EventGroupId,e.IngredientId,i.Name AS IngredientName,"
                + "e.ReviewType,e.QtyBefore,e.QtyAfter,e.ReviewStatus,e.ReviewNote,e.CreatedAt "
                + "FROM inventory.WasteEntry e JOIN catalog.Ingredient i ON i.IngredientId=e.IngredientId "
                + "WHERE e.BranchId=? AND e.ReviewStatus='OPEN' ORDER BY e.CreatedAt DESC,e.WasteEntryId DESC";
        List<WasteEventReview> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Resolve atomically; returned id chính là WasteEntryId đang được review. */
    public Long resolveReturningEntryId(Connection conn, int branchId, long wasteEntryId,
                                        int managerId, String note) throws SQLException {
        final String sql = "DECLARE @resolved TABLE(WasteEntryId BIGINT); "
                + "UPDATE inventory.WasteEntry SET ReviewStatus='RESOLVED',ResolvedBy=?,"
                + "ResolvedAt=SYSUTCDATETIME(),ResolutionNote=? "
                + "OUTPUT inserted.WasteEntryId INTO @resolved(WasteEntryId) "
                + "WHERE WasteEntryId=? AND BranchId=? AND ReviewStatus='OPEN'; "
                + "SELECT WasteEntryId FROM @resolved;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, managerId);
            if (note == null || note.isBlank()) ps.setNull(2, Types.NVARCHAR); else ps.setString(2, note);
            ps.setLong(3, wasteEntryId);
            ps.setInt(4, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : null; }
        }
    }

    private static WasteEventReview map(ResultSet rs) throws SQLException {
        WasteEventReview review = new WasteEventReview();
        review.setWasteEntryId(rs.getLong("WasteEntryId"));
        review.setEventGroupId(rs.getString("EventGroupId"));
        review.setIngredientId(rs.getInt("IngredientId"));
        review.setIngredientName(rs.getString("IngredientName"));
        review.setReviewType(rs.getString("ReviewType"));
        review.setQtyBefore(rs.getBigDecimal("QtyBefore"));
        review.setQtyAfter(rs.getBigDecimal("QtyAfter"));
        review.setStatus(rs.getString("ReviewStatus"));
        review.setNote(rs.getString("ReviewNote"));
        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) review.setCreatedAt(createdAt.toLocalDateTime());
        return review;
    }
}
