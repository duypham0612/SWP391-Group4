package com.cafe.dao.ops;

import com.cafe.model.WasteEventAudit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** DAO dùng chung cho nhật ký nghiệp vụ append-only trong ops.ActivityLog. */
public class ActivityLogDao {
    public void insertOrderItem(Connection connection, int orderItemId, int branchId, String action,
                                String fromStatus, String toStatus, String reason, Integer performedBy)
            throws SQLException {
        insert(connection, "ORDER_ITEM", orderItemId, branchId, action,
                fromStatus, toStatus, reason, performedBy);
    }

    public void insertWasteEntry(Connection connection, long wasteEntryId, int branchId, String action,
                                 String before, String after, String reason, int userId)
            throws SQLException {
        // EntityType khớp tên bảng nghiệp vụ inventory.WasteEntry; EntityId là WasteEntryId.
        insert(connection, "WASTE_ENTRY", wasteEntryId, branchId, action,
                before, after, reason, userId);
    }

    private void insert(Connection connection, String entityType, long entityId, int branchId,
                        String action, String before, String after, String reason, Integer userId)
            throws SQLException {
        final String sql = "INSERT INTO ops.ActivityLog"
                + "(EntityType,EntityId,BranchId,ActionType,FromValue,ToValue,Reason,PerformedBy) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entityType);
            statement.setLong(2, entityId);
            statement.setInt(3, branchId);
            statement.setString(4, action);
            text(statement, 5, before);
            text(statement, 6, after);
            text(statement, 7, reason);
            if (userId == null) statement.setNull(8, Types.INTEGER);
            else statement.setInt(8, userId);
            statement.executeUpdate();
        }
    }

    /** Các thao tác sửa/hủy hao hụt của chi nhánh, mới nhất trước. */
    public List<WasteEventAudit> findWasteCorrectionsByBranchBetween(
            Connection connection, int branchId, LocalDateTime fromUtc, LocalDateTime toUtc, int limit)
            throws SQLException {
        final String sql = "SELECT a.ActivityLogId AS WasteEventAuditId,"
                + "a.EntityId AS WasteEntryId,w.EventGroupId,"
                + "a.ActionType,a.FromValue AS BeforeValue,a.ToValue AS AfterValue,"
                + "a.Reason,a.PerformedBy,a.PerformedAt,u.FullName AS PerformedByName,"
                + "i.Name AS IngredientName,i.Unit AS IngredientUnit,w.WasteType "
                + "FROM ops.ActivityLog a "
                + "LEFT JOIN inventory.WasteEntry w ON w.WasteEntryId=a.EntityId "
                + "LEFT JOIN catalog.Ingredient i ON i.IngredientId=w.IngredientId "
                + "JOIN iam.UserAccount u ON u.UserId=a.PerformedBy "
                + "WHERE a.EntityType='WASTE_ENTRY' "
                + "AND COALESCE(a.BranchId,w.BranchId)=? AND a.ActionType<>'CREATE' "
                + "AND a.PerformedAt>=? AND a.PerformedAt<? "
                + "ORDER BY a.PerformedAt DESC, a.ActivityLogId DESC "
                + "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        List<WasteEventAudit> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, branchId);
            statement.setTimestamp(2, Timestamp.valueOf(fromUtc));
            statement.setTimestamp(3, Timestamp.valueOf(toUtc));
            statement.setInt(4, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) result.add(mapWasteAudit(rs));
            }
        }
        return result;
    }

    private static WasteEventAudit mapWasteAudit(ResultSet rs) throws SQLException {
        WasteEventAudit audit = new WasteEventAudit();
        audit.setWasteEventAuditId(rs.getLong("WasteEventAuditId"));
        audit.setActionType(rs.getString("ActionType"));
        audit.setBeforeValue(rs.getString("BeforeValue"));
        audit.setAfterValue(rs.getString("AfterValue"));
        audit.setReason(rs.getString("Reason"));
        audit.setPerformedBy(rs.getInt("PerformedBy"));
        Timestamp performedAt = rs.getTimestamp("PerformedAt");
        if (performedAt != null) audit.setPerformedAt(performedAt.toLocalDateTime());
        audit.setPerformedByName(rs.getString("PerformedByName"));
        audit.setIngredientName(rs.getString("IngredientName"));
        audit.setIngredientUnit(rs.getString("IngredientUnit"));
        audit.setWasteType(rs.getString("WasteType"));
        return audit;
    }

    private static void text(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) statement.setNull(index, Types.NVARCHAR);
        else statement.setString(index, value);
    }
}
