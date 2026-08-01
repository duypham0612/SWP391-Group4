package com.cafe.dao.shared;

import com.cafe.model.WasteEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/** Truy vấn metadata event đã được lặp trực tiếp trên inventory.WasteEntry. */
public class WasteEventDao {
    public record EventMetrics(int myRemakes, int myIngredientWastes, int branchRemakes) { }

    public EventMetrics loadMetrics(Connection conn, int branchId, int userId,
                                    java.time.LocalDateTime businessDayStartUtc) throws SQLException {
        final String key = "COALESCE(EventGroupId,'ENTRY:'+CONVERT(varchar(20),WasteEntryId))";
        final String sql = "SELECT "
                + "COUNT(DISTINCT CASE WHEN EventKind='REMAKE' AND CreatedBy=? THEN " + key + " END) AS MyRemakes,"
                + "COUNT(DISTINCT CASE WHEN EventKind='INGREDIENT_WASTE' AND CreatedBy=? THEN " + key + " END) AS MyWastes,"
                + "COUNT(DISTINCT CASE WHEN EventKind='REMAKE' THEN " + key + " END) AS BranchRemakes "
                + "FROM inventory.WasteEntry WHERE BranchId=? AND CreatedAt>=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, branchId);
            ps.setTimestamp(4, Timestamp.valueOf(businessDayStartUtc));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new EventMetrics(0, 0, 0);
                return new EventMetrics(rs.getInt("MyRemakes"), rs.getInt("MyWastes"), rs.getInt("BranchRemakes"));
            }
        }
    }

    public boolean existsGroup(Connection conn, int branchId, String eventGroupId) throws SQLException {
        if (eventGroupId == null || eventGroupId.isBlank()) return false;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TOP 1 1 FROM inventory.WasteEntry WHERE BranchId=? AND EventGroupId=?")) {
            ps.setInt(1, branchId);
            ps.setString(2, eventGroupId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public WasteEvent findByGroup(Connection conn, int branchId, String eventGroupId) throws SQLException {
        final String sql = "SELECT TOP 1 e.*,p.Name AS ProductName FROM inventory.WasteEntry e "
                + "LEFT JOIN catalog.Product p ON p.ProductId=e.ProductId "
                + "WHERE e.BranchId=? AND e.EventGroupId=? ORDER BY e.WasteEntryId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setString(2, eventGroupId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int updateCause(Connection conn, int branchId, String eventGroupId,
                           String causeCode, String causeDetail) throws SQLException {
        final String sql = "UPDATE inventory.WasteEntry SET CauseCode=?,CauseDetail=? "
                + "WHERE BranchId=? AND EventGroupId=? AND EventKind='INGREDIENT_WASTE' AND Source='MANUAL'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, causeCode);
            nullableString(ps, 2, causeDetail);
            ps.setInt(3, branchId);
            ps.setString(4, eventGroupId);
            return ps.executeUpdate();
        }
    }

    private static WasteEvent map(ResultSet rs) throws SQLException {
        WasteEvent event = new WasteEvent();
        event.setEventGroupId(rs.getString("EventGroupId"));
        event.setBranchId(rs.getInt("BranchId"));
        event.setEventKind(rs.getString("EventKind"));
        event.setSource(rs.getString("Source"));
        event.setProductId(nullable(rs, "ProductId"));
        event.setOrderItemId(nullable(rs, "OrderItemId"));
        event.setCupQuantity(nullable(rs, "CupQuantity"));
        event.setCauseCode(rs.getString("CauseCode"));
        event.setCauseDetail(rs.getString("CauseDetail"));
        event.setShiftAssignmentId(nullable(rs, "ShiftAssignmentId"));
        event.setCreatedBy(rs.getInt("CreatedBy"));
        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) event.setCreatedAt(createdAt.toLocalDateTime());
        event.setProductName(rs.getString("ProductName"));
        return event;
    }

    private static Integer nullable(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static void nullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) ps.setNull(index, Types.NVARCHAR); else ps.setString(index, value);
    }
}
