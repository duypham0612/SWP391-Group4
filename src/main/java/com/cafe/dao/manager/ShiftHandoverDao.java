package com.cafe.dao.manager;

import com.cafe.model.ShiftAssignment;
import com.cafe.model.ShiftHandover;
import com.cafe.model.ShiftHandoverRecipient;
import com.cafe.model.ShiftHandoverTask;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** DAO bàn giao có người nhận và đầu việc theo ca. */
public class ShiftHandoverDao {
    private static final String SELECT_HANDOVER =
        "SELECT sh.ShiftHandoverId, sh.BranchId, sh.Note, sh.CreatedBy, sh.SourceShiftAssignmentId, sh.OverallStatus, sh.CreatedAt, " +
        "u.FullName AS CreatedByName, st.Name AS SourceTemplateName, st.StartTime AS SourceStartTime, st.EndTime AS SourceEndTime " +
        "FROM hr.ShiftHandover sh JOIN iam.[User] u ON u.UserId=sh.CreatedBy " +
        "LEFT JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=sh.SourceShiftAssignmentId " +
        "LEFT JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId ";

    public ShiftAssignment findOpenSourceAssignment(Connection conn, int userId, int branchId) throws SQLException {
        final String sql = "SELECT TOP 1 sa.ShiftAssignmentId, sa.ShiftTemplateId, sa.UserId, sa.WorkDate, " +
            "st.Name AS TemplateName, st.StartTime, st.EndTime, u.FullName AS UserName, r.Code AS RoleCode " +
            "FROM hr.Attendance a JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=a.ShiftAssignmentId " +
            "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId JOIN iam.[User] u ON u.UserId=sa.UserId " +
            "JOIN iam.Role r ON r.RoleId=u.RoleId WHERE sa.UserId=? AND st.BranchId=? " +
            "AND a.CheckInAt IS NOT NULL AND a.CheckOutAt IS NULL ORDER BY a.AttendanceId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ShiftAssignment a = new ShiftAssignment();
                a.setShiftAssignmentId(rs.getInt("ShiftAssignmentId"));
                a.setShiftTemplateId(rs.getInt("ShiftTemplateId")); a.setUserId(rs.getInt("UserId"));
                Date workDate = rs.getDate("WorkDate"); if (workDate != null) a.setWorkDate(workDate.toLocalDate());
                a.setTemplateName(rs.getString("TemplateName"));
                java.sql.Time start = rs.getTime("StartTime"), end = rs.getTime("EndTime");
                if (start != null) a.setStartTime(start.toLocalTime()); if (end != null) a.setEndTime(end.toLocalTime());
                a.setUserName(rs.getString("UserName")); a.setRoleCode(rs.getString("RoleCode"));
                return a;
            }
        }
    }

    public int insert(Connection conn, int branchId, String note, int createdBy, int sourceAssignmentId) throws SQLException {
        final String sql = "INSERT INTO hr.ShiftHandover(BranchId, Note, CreatedBy, SourceShiftAssignmentId, OverallStatus) VALUES (?,?,?,?, 'WAITING_RECEIPT')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId); ps.setString(2, note); ps.setInt(3, createdBy); ps.setInt(4, sourceAssignmentId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : 0; }
        }
    }

    public void insertRecipient(Connection conn, int handoverId, int userId, Integer assignmentId, String type) throws SQLException {
        final String sql = "INSERT INTO hr.ShiftHandoverRecipient(ShiftHandoverId,RecipientUserId,RecipientShiftAssignmentId,RecipientType) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, handoverId); ps.setInt(2, userId);
            if (assignmentId == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, assignmentId);
            ps.setString(4, type); ps.executeUpdate();
        }
    }

    public void insertTask(Connection conn, int handoverId, String content) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO hr.ShiftHandoverTask(ShiftHandoverId,Content) VALUES (?,?)")) {
            ps.setInt(1, handoverId); ps.setString(2, content); ps.executeUpdate();
        }
    }

    public List<ShiftHandover> findByBranch(Connection conn, int branchId, int currentUserId) throws SQLException {
        List<ShiftHandover> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_HANDOVER + "WHERE sh.BranchId=? ORDER BY sh.CreatedAt DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(mapHandover(rs)); }
        }
        for (ShiftHandover handover : result) hydrate(conn, handover, currentUserId);
        return result;
    }

    public List<ShiftHandover> findManagerFallbacks(Connection conn, int branchId, int managerUserId) throws SQLException {
        List<ShiftHandover> all = findByBranch(conn, branchId, managerUserId);
        all.removeIf(h -> !h.isCurrentUserRecipient());
        return all;
    }

    public int countUnacknowledgedForUser(Connection conn, int branchId, int userId) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM hr.ShiftHandoverRecipient r JOIN hr.ShiftHandover h ON h.ShiftHandoverId=r.ShiftHandoverId " +
            "WHERE h.BranchId=? AND r.RecipientUserId=? AND r.AcknowledgedAt IS NULL AND h.OverallStatus <> 'LEGACY'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public boolean acknowledge(Connection conn, int handoverId, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE hr.ShiftHandoverRecipient SET AcknowledgedAt=SYSUTCDATETIME() WHERE ShiftHandoverId=? AND RecipientUserId=? AND AcknowledgedAt IS NULL")) {
            ps.setInt(1, handoverId); ps.setInt(2, userId); return ps.executeUpdate() == 1;
        }
    }

    public boolean isAcknowledgedRecipient(Connection conn, int handoverId, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM hr.ShiftHandoverRecipient WHERE ShiftHandoverId=? AND RecipientUserId=? AND AcknowledgedAt IS NOT NULL")) {
            ps.setInt(1, handoverId); ps.setInt(2, userId); try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean updateTaskStatus(Connection conn, int taskId, int handoverId, String status, int userId) throws SQLException {
        final String sql = "UPDATE hr.ShiftHandoverTask SET Status=?, UpdatedBy=?, UpdatedAt=SYSUTCDATETIME() WHERE ShiftHandoverTaskId=? AND ShiftHandoverId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status); ps.setInt(2, userId); ps.setInt(3, taskId); ps.setInt(4, handoverId); return ps.executeUpdate() == 1;
        }
    }

    public void refreshOverallStatus(Connection conn, int handoverId) throws SQLException {
        int recipients = count(conn, "SELECT COUNT(*) FROM hr.ShiftHandoverRecipient WHERE ShiftHandoverId=?", handoverId);
        int acknowledged = count(conn, "SELECT COUNT(*) FROM hr.ShiftHandoverRecipient WHERE ShiftHandoverId=? AND AcknowledgedAt IS NOT NULL", handoverId);
        int tasks = count(conn, "SELECT COUNT(*) FROM hr.ShiftHandoverTask WHERE ShiftHandoverId=?", handoverId);
        int done = count(conn, "SELECT COUNT(*) FROM hr.ShiftHandoverTask WHERE ShiftHandoverId=? AND Status='DONE'", handoverId);
        String status = recipients > 0 && recipients == acknowledged && tasks > 0 && tasks == done ? "COMPLETED"
            : (acknowledged > 0 ? "IN_PROGRESS" : "WAITING_RECEIPT");
        try (PreparedStatement ps = conn.prepareStatement("UPDATE hr.ShiftHandover SET OverallStatus=? WHERE ShiftHandoverId=?")) {
            ps.setString(1, status); ps.setInt(2, handoverId); ps.executeUpdate();
        }
    }

    private int count(Connection conn, String sql, int handoverId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, handoverId); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; } }
    }

    private void hydrate(Connection conn, ShiftHandover h, int currentUserId) throws SQLException {
        List<ShiftHandoverRecipient> recipients = new ArrayList<>();
        final String receiverSql = "SELECT r.ShiftHandoverRecipientId,r.RecipientUserId,r.RecipientShiftAssignmentId,r.RecipientType,r.AcknowledgedAt,u.FullName, " +
            "st.Name AS TemplateName,st.StartTime,st.EndTime FROM hr.ShiftHandoverRecipient r JOIN iam.[User] u ON u.UserId=r.RecipientUserId " +
            "LEFT JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=r.RecipientShiftAssignmentId LEFT JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId " +
            "WHERE r.ShiftHandoverId=? ORDER BY u.FullName";
        try (PreparedStatement ps = conn.prepareStatement(receiverSql)) {
            ps.setInt(1, h.getShiftHandoverId());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {
                ShiftHandoverRecipient r = new ShiftHandoverRecipient(); r.setShiftHandoverRecipientId(rs.getInt(1)); r.setRecipientUserId(rs.getInt(2));
                int assignment = rs.getInt(3); if (!rs.wasNull()) r.setRecipientShiftAssignmentId(assignment); r.setRecipientType(rs.getString(4));
                Timestamp acknowledged = rs.getTimestamp(5); if (acknowledged != null) r.setAcknowledgedAt(acknowledged.toLocalDateTime()); r.setRecipientName(rs.getString(6));
                String template = rs.getString(7); java.sql.Time start = rs.getTime(8), end = rs.getTime(9);
                r.setShiftLabel(template == null ? "Quản lý chi nhánh" : template + " " + start + "–" + end);
                recipients.add(r); if (r.getRecipientUserId() == currentUserId) { h.setCurrentUserRecipient(true); h.setCurrentUserAcknowledged(r.isAcknowledged()); }
            }}
        }
        h.setRecipients(recipients);
        List<ShiftHandoverTask> tasks = new ArrayList<>();
        final String taskSql = "SELECT t.ShiftHandoverTaskId,t.Content,t.Status,t.UpdatedBy,t.UpdatedAt,u.FullName AS UpdatedByName FROM hr.ShiftHandoverTask t LEFT JOIN iam.[User] u ON u.UserId=t.UpdatedBy WHERE t.ShiftHandoverId=? ORDER BY t.ShiftHandoverTaskId";
        try (PreparedStatement ps = conn.prepareStatement(taskSql)) {
            ps.setInt(1, h.getShiftHandoverId());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {
                ShiftHandoverTask t = new ShiftHandoverTask(); t.setShiftHandoverTaskId(rs.getInt(1)); t.setContent(rs.getString(2)); t.setStatus(rs.getString(3));
                int updater = rs.getInt(4); if (!rs.wasNull()) t.setUpdatedBy(updater); Timestamp updated = rs.getTimestamp(5); if (updated != null) t.setUpdatedAt(updated.toLocalDateTime()); t.setUpdatedByName(rs.getString(6)); tasks.add(t);
            }}
        }
        h.setTasks(tasks);
    }

    private ShiftHandover mapHandover(ResultSet rs) throws SQLException {
        ShiftHandover h = new ShiftHandover(); h.setShiftHandoverId(rs.getInt("ShiftHandoverId")); h.setBranchId(rs.getInt("BranchId")); h.setNote(rs.getString("Note")); h.setCreatedBy(rs.getInt("CreatedBy"));
        int source = rs.getInt("SourceShiftAssignmentId"); if (!rs.wasNull()) h.setSourceShiftAssignmentId(source); h.setOverallStatus(rs.getString("OverallStatus")); Timestamp created = rs.getTimestamp("CreatedAt"); if (created != null) h.setCreatedAt(created.toLocalDateTime()); h.setCreatedByName(rs.getString("CreatedByName"));
        String template = rs.getString("SourceTemplateName"); java.sql.Time start = rs.getTime("SourceStartTime"), end = rs.getTime("SourceEndTime"); if (template != null) h.setSourceShiftLabel(template + " " + start + "–" + end);
        return h;
    }
}
