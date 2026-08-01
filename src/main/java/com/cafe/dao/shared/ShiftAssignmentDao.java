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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ShiftAssignmentDao {

    private static final String SELECT =
        "SELECT sa.ShiftAssignmentId, sa.ShiftName, sa.StartTime, sa.EndTime, " +
        "       sa.BranchId, sa.UserId, sa.WorkDate, sa.HourlyRateSnapshot, " +
        "       u.FullName AS UserName, u.RoleCode " +
        "FROM hr.ShiftAssignment sa " +
        "JOIN iam.UserAccount u ON u.UserId = sa.UserId ";

    public List<ShiftAssignment> findByBranchAndWeek(Connection conn, int branchId, LocalDate weekStart)
            throws SQLException {
        return findByBranchRange(conn, branchId, weekStart, weekStart.plusDays(7));
    }

    public List<ShiftAssignment> findByBranchRange(Connection conn, int branchId,
                                                    LocalDate from, LocalDate untilExclusive)
            throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
                "WHERE sa.BranchId=? AND sa.WorkDate>=? AND sa.WorkDate<? " +
                "ORDER BY sa.WorkDate, sa.StartTime, sa.ShiftAssignmentId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(untilExclusive));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<ShiftAssignment> findByUserAndDate(Connection conn, int userId, LocalDate date)
            throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE sa.UserId=? AND sa.WorkDate=? ORDER BY sa.StartTime")) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<ShiftAssignment> findByUserAndWeek(Connection conn, int userId, LocalDate weekStart)
            throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
                "WHERE sa.UserId=? AND sa.WorkDate>=? AND sa.WorkDate<? " +
                "ORDER BY sa.WorkDate, sa.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(weekStart));
            ps.setDate(3, Date.valueOf(weekStart.plusDays(7)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public ShiftAssignment findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE sa.ShiftAssignmentId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean hasAttendance(Connection conn, int assignmentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT CASE WHEN EXISTS (SELECT 1 FROM hr.ShiftAssignment " +
                "WHERE ShiftAssignmentId=? AND AttendanceStatus IS NOT NULL) THEN 1 ELSE 0 END")) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    public boolean hasOpenAttendance(Connection conn, int assignmentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT CASE WHEN EXISTS (SELECT 1 FROM hr.ShiftAssignment " +
                "WHERE ShiftAssignmentId=? AND AttendanceStatus IS NOT NULL " +
                "AND CheckInAt IS NOT NULL AND CheckOutAt IS NULL) THEN 1 ELSE 0 END")) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    public ShiftAssignment findOpenByUserAndBranch(Connection conn, int userId, int branchId)
            throws SQLException {
        final String sql = SELECT +
                "WHERE sa.UserId=? AND sa.BranchId=? AND sa.AttendanceStatus IS NOT NULL " +
                "AND sa.CheckInAt IS NOT NULL AND sa.CheckOutAt IS NULL " +
                "ORDER BY sa.CheckInAt DESC, sa.ShiftAssignmentId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, String shiftName, LocalTime startTime, LocalTime endTime,
                      int userId, LocalDate workDate, int branchId) throws SQLException {
        final String sql =
                "INSERT INTO hr.ShiftAssignment(ShiftName, StartTime, EndTime, UserId, WorkDate, BranchId) " +
                "SELECT ?, ?, ?, u.UserId, ?, ? FROM iam.UserAccount u " +
                "WHERE u.UserId=? AND u.BranchId=? AND u.Status='ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setNString(1, shiftName);
            ps.setTime(2, Time.valueOf(startTime));
            ps.setTime(3, Time.valueOf(endTime));
            ps.setDate(4, Date.valueOf(workDate));
            ps.setInt(5, branchId);
            ps.setInt(6, userId);
            ps.setInt(7, branchId);
            if (ps.executeUpdate() != 1) return 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public int update(Connection conn, int assignmentId, String shiftName,
                      LocalTime startTime, LocalTime endTime, int userId,
                      LocalDate workDate, int branchId) throws SQLException {
        final String sql =
                "UPDATE sa SET ShiftName=?, StartTime=?, EndTime=?, UserId=?, WorkDate=? " +
                "FROM hr.ShiftAssignment sa JOIN iam.UserAccount u ON u.UserId=? " +
                "WHERE sa.ShiftAssignmentId=? AND sa.BranchId=? AND sa.AttendanceStatus IS NULL " +
                "AND u.BranchId=? AND u.Status='ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, shiftName);
            ps.setTime(2, Time.valueOf(startTime));
            ps.setTime(3, Time.valueOf(endTime));
            ps.setInt(4, userId);
            ps.setDate(5, Date.valueOf(workDate));
            ps.setInt(6, userId);
            ps.setInt(7, assignmentId);
            ps.setInt(8, branchId);
            ps.setInt(9, branchId);
            return ps.executeUpdate();
        }
    }

    public int delete(Connection conn, int id, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM hr.ShiftAssignment WHERE ShiftAssignmentId=? AND BranchId=? " +
                "AND AttendanceStatus IS NULL")) {
            ps.setInt(1, id);
            ps.setInt(2, branchId);
            return ps.executeUpdate();
        }
    }

    private ShiftAssignment map(ResultSet rs) throws SQLException {
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftAssignmentId(rs.getInt("ShiftAssignmentId"));
        a.setBranchId(rs.getInt("BranchId"));
        a.setUserId(rs.getInt("UserId"));
        Date workDate = rs.getDate("WorkDate");
        if (workDate != null) a.setWorkDate(workDate.toLocalDate());
        a.setShiftName(rs.getString("ShiftName"));
        Time start = rs.getTime("StartTime");
        Time end = rs.getTime("EndTime");
        if (start != null) a.setStartTime(start.toLocalTime());
        if (end != null) a.setEndTime(end.toLocalTime());
        a.setHourlyRateSnapshot(rs.getBigDecimal("HourlyRateSnapshot"));
        a.setUserName(rs.getString("UserName"));
        a.setRoleCode(rs.getString("RoleCode"));
        return a;
    }
}
