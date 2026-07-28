package com.cafe.dao.shared;

import com.cafe.model.ShiftHandover;
import com.cafe.model.ShiftHandoverRecipient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical DAO for hr.ShiftHandoverRecipient. */
public class ShiftHandoverRecipientDao {

    public void insert(Connection conn, int handoverId, int userId, Integer assignmentId, String type)
            throws SQLException {
        final String sql = "INSERT INTO hr.ShiftHandoverRecipient"
                + "(ShiftHandoverId,RecipientUserId,RecipientShiftAssignmentId,RecipientType) "
                + "VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, handoverId);
            ps.setInt(2, userId);
            if (assignmentId == null) ps.setNull(3, java.sql.Types.INTEGER);
            else ps.setInt(3, assignmentId);
            ps.setString(4, type);
            ps.executeUpdate();
        }
    }

    public int countUnacknowledgedForUser(Connection conn, int branchId, int userId)
            throws SQLException {
        final String sql = "SELECT COUNT(*) FROM hr.ShiftHandoverRecipient r "
                + "JOIN hr.ShiftHandover h ON h.ShiftHandoverId=r.ShiftHandoverId "
                + "WHERE h.BranchId=? AND r.RecipientUserId=? AND r.AcknowledgedAt IS NULL "
                + "AND h.OverallStatus <> 'LEGACY'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countClaimableInBranch(Connection conn, int branchId, int userId, int staleAfterHours)
            throws SQLException {
        final String sql = "SELECT COUNT(*) FROM hr.ShiftHandover sh "
                + "WHERE sh.BranchId=? AND sh.CreatedBy<>? AND sh.OverallStatus='WAITING_RECEIPT' "
                + "AND NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient a "
                + "WHERE a.ShiftHandoverId=sh.ShiftHandoverId AND a.AcknowledgedAt IS NOT NULL) "
                + "AND NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient m "
                + "WHERE m.ShiftHandoverId=sh.ShiftHandoverId AND m.RecipientUserId=?) "
                + "AND (NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient r "
                + "WHERE r.ShiftHandoverId=sh.ShiftHandoverId) "
                + "OR sh.CreatedAt < DATEADD(HOUR, -?, SYSUTCDATETIME()))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ps.setInt(4, staleAfterHours);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Atomic claim: the INSERT...SELECT eligibility check prevents two recipients winning. */
    public boolean claimStale(Connection conn, int handoverId, int branchId, int userId,
                              Integer assignmentId, int staleAfterHours) throws SQLException {
        final String sql = "INSERT INTO hr.ShiftHandoverRecipient"
                + "(ShiftHandoverId,RecipientUserId,RecipientShiftAssignmentId,RecipientType,AcknowledgedAt) "
                + "SELECT sh.ShiftHandoverId, ?, ?, 'NEXT_SHIFT', SYSUTCDATETIME() "
                + "FROM hr.ShiftHandover sh WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE sh.ShiftHandoverId=? AND sh.BranchId=? AND sh.CreatedBy<>? "
                + "AND sh.OverallStatus='WAITING_RECEIPT' "
                + "AND NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient a "
                + "WHERE a.ShiftHandoverId=sh.ShiftHandoverId AND a.AcknowledgedAt IS NOT NULL) "
                + "AND NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient m "
                + "WHERE m.ShiftHandoverId=sh.ShiftHandoverId AND m.RecipientUserId=?) "
                + "AND (NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient r "
                + "WHERE r.ShiftHandoverId=sh.ShiftHandoverId) "
                + "OR sh.CreatedAt < DATEADD(HOUR, -?, SYSUTCDATETIME()))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (assignmentId == null) ps.setNull(2, java.sql.Types.INTEGER);
            else ps.setInt(2, assignmentId);
            ps.setInt(3, handoverId);
            ps.setInt(4, branchId);
            ps.setInt(5, userId);
            ps.setInt(6, userId);
            ps.setInt(7, staleAfterHours);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean acknowledge(Connection conn, int handoverId, int userId) throws SQLException {
        final String sql = "UPDATE hr.ShiftHandoverRecipient SET AcknowledgedAt=SYSUTCDATETIME() "
                + "WHERE ShiftHandoverId=? AND RecipientUserId=? AND AcknowledgedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, handoverId);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean isAcknowledged(Connection conn, int handoverId, int userId) throws SQLException {
        final String sql = "SELECT 1 FROM hr.ShiftHandoverRecipient "
                + "WHERE ShiftHandoverId=? AND RecipientUserId=? AND AcknowledgedAt IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, handoverId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countAcknowledged(Connection conn, int handoverId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM hr.ShiftHandoverRecipient "
                        + "WHERE ShiftHandoverId=? AND AcknowledgedAt IS NOT NULL")) {
            ps.setInt(1, handoverId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void attachTo(Connection conn, List<ShiftHandover> handovers, int currentUserId)
            throws SQLException {
        if (handovers.isEmpty()) return;
        Map<Integer, ShiftHandover> byId = index(handovers);
        String sql = "SELECT r.ShiftHandoverId,r.ShiftHandoverRecipientId,r.RecipientUserId,"
                + "r.RecipientShiftAssignmentId,r.RecipientType,r.AcknowledgedAt,u.FullName, "
                + "st.Name AS TemplateName,st.StartTime,st.EndTime "
                + "FROM hr.ShiftHandoverRecipient r "
                + "JOIN iam.[User] u ON u.UserId=r.RecipientUserId "
                + "LEFT JOIN hr.ShiftAssignment sa "
                + "ON sa.ShiftAssignmentId=r.RecipientShiftAssignmentId "
                + "LEFT JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId "
                + "WHERE r.ShiftHandoverId IN (" + placeholders(byId.size()) + ") "
                + "ORDER BY u.FullName";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, byId.keySet());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShiftHandover handover = byId.get(rs.getInt(1));
                    if (handover == null) continue;
                    ShiftHandoverRecipient recipient = new ShiftHandoverRecipient();
                    recipient.setShiftHandoverRecipientId(rs.getInt(2));
                    recipient.setRecipientUserId(rs.getInt(3));
                    int assignment = rs.getInt(4);
                    if (!rs.wasNull()) recipient.setRecipientShiftAssignmentId(assignment);
                    recipient.setRecipientType(rs.getString(5));
                    Timestamp acknowledged = rs.getTimestamp(6);
                    if (acknowledged != null) recipient.setAcknowledgedAt(acknowledged.toLocalDateTime());
                    recipient.setRecipientName(rs.getString(7));
                    String template = rs.getString(8);
                    java.sql.Time start = rs.getTime(9);
                    java.sql.Time end = rs.getTime(10);
                    recipient.setShiftLabel(template == null ? "Quản lý chi nhánh"
                            : template + " " + start + "–" + end);
                    handover.getRecipients().add(recipient);
                    if (recipient.getRecipientUserId() == currentUserId) {
                        handover.setCurrentUserRecipient(true);
                        handover.setCurrentUserAcknowledged(recipient.isAcknowledged());
                    }
                }
            }
        }
    }

    private static Map<Integer, ShiftHandover> index(List<ShiftHandover> handovers) {
        Map<Integer, ShiftHandover> byId = new LinkedHashMap<>();
        for (ShiftHandover handover : handovers) {
            handover.setRecipients(new ArrayList<>());
            byId.put(handover.getShiftHandoverId(), handover);
        }
        return byId;
    }

    private static String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private static void bindIds(PreparedStatement ps, Collection<Integer> ids) throws SQLException {
        int index = 1;
        for (Integer id : ids) ps.setInt(index++, id);
    }
}
