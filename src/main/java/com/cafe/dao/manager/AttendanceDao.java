package com.cafe.dao.manager;

import com.cafe.model.Attendance;
import com.cafe.model.MonthlyAttendanceRow;
import com.cafe.model.PayrollRow;
import com.cafe.model.ShiftAssignment;

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
        "       u.FullName AS UserName, u.Phone AS UserPhone, r.Name AS RoleName, b.Name AS BranchName, " +
        "       ap.FullName AS ApproverName " +
        "FROM hr.Attendance a " +
        "JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId = a.ShiftAssignmentId " +
        "JOIN hr.ShiftTemplate   st ON st.ShiftTemplateId  = sa.ShiftTemplateId " +
        "JOIN iam.[User] u          ON u.UserId = sa.UserId " +
        "JOIN iam.Role   r          ON r.RoleId = u.RoleId " +
        "JOIN org.Branch b          ON b.BranchId = st.BranchId " +
        "LEFT JOIN iam.[User] ap    ON ap.UserId = a.ApprovedBy ";

    /** Các ca hôm nay của chính nhân viên, đã scope theo chi nhánh. */
    public List<ShiftAssignment> findTodayAssignments(Connection conn, int userId, int branchId, LocalDate workDate) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql =
            "SELECT sa.ShiftAssignmentId, sa.ShiftTemplateId, sa.UserId, sa.WorkDate, " +
            "       st.Name AS TemplateName, st.StartTime, st.EndTime, u.FullName AS UserName " +
            "FROM hr.ShiftAssignment sa " +
            "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId = sa.ShiftTemplateId " +
            "JOIN iam.[User] u        ON u.UserId = sa.UserId " +
            "WHERE sa.UserId=? AND st.BranchId=? AND sa.WorkDate=? " +
            "ORDER BY st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, branchId);
            ps.setDate(3, Date.valueOf(workDate));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(mapAssignment(rs)); }
        }
        return out;
    }

    /** Bản chấm công mới nhất của một assignment, kể cả đã đóng. */
    public Attendance findByAssignment(Connection conn, int assignmentId) throws SQLException {
        final String sql = SELECT.replaceFirst("SELECT ", "SELECT TOP 1 ") +
            "WHERE a.ShiftAssignmentId=? ORDER BY a.AttendanceId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Giờ UTC từ SQL Server để đồng bộ với dữ liệu chấm công hiện có. */
    public Timestamp currentUtc(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT SYSUTCDATETIME()")) {
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getTimestamp(1) : new Timestamp(System.currentTimeMillis()); }
        }
    }

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

    /** TẤT CẢ chấm công của chi nhánh (mọi trạng thái) — 1 màn gộp, mới nhất trước. */
    public List<Attendance> findByBranch(Connection conn, int branchId) throws SQLException {
        List<Attendance> out = new ArrayList<>();
        final String sql = SELECT + "WHERE st.BranchId=? ORDER BY sa.WorkDate DESC, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Đổi trạng thái + người duyệt (null khi trả về PENDING). */
    public void updateApproval(Connection conn, int id, String status, Integer approverId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE hr.Attendance SET Status=?, ApprovedBy=? WHERE AttendanceId=?")) {
            ps.setString(1, status);
            if (approverId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, approverId);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
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

    /** Lịch đi làm 1 tháng của CHÍNH nhân viên — gồm cả ngày được xếp ca mà không đi. */
    public List<MonthlyAttendanceRow> findMonthlyByUser(Connection conn, int userId, int branchId,
                                                         LocalDate monthStart, LocalDate monthEndExclusive)
            throws SQLException {
        List<MonthlyAttendanceRow> out = new ArrayList<>();
        final String sql =
            "SELECT sa.WorkDate, st.Name AS TemplateName, st.StartTime, st.EndTime, " +
            "       a.CheckInAt, a.CheckOutAt, a.Status " +
            "FROM hr.ShiftAssignment sa " +
            "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId = sa.ShiftTemplateId " +
            "OUTER APPLY ( " +
            "    SELECT TOP 1 att.CheckInAt, att.CheckOutAt, att.Status " +
            "    FROM hr.Attendance att " +
            "    WHERE att.ShiftAssignmentId = sa.ShiftAssignmentId " +
            "    ORDER BY att.AttendanceId DESC " +
            ") a " +
            "WHERE sa.UserId=? AND st.BranchId=? AND sa.WorkDate>=? AND sa.WorkDate<? " +
            "ORDER BY sa.WorkDate DESC, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, branchId);
            ps.setDate(3, Date.valueOf(monthStart));
            ps.setDate(4, Date.valueOf(monthEndExclusive));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapMonthly(rs));
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
        a.setUserPhone(rs.getString("UserPhone"));
        a.setRoleName(rs.getString("RoleName"));
        a.setBranchName(rs.getString("BranchName"));
        a.setApproverName(rs.getString("ApproverName"));
        return a;
    }

    private ShiftAssignment mapAssignment(ResultSet rs) throws SQLException {
        ShiftAssignment a = new ShiftAssignment();
        a.setShiftAssignmentId(rs.getInt("ShiftAssignmentId"));
        a.setShiftTemplateId(rs.getInt("ShiftTemplateId"));
        a.setUserId(rs.getInt("UserId"));
        Date d = rs.getDate("WorkDate");
        if (d != null) a.setWorkDate(d.toLocalDate());
        a.setTemplateName(rs.getString("TemplateName"));
        Time st = rs.getTime("StartTime");
        Time et = rs.getTime("EndTime");
        if (st != null) a.setStartTime(st.toLocalTime());
        if (et != null) a.setEndTime(et.toLocalTime());
        a.setUserName(rs.getString("UserName"));
        return a;
    }

    private MonthlyAttendanceRow mapMonthly(ResultSet rs) throws SQLException {
        MonthlyAttendanceRow r = new MonthlyAttendanceRow();
        Date d = rs.getDate("WorkDate");
        if (d != null) r.setWorkDate(d.toLocalDate());
        r.setTemplateName(rs.getString("TemplateName"));
        Time st = rs.getTime("StartTime");
        if (st != null) r.setShiftStart(st.toLocalTime());
        Time et = rs.getTime("EndTime");
        if (et != null) r.setShiftEnd(et.toLocalTime());
        Timestamp ci = rs.getTimestamp("CheckInAt");
        if (ci != null) r.setCheckInAt(ci.toLocalDateTime());
        Timestamp co = rs.getTimestamp("CheckOutAt");
        if (co != null) r.setCheckOutAt(co.toLocalDateTime());
        r.setStatus(rs.getString("Status"));
        return r;
    }
}
