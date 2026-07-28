package com.cafe.dao.shared;

import com.cafe.model.ShiftHandover;
import com.cafe.model.ShiftHandoverTask;

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

/** Canonical DAO for hr.ShiftHandoverTask. */
public class ShiftHandoverTaskDao {

    public void insert(Connection conn, int handoverId, String content) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO hr.ShiftHandoverTask(ShiftHandoverId,Content) VALUES (?,?)")) {
            ps.setInt(1, handoverId);
            ps.setString(2, content);
            ps.executeUpdate();
        }
    }

    public List<ShiftHandoverTask> findOpenForUser(Connection conn, int branchId, int userId, int limit)
            throws SQLException {
        final String sql = "SELECT TOP (?) t.ShiftHandoverTaskId, t.Content, t.Status, "
                + "u.FullName AS CreatedByName "
                + "FROM hr.ShiftHandoverTask t "
                + "JOIN hr.ShiftHandover h ON h.ShiftHandoverId=t.ShiftHandoverId "
                + "JOIN hr.ShiftHandoverRecipient r ON r.ShiftHandoverId=h.ShiftHandoverId "
                + "AND r.RecipientUserId=? AND r.AcknowledgedAt IS NOT NULL "
                + "JOIN iam.[User] u ON u.UserId=h.CreatedBy "
                + "WHERE h.BranchId=? AND t.Status <> 'DONE' "
                + "ORDER BY h.CreatedAt DESC, t.ShiftHandoverTaskId";
        List<ShiftHandoverTask> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, userId);
            ps.setInt(3, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShiftHandoverTask task = new ShiftHandoverTask();
                    task.setShiftHandoverTaskId(rs.getInt(1));
                    task.setContent(rs.getString(2));
                    task.setStatus(rs.getString(3));
                    task.setSourceLabel(rs.getString(4));
                    result.add(task);
                }
            }
        }
        return result;
    }

    public int countOpenForUser(Connection conn, int branchId, int userId) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM hr.ShiftHandoverTask t "
                + "JOIN hr.ShiftHandover h ON h.ShiftHandoverId=t.ShiftHandoverId "
                + "JOIN hr.ShiftHandoverRecipient r ON r.ShiftHandoverId=h.ShiftHandoverId "
                + "AND r.RecipientUserId=? AND r.AcknowledgedAt IS NOT NULL "
                + "WHERE h.BranchId=? AND t.Status <> 'DONE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean updateStatus(Connection conn, int taskId, int handoverId, String status, int userId)
            throws SQLException {
        final String sql = "UPDATE hr.ShiftHandoverTask SET Status=?, UpdatedBy=?, "
                + "UpdatedAt=SYSUTCDATETIME() WHERE ShiftHandoverTaskId=? AND ShiftHandoverId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.setInt(3, taskId);
            ps.setInt(4, handoverId);
            return ps.executeUpdate() == 1;
        }
    }

    public int countByHandover(Connection conn, int handoverId) throws SQLException {
        return count(conn, "SELECT COUNT(*) FROM hr.ShiftHandoverTask WHERE ShiftHandoverId=?",
                handoverId);
    }

    public int countDoneByHandover(Connection conn, int handoverId) throws SQLException {
        return count(conn, "SELECT COUNT(*) FROM hr.ShiftHandoverTask "
                + "WHERE ShiftHandoverId=? AND Status='DONE'", handoverId);
    }

    public void attachTo(Connection conn, List<ShiftHandover> handovers) throws SQLException {
        if (handovers.isEmpty()) return;
        Map<Integer, ShiftHandover> byId = index(handovers);
        final String sql = "SELECT t.ShiftHandoverId,t.ShiftHandoverTaskId,t.Content,t.Status,"
                + "t.UpdatedBy,t.UpdatedAt,u.FullName AS UpdatedByName "
                + "FROM hr.ShiftHandoverTask t "
                + "LEFT JOIN iam.[User] u ON u.UserId=t.UpdatedBy "
                + "WHERE t.ShiftHandoverId IN (" + placeholders(byId.size()) + ") "
                + "ORDER BY t.ShiftHandoverTaskId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, byId.keySet());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShiftHandover handover = byId.get(rs.getInt(1));
                    if (handover == null) continue;
                    ShiftHandoverTask task = new ShiftHandoverTask();
                    task.setShiftHandoverTaskId(rs.getInt(2));
                    task.setContent(rs.getString(3));
                    task.setStatus(rs.getString(4));
                    int updater = rs.getInt(5);
                    if (!rs.wasNull()) task.setUpdatedBy(updater);
                    Timestamp updated = rs.getTimestamp(6);
                    if (updated != null) task.setUpdatedAt(updated.toLocalDateTime());
                    task.setUpdatedByName(rs.getString(7));
                    handover.getTasks().add(task);
                }
            }
        }
    }

    private static int count(Connection conn, String sql, int handoverId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, handoverId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static Map<Integer, ShiftHandover> index(List<ShiftHandover> handovers) {
        Map<Integer, ShiftHandover> byId = new LinkedHashMap<>();
        for (ShiftHandover handover : handovers) {
            handover.setTasks(new ArrayList<>());
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
