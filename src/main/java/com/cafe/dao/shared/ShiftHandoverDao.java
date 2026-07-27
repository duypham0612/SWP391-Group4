package com.cafe.dao.shared;

import com.cafe.model.ShiftHandover;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Canonical DAO for hr.ShiftHandover. */
public class ShiftHandoverDao {
    private static final String HANDOVER_COLUMNS =
        "sh.ShiftHandoverId, sh.BranchId, sh.Note, sh.CreatedBy, sh.SourceShiftAssignmentId, sh.OverallStatus, sh.CreatedAt, " +
        "u.FullName AS CreatedByName, st.Name AS SourceTemplateName, st.StartTime AS SourceStartTime, st.EndTime AS SourceEndTime ";
    private static final String HANDOVER_FROM =
        "FROM hr.ShiftHandover sh JOIN iam.[User] u ON u.UserId=sh.CreatedBy " +
        "LEFT JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=sh.SourceShiftAssignmentId " +
        "LEFT JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId ";

    public boolean existsInBranch(Connection conn, int handoverId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM hr.ShiftHandover WHERE ShiftHandoverId=? AND BranchId=?")) {
            ps.setInt(1, handoverId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsForSourceAssignment(Connection conn, int sourceAssignmentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM hr.ShiftHandover WHERE SourceShiftAssignmentId=?")) {
            ps.setInt(1, sourceAssignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(Connection conn, int branchId, String note, int createdBy, int sourceAssignmentId)
            throws SQLException {
        final String sql = "INSERT INTO hr.ShiftHandover"
                + "(BranchId, Note, CreatedBy, SourceShiftAssignmentId, OverallStatus) "
                + "VALUES (?,?,?,?, 'WAITING_RECEIPT')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setString(2, note);
            ps.setInt(3, createdBy);
            ps.setInt(4, sourceAssignmentId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    private static String filterSql(String scope, String status, String query) {
        StringBuilder sql = new StringBuilder(" WHERE sh.BranchId=?");
        if ("MINE".equals(scope)) {
            sql.append(" AND EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient rf "
                    + "WHERE rf.ShiftHandoverId=sh.ShiftHandoverId AND rf.RecipientUserId=?)");
        } else if ("SENT".equals(scope)) {
            sql.append(" AND sh.CreatedBy=?");
        }
        if (status != null && !status.isEmpty()) sql.append(" AND sh.OverallStatus=?");
        if (query != null && !query.isEmpty()) {
            sql.append(" AND (u.FullName LIKE ? OR sh.Note LIKE ? OR EXISTS "
                    + "(SELECT 1 FROM hr.ShiftHandoverTask tf "
                    + "WHERE tf.ShiftHandoverId=sh.ShiftHandoverId AND tf.Content LIKE ?))");
        }
        return sql.toString();
    }

    private static int bindFilter(PreparedStatement ps, int index, int branchId, int userId,
                                  String scope, String status, String query) throws SQLException {
        ps.setInt(index++, branchId);
        if ("MINE".equals(scope) || "SENT".equals(scope)) ps.setInt(index++, userId);
        if (status != null && !status.isEmpty()) ps.setString(index++, status);
        if (query != null && !query.isEmpty()) {
            String like = "%" + query + "%";
            ps.setString(index++, like);
            ps.setString(index++, like);
            ps.setString(index++, like);
        }
        return index;
    }

    public int countByFilter(Connection conn, int branchId, int userId, String scope,
                             String status, String query) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM hr.ShiftHandover sh "
                + "JOIN iam.[User] u ON u.UserId=sh.CreatedBy"
                + filterSql(scope, status, query);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, 1, branchId, userId, scope, status, query);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<ShiftHandover> findPage(Connection conn, int branchId, int userId, String scope,
                                        String status, String query, int offset, int limit)
            throws SQLException {
        final String sql = "SELECT " + HANDOVER_COLUMNS
                + ", CASE WHEN EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient ro "
                + "WHERE ro.ShiftHandoverId=sh.ShiftHandoverId AND ro.RecipientUserId=? "
                + "AND ro.AcknowledgedAt IS NULL) THEN 0 ELSE 1 END AS NeedsMyAck "
                + HANDOVER_FROM + filterSql(scope, status, query)
                + " ORDER BY NeedsMyAck, sh.CreatedAt DESC, sh.ShiftHandoverId DESC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<ShiftHandover> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int index = bindFilter(ps, 2, branchId, userId, scope, status, query);
            ps.setInt(index++, offset);
            ps.setInt(index, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public List<ShiftHandover> findManagerFallbacks(Connection conn, int branchId,
                                                     int managerUserId, int limit)
            throws SQLException {
        final String sql = "SELECT TOP (?) " + HANDOVER_COLUMNS + HANDOVER_FROM
                + " WHERE sh.BranchId=? AND sh.OverallStatus <> 'COMPLETED'"
                + " AND EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient rm "
                + "WHERE rm.ShiftHandoverId=sh.ShiftHandoverId AND rm.RecipientUserId=?)"
                + " ORDER BY sh.CreatedAt DESC, sh.ShiftHandoverId DESC";
        List<ShiftHandover> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, branchId);
            ps.setInt(3, managerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(map(rs));
            }
        }
        return result;
    }

    public int countWaitingReceiptInBranch(Connection conn, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM hr.ShiftHandover "
                        + "WHERE BranchId=? AND OverallStatus='WAITING_RECEIPT'")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void updateOverallStatus(Connection conn, int handoverId, int acknowledged,
                                    int taskCount, int doneTaskCount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE hr.ShiftHandover SET OverallStatus=? WHERE ShiftHandoverId=?")) {
            ps.setString(1, overallStatus(acknowledged, taskCount, doneTaskCount));
            ps.setInt(2, handoverId);
            ps.executeUpdate();
        }
    }

    static String overallStatus(int acknowledged, int tasks, int done) {
        return acknowledged > 0 && tasks > 0 && tasks == done ? "COMPLETED"
                : (acknowledged > 0 ? "IN_PROGRESS" : "WAITING_RECEIPT");
    }

    private ShiftHandover map(ResultSet rs) throws SQLException {
        ShiftHandover handover = new ShiftHandover();
        handover.setShiftHandoverId(rs.getInt("ShiftHandoverId"));
        handover.setBranchId(rs.getInt("BranchId"));
        handover.setNote(rs.getString("Note"));
        handover.setCreatedBy(rs.getInt("CreatedBy"));
        int source = rs.getInt("SourceShiftAssignmentId");
        if (!rs.wasNull()) handover.setSourceShiftAssignmentId(source);
        handover.setOverallStatus(rs.getString("OverallStatus"));
        Timestamp created = rs.getTimestamp("CreatedAt");
        if (created != null) handover.setCreatedAt(created.toLocalDateTime());
        handover.setCreatedByName(rs.getString("CreatedByName"));
        String template = rs.getString("SourceTemplateName");
        java.sql.Time start = rs.getTime("SourceStartTime");
        java.sql.Time end = rs.getTime("SourceEndTime");
        if (template != null) handover.setSourceShiftLabel(template + " " + start + "–" + end);
        return handover;
    }
}
