package com.cafe.dao.shared;

import com.cafe.model.ShiftAssignment;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShiftAssignmentDao {

    /** Join template + user; lọc theo chi nhánh của template. */
    private static final String SELECT =
        "SELECT sa.ShiftAssignmentId, sa.ShiftTemplateId, sa.BranchId, sa.UserId, sa.WorkDate, " +
        "       st.Name AS TemplateName, st.StartTime, st.EndTime, u.FullName AS UserName, r.Code AS RoleCode " +
        "FROM hr.ShiftAssignment sa " +
        "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId = sa.ShiftTemplateId " +
        "JOIN iam.UserAccount u        ON u.UserId = sa.UserId " +
        "JOIN iam.Role r          ON r.RoleId = u.RoleId ";

    /** Lịch tuần: tất cả phân công của chi nhánh trong [weekStart, weekStart+7). */
    public List<ShiftAssignment> findByBranchAndWeek(Connection conn, int branchId, LocalDate weekStart) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE st.BranchId=? AND sa.WorkDate >= ? AND sa.WorkDate < ? " +
            "ORDER BY sa.WorkDate, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(weekStart));
            ps.setDate(3, Date.valueOf(weekStart.plusDays(7)));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các phân công trong khoảng ngày, dùng để tìm ca tiếp theo cho bàn giao. */
    public List<ShiftAssignment> findByBranchRange(Connection conn, int branchId, LocalDate from, LocalDate untilExclusive) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE st.BranchId=? AND sa.WorkDate >= ? AND sa.WorkDate < ? ORDER BY sa.WorkDate, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(untilExclusive));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các phân công của 1 nhân viên trong 1 ngày — dùng cho detectConflict (kèm giờ ca). */
    public List<ShiftAssignment> findByUserAndDate(Connection conn, int userId, LocalDate date) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT + "WHERE sa.UserId=? AND sa.WorkDate=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các phân công của 1 nhân viên trong tuần [weekStart, weekStart+7). */
    public List<ShiftAssignment> findByUserAndWeek(Connection conn, int userId, LocalDate weekStart) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE sa.UserId=? AND sa.WorkDate >= ? AND sa.WorkDate < ? " +
            "ORDER BY sa.WorkDate, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(weekStart));
            ps.setDate(3, Date.valueOf(weekStart.plusDays(7)));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public ShiftAssignment findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE sa.ShiftAssignmentId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int countByTemplate(Connection conn, int templateId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM hr.ShiftAssignment WHERE ShiftTemplateId=?")) {
            ps.setInt(1, templateId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public boolean hasAttendance(Connection conn, int assignmentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT CASE WHEN EXISTS (SELECT 1 FROM hr.Attendance WHERE ShiftAssignmentId=?) THEN 1 ELSE 0 END")) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }

    public boolean hasOpenAttendance(Connection conn, int assignmentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT CASE WHEN EXISTS (SELECT 1 FROM hr.Attendance " +
                "WHERE ShiftAssignmentId=? AND CheckInAt IS NOT NULL AND CheckOutAt IS NULL) THEN 1 ELSE 0 END")) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }

    /**
     * Ca đã check-in nhưng chưa check-out của nhân viên tại chi nhánh.
     * Query trả về ShiftAssignment nên thuộc DAO sở hữu hr.ShiftAssignment.
     */
    public ShiftAssignment findOpenByUserAndBranch(Connection conn, int userId, int branchId)
            throws SQLException {
        final String sql = SELECT
                + "JOIN hr.Attendance a ON a.ShiftAssignmentId=sa.ShiftAssignmentId "
                + "WHERE sa.UserId=? AND st.BranchId=? "
                + "AND a.CheckInAt IS NOT NULL AND a.CheckOutAt IS NULL "
                + "ORDER BY a.AttendanceId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, int templateId, int userId, LocalDate workDate, int branchId)
            throws SQLException {
        final String sql = "INSERT INTO hr.ShiftAssignment(ShiftTemplateId, BranchId, UserId, WorkDate) " +
                "OUTPUT INSERTED.ShiftAssignmentId " +
                "SELECT st.ShiftTemplateId, st.BranchId, u.UserId, ? " +
                "FROM hr.ShiftTemplate st CROSS JOIN iam.UserAccount u " +
                "WHERE st.ShiftTemplateId=? AND st.BranchId=? " +
                "AND u.UserId=? AND u.BranchId=? AND u.Status='ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(workDate));
            ps.setInt(2, templateId);
            ps.setInt(3, branchId);
            ps.setInt(4, userId);
            ps.setInt(5, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public int delete(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM hr.ShiftAssignment WHERE ShiftAssignmentId=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private ShiftAssignment map(ResultSet rs) throws SQLException {
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftAssignmentId(rs.getInt("ShiftAssignmentId"));
        a.setShiftTemplateId(rs.getInt("ShiftTemplateId"));
        a.setBranchId(rs.getInt("BranchId"));
        a.setUserId(rs.getInt("UserId"));
        Date d = rs.getDate("WorkDate");
        if (d != null) a.setWorkDate(d.toLocalDate());
        a.setTemplateName(rs.getString("TemplateName"));
        Time st = rs.getTime("StartTime");
        Time et = rs.getTime("EndTime");
        if (st != null) a.setStartTime(st.toLocalTime());
        if (et != null) a.setEndTime(et.toLocalTime());
        a.setUserName(rs.getString("UserName"));
        a.setRoleCode(rs.getString("RoleCode"));
        return a;
    }
}
