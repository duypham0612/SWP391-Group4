package com.cafe.dao.manager;

import com.cafe.model.Attendance;
import com.cafe.model.PayrollRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDao {

    private static final String SELECT =
        "SELECT a.AttendanceId, a.ShiftAssignmentId, a.CheckInAt, a.CheckOutAt, a.Status, a.ApprovedBy, " +
        "       sa.WorkDate, sa.UserId, st.BranchId, st.Name AS TemplateName, st.StartTime, st.EndTime, " +
        "       u.FullName AS UserName, ap.FullName AS ApproverName " +
        "FROM hr.Attendance a " +
        "JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId = a.ShiftAssignmentId " +
        "JOIN hr.ShiftTemplate   st ON st.ShiftTemplateId  = sa.ShiftTemplateId " +
        "JOIN iam.[User] u          ON u.UserId = sa.UserId " +
        "LEFT JOIN iam.[User] ap    ON ap.UserId = a.ApprovedBy ";

    /** Chấm công của chi nhánh theo trạng thái (PENDING/APPROVED/REJECTED). */
    public List<Attendance> findByStatus(Connection conn, int branchId, String status) throws SQLException {
        List<Attendance> out = new ArrayList<>();
        final String sql = SELECT + "WHERE st.BranchId=? AND a.Status=? ORDER BY sa.WorkDate DESC, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public Attendance findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE a.AttendanceId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int countByStatus(Connection conn, int branchId, String status) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM hr.Attendance a " +
            "JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=a.ShiftAssignmentId " +
            "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId " +
            "WHERE st.BranchId=? AND a.Status=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public void updateStatus(Connection conn, int id, String status, int approverId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE hr.Attendance SET Status=?, ApprovedBy=? WHERE AttendanceId=?")) {
            ps.setString(1, status);
            ps.setInt(2, approverId);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /** Sửa giờ check-in/out (Manager chỉnh tay). */
    public void update(Connection conn, int id, Timestamp checkIn, Timestamp checkOut) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE hr.Attendance SET CheckInAt=?, CheckOutAt=? WHERE AttendanceId=?")) {
            ps.setTimestamp(1, checkIn);
            ps.setTimestamp(2, checkOut);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public int insert(Connection conn, int shiftAssignmentId, Timestamp checkIn, Timestamp checkOut, String status) throws SQLException {
        final String sql = "INSERT INTO hr.Attendance(ShiftAssignmentId, CheckInAt, CheckOutAt, Status) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, shiftAssignmentId);
            ps.setTimestamp(2, checkIn);
            ps.setTimestamp(3, checkOut);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    /** Tổng hợp bảng lương tháng từ chấm công APPROVED, group theo nhân viên. */
    public List<PayrollRow> aggregateApprovedByMonth(Connection conn, int branchId, LocalDate monthStart) throws SQLException {
        List<PayrollRow> out = new ArrayList<>();
        final String sql =
            "SELECT u.UserId, u.FullName, r.Name AS RoleName, " +
            "       COUNT(*) AS Shifts, " +
            "       SUM(CASE WHEN a.CheckInAt IS NOT NULL AND a.CheckOutAt IS NOT NULL " +
            "            THEN DATEDIFF(MINUTE, a.CheckInAt, a.CheckOutAt) ELSE 0 END) AS Minutes " +
            "FROM hr.Attendance a " +
            "JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=a.ShiftAssignmentId " +
            "JOIN hr.ShiftTemplate   st ON st.ShiftTemplateId=sa.ShiftTemplateId " +
            "JOIN iam.[User] u          ON u.UserId=sa.UserId " +
            "JOIN iam.Role   r          ON r.RoleId=u.RoleId " +
            "WHERE st.BranchId=? AND a.Status='APPROVED' " +
            "  AND sa.WorkDate >= ? AND sa.WorkDate < ? " +
            "GROUP BY u.UserId, u.FullName, r.Name " +
            "ORDER BY u.FullName";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(monthStart));
            ps.setDate(3, Date.valueOf(monthStart.plusMonths(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PayrollRow row = new PayrollRow();
                    row.setUserId(rs.getInt("UserId"));
                    row.setUserName(rs.getString("FullName"));
                    row.setRoleName(rs.getString("RoleName"));
                    row.setApprovedShifts(rs.getInt("Shifts"));
                    row.setTotalHours(Math.round(rs.getInt("Minutes") / 60.0 * 10) / 10.0);
                    out.add(row);
                }
            }
        }
        return out;
    }

    private Attendance map(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.setAttendanceId(rs.getInt("AttendanceId"));
        a.setShiftAssignmentId(rs.getInt("ShiftAssignmentId"));
        Timestamp ci = rs.getTimestamp("CheckInAt");
        Timestamp co = rs.getTimestamp("CheckOutAt");
        if (ci != null) a.setCheckInAt(ci.toLocalDateTime());
        if (co != null) a.setCheckOutAt(co.toLocalDateTime());
        a.setStatus(rs.getString("Status"));
        int ab = rs.getInt("ApprovedBy");
        if (!rs.wasNull()) a.setApprovedBy(ab);
        Date d = rs.getDate("WorkDate");
        if (d != null) a.setWorkDate(d.toLocalDate());
        a.setUserId(rs.getInt("UserId"));
        a.setTemplateName(rs.getString("TemplateName"));
        Time st = rs.getTime("StartTime");
        Time et = rs.getTime("EndTime");
        if (st != null) a.setStartTime(st.toLocalTime());
        if (et != null) a.setEndTime(et.toLocalTime());
        a.setUserName(rs.getString("UserName"));
        a.setApproverName(rs.getString("ApproverName"));
        return a;
    }
}
