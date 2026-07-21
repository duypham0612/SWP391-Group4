package com.cafe.dao.shared;

import com.cafe.model.BaristaActionLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/** Append-only audit DAO. Never UPDATE or DELETE rows from this table. */
public class BaristaActionLogDao {
    public long insert(Connection conn, int branchId, Integer shiftId, String entityType, Long entityId,
                       String actionType, String beforeJson, String afterJson, String reason,
                       Integer performedBy, String correlationId) throws SQLException {
        String sql = "INSERT INTO ops.BaristaActionLog" +
                "(BranchId,ShiftId,EntityType,EntityId,ActionType,BeforeJson,AfterJson,Reason,PerformedBy,CorrelationId)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?)";
        Integer effectiveShiftId = shiftId == null ? findOpenAttendance(conn, branchId, performedBy) : shiftId;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            if (effectiveShiftId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, effectiveShiftId);
            ps.setString(3, entityType);
            if (entityId == null) ps.setNull(4, Types.BIGINT); else ps.setLong(4, entityId);
            ps.setString(5, actionType);
            setNString(ps, 6, beforeJson);
            setNString(ps, 7, afterJson);
            setNString(ps, 8, reason);
            if (performedBy == null) ps.setNull(9, Types.INTEGER); else ps.setInt(9, performedBy);
            if (correlationId == null) ps.setNull(10, Types.VARCHAR); else ps.setString(10, correlationId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getLong(1) : 0L; }
        }
    }

    private Integer findOpenAttendance(Connection conn, int branchId, Integer userId) throws SQLException {
        if (userId == null || userId <= 0) return null;
        String sql = "SELECT TOP 1 a.AttendanceId FROM hr.Attendance a " +
                "JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=a.ShiftAssignmentId " +
                "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId " +
                "WHERE sa.UserId=? AND st.BranchId=? AND a.CheckInAt IS NOT NULL AND a.CheckOutAt IS NULL " +
                "ORDER BY a.AttendanceId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }
    }

    public List<BaristaActionLog> findByBranch(Connection conn, int branchId, int limit) throws SQLException {
        List<BaristaActionLog> out = new ArrayList<>();
        String sql = "SELECT TOP (?) l.*, u.FullName AS PerformedByName " +
                "FROM ops.BaristaActionLog l LEFT JOIN iam.[User] u ON u.UserId=l.PerformedBy " +
                "WHERE l.BranchId=? ORDER BY l.CreatedAt DESC, l.BaristaActionLogId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, Math.min(limit, 5000)));
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public List<BaristaActionLog> findAllByBranch(Connection conn, int branchId) throws SQLException {
        List<BaristaActionLog> out = new ArrayList<>();
        String sql = "SELECT l.*, u.FullName AS PerformedByName FROM ops.BaristaActionLog l " +
                "LEFT JOIN iam.[User] u ON u.UserId=l.PerformedBy WHERE l.BranchId=? " +
                "ORDER BY l.CreatedAt DESC, l.BaristaActionLogId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public List<BaristaActionLog> findByBranchSince(Connection conn, int branchId, LocalDateTime fromUtc, int limit) throws SQLException {
        List<BaristaActionLog> out = new ArrayList<>();
        String sql = "SELECT TOP (?) l.*, u.FullName AS PerformedByName FROM ops.BaristaActionLog l " +
                "LEFT JOIN iam.[User] u ON u.UserId=l.PerformedBy WHERE l.BranchId=? AND l.CreatedAt>=? " +
                "ORDER BY l.CreatedAt DESC, l.BaristaActionLogId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, Math.min(limit, 5000))); ps.setInt(2, branchId);
            ps.setTimestamp(3, Timestamp.valueOf(fromUtc));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public List<BaristaActionLog> findByEntity(Connection conn, int branchId, String entityType, long entityId) throws SQLException {
        List<BaristaActionLog> out = new ArrayList<>();
        String sql = "SELECT l.*, u.FullName AS PerformedByName FROM ops.BaristaActionLog l " +
                "LEFT JOIN iam.[User] u ON u.UserId=l.PerformedBy " +
                "WHERE l.BranchId=? AND l.EntityType=? AND l.EntityId=? " +
                "ORDER BY l.CreatedAt ASC, l.BaristaActionLogId ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setString(2, entityType); ps.setLong(3, entityId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    private static void setNString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) ps.setNull(idx, Types.NVARCHAR); else ps.setNString(idx, value);
    }

    private static BaristaActionLog map(ResultSet rs) throws SQLException {
        BaristaActionLog l = new BaristaActionLog();
        l.setActionLogId(rs.getLong("BaristaActionLogId"));
        l.setBranchId(rs.getInt("BranchId"));
        int shift = rs.getInt("ShiftId"); l.setShiftId(rs.wasNull() ? null : shift);
        l.setEntityType(rs.getString("EntityType"));
        long entity = rs.getLong("EntityId"); l.setEntityId(rs.wasNull() ? null : entity);
        l.setActionType(rs.getString("ActionType"));
        l.setBeforeJson(rs.getString("BeforeJson"));
        l.setAfterJson(rs.getString("AfterJson"));
        l.setReason(rs.getString("Reason"));
        int by = rs.getInt("PerformedBy"); l.setPerformedBy(rs.wasNull() ? null : by);
        l.setPerformedByName(rs.getString("PerformedByName"));
        l.setCorrelationId(rs.getString("CorrelationId"));
        Timestamp ts = rs.getTimestamp("CreatedAt");
        l.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        return l;
    }
}
