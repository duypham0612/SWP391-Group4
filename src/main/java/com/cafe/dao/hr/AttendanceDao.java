package com.cafe.dao.hr;

import com.cafe.model.MonthlyAttendanceRow;
import com.cafe.model.PayrollRow;
import com.cafe.model.ShiftAssignment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDao {

    private static final String SELECT =
        "SELECT sa.ShiftAssignmentId, sa.ShiftName, sa.StartTime, sa.EndTime, " +
        "       sa.BranchId, sa.UserId, sa.WorkDate, sa.HourlyRateSnapshot, " +
        "       sa.CheckInAt, sa.CheckOutAt, sa.AttendanceStatus, " +
        "       sa.ApprovedBy, sa.ApprovedAt, " +
        "       u.FullName AS UserName, u.Phone AS UserPhone, u.RoleCode, " +
        "       b.Name AS BranchName, ap.FullName AS ApproverName " +
        "FROM hr.ShiftAssignment sa " +
        "JOIN iam.UserAccount u ON u.UserId=sa.UserId " +
        "JOIN org.Branch b ON b.BranchId=sa.BranchId " +
        "LEFT JOIN iam.UserAccount ap ON ap.UserId=sa.ApprovedBy ";

    public List<ShiftAssignment> findClockAssignments(Connection conn, int userId, int branchId,
                                                       LocalDate businessDate) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
                "WHERE sa.UserId=? AND sa.BranchId=? AND sa.WorkDate BETWEEN ? AND ? " +
                "ORDER BY sa.WorkDate, sa.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, branchId);
            ps.setDate(3, Date.valueOf(businessDate.minusDays(1)));
            ps.setDate(4, Date.valueOf(businessDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public ShiftAssignment findByAssignment(Connection conn, int assignmentId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE sa.ShiftAssignmentId=?")) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public ShiftAssignment findByAssignmentForUpdate(Connection conn, int assignmentId)
            throws SQLException {
        final String lockedSelect = SELECT.replace(
                "FROM hr.ShiftAssignment sa ",
                "FROM hr.ShiftAssignment sa WITH (UPDLOCK, HOLDLOCK) ");
        try (PreparedStatement ps = conn.prepareStatement(
                lockedSelect + "WHERE sa.ShiftAssignmentId=?")) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Timestamp currentUtc(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT SYSUTCDATETIME()");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getTimestamp(1)
                    : new Timestamp(System.currentTimeMillis());
        }
    }

    public List<ShiftAssignment> findByStatus(Connection conn, int branchId, String status)
            throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
                "WHERE sa.BranchId=? AND sa.AttendanceStatus=? " +
                "ORDER BY sa.WorkDate DESC, sa.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<ShiftAssignment> findOpenByBranch(Connection conn, int branchId,
                                                   LocalDate businessDate)
            throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
                "WHERE sa.BranchId=? AND sa.AttendanceStatus IS NOT NULL " +
                "AND sa.CheckInAt IS NOT NULL AND sa.CheckOutAt IS NULL " +
                "AND sa.WorkDate>=? AND sa.WorkDate<=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(businessDate.minusDays(1)));
            ps.setDate(3, Date.valueOf(businessDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<ShiftAssignment> findByBranch(Connection conn, int branchId)
            throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql = SELECT +
                "WHERE sa.BranchId=? AND sa.AttendanceStatus IS NOT NULL " +
                "ORDER BY sa.WorkDate DESC, sa.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /**
     * Snapshot được ghi trong cùng UPDATE chuyển trạng thái sang APPROVED.
     * Dòng đã APPROVED được lưu lại sẽ giữ nguyên snapshot; mở lại rồi duyệt mới sẽ chụp lại.
     */
    public void updateApproval(Connection conn, int assignmentId,
                               String status, Integer approverId) throws SQLException {
        final String sql =
                "UPDATE sa SET AttendanceStatus=?, ApprovedBy=?, " +
                "ApprovedAt=CASE WHEN ?='PENDING' THEN NULL ELSE SYSUTCDATETIME() END, " +
                "HourlyRateSnapshot=CASE WHEN ?='APPROVED' " +
                "AND sa.AttendanceStatus<>'APPROVED' THEN u.HourlyRate " +
                "ELSE sa.HourlyRateSnapshot END " +
                "FROM hr.ShiftAssignment sa " +
                "JOIN iam.UserAccount u ON u.UserId=sa.UserId " +
                "WHERE sa.ShiftAssignmentId=? AND sa.AttendanceStatus IS NOT NULL " +
                "AND (?<>'APPROVED' OR sa.AttendanceStatus='APPROVED' " +
                "OR u.HourlyRate IS NOT NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindApproval(ps, 1, status, approverId);
            ps.setInt(5, assignmentId);
            ps.setString(6, status);
            ps.executeUpdate();
        }
    }

    public int updateApprovalByBranch(Connection conn, int assignmentId, int branchId,
                                      String status, Integer approverId) throws SQLException {
        final String sql =
                "UPDATE sa SET AttendanceStatus=?, ApprovedBy=?, " +
                "ApprovedAt=CASE WHEN ?='PENDING' THEN NULL ELSE SYSUTCDATETIME() END, " +
                "HourlyRateSnapshot=CASE WHEN ?='APPROVED' " +
                "AND sa.AttendanceStatus<>'APPROVED' THEN u.HourlyRate " +
                "ELSE sa.HourlyRateSnapshot END " +
                "FROM hr.ShiftAssignment sa " +
                "JOIN iam.UserAccount u ON u.UserId=sa.UserId " +
                "WHERE sa.ShiftAssignmentId=? AND sa.BranchId=? " +
                "AND sa.AttendanceStatus IS NOT NULL " +
                "AND (?<>'APPROVED' OR sa.AttendanceStatus='APPROVED' " +
                "OR u.HourlyRate IS NOT NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindApproval(ps, 1, status, approverId);
            ps.setInt(5, assignmentId);
            ps.setInt(6, branchId);
            ps.setString(7, status);
            return ps.executeUpdate();
        }
    }

    public boolean canApproveWithSnapshot(Connection conn, int assignmentId, int branchId)
            throws SQLException {
        final String sql =
                "SELECT CASE WHEN EXISTS (SELECT 1 FROM hr.ShiftAssignment sa " +
                "JOIN iam.UserAccount u ON u.UserId=sa.UserId " +
                "WHERE sa.ShiftAssignmentId=? AND sa.BranchId=? " +
                "AND (sa.AttendanceStatus='APPROVED' OR u.HourlyRate IS NOT NULL)) " +
                "THEN 1 ELSE 0 END";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }

    private void bindApproval(PreparedStatement ps, int index,
                              String status, Integer approverId) throws SQLException {
        ps.setString(index++, status);
        if (approverId == null) ps.setNull(index++, Types.INTEGER);
        else ps.setInt(index++, approverId);
        ps.setString(index++, status);
        ps.setString(index, status);
    }

    public ShiftAssignment findById(Connection conn, int assignmentId) throws SQLException {
        return findByAssignment(conn, assignmentId);
    }

    public int countByStatus(Connection conn, int branchId, String status)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM hr.ShiftAssignment " +
                "WHERE BranchId=? AND AttendanceStatus=?")) {
            ps.setInt(1, branchId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void update(Connection conn, int assignmentId,
                       Timestamp checkIn, Timestamp checkOut) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE hr.ShiftAssignment SET CheckInAt=?, CheckOutAt=? " +
                "WHERE ShiftAssignmentId=?")) {
            ps.setTimestamp(1, checkIn);
            ps.setTimestamp(2, checkOut);
            ps.setInt(3, assignmentId);
            ps.executeUpdate();
        }
    }

    public int updateByBranch(Connection conn, int assignmentId, int branchId,
                              Timestamp checkIn, Timestamp checkOut) throws SQLException {
        final String sql =
                "UPDATE hr.ShiftAssignment SET CheckInAt=?, CheckOutAt=? " +
                "WHERE ShiftAssignmentId=? AND BranchId=? " +
                "AND AttendanceStatus IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, checkIn);
            ps.setTimestamp(2, checkOut);
            ps.setInt(3, assignmentId);
            ps.setInt(4, branchId);
            return ps.executeUpdate();
        }
    }

    public int clockIn(Connection conn, int assignmentId, Timestamp checkIn)
            throws SQLException {
        final String sql =
                "UPDATE hr.ShiftAssignment SET CheckInAt=?, CheckOutAt=NULL, " +
                "AttendanceStatus='PENDING', ApprovedBy=NULL, ApprovedAt=NULL " +
                "WHERE ShiftAssignmentId=? AND CheckInAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, checkIn);
            ps.setInt(2, assignmentId);
            return ps.executeUpdate();
        }
    }

    /**
     * Lương runtime = tổng(giờ từng ca * snapshot), fallback sang đơn giá hiện tại
     * chỉ cho dữ liệu cũ chưa có snapshot.
     */
    public List<PayrollRow> aggregateApprovedByMonth(Connection conn, int branchId,
                                                      LocalDate monthStart)
            throws SQLException {
        List<PayrollRow> out = new ArrayList<>();
        final String sql =
                "SELECT u.UserId, u.FullName, " +
                roleNameCase("u.RoleCode") + " AS RoleName, COUNT(*) AS Shifts, " +
                "SUM(CASE WHEN sa.CheckInAt IS NOT NULL AND sa.CheckOutAt IS NOT NULL " +
                "THEN DATEDIFF(MINUTE,sa.CheckInAt,sa.CheckOutAt) ELSE 0 END) AS Minutes, " +
                "SUM(CASE WHEN sa.CheckInAt IS NOT NULL AND sa.CheckOutAt IS NOT NULL " +
                "THEN CAST(DATEDIFF(MINUTE,sa.CheckInAt,sa.CheckOutAt) AS DECIMAL(19,4)) " +
                "/ 60 * COALESCE(sa.HourlyRateSnapshot,u.HourlyRate,0) ELSE 0 END) AS Salary " +
                "FROM hr.ShiftAssignment sa " +
                "JOIN iam.UserAccount u ON u.UserId=sa.UserId " +
                "WHERE sa.BranchId=? AND sa.AttendanceStatus='APPROVED' " +
                "AND sa.WorkDate>=? AND sa.WorkDate<? " +
                "GROUP BY u.UserId,u.FullName,u.RoleCode ORDER BY u.FullName";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(monthStart));
            ps.setDate(3, Date.valueOf(monthStart.plusMonths(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long minutes = rs.getLong("Minutes");
                    BigDecimal hours = BigDecimal.valueOf(minutes)
                            .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
                    BigDecimal salary = rs.getBigDecimal("Salary");
                    if (salary == null) salary = BigDecimal.ZERO;

                    PayrollRow row = new PayrollRow();
                    row.setUserId(rs.getInt("UserId"));
                    row.setUserName(rs.getString("FullName"));
                    row.setRoleName(rs.getString("RoleName"));
                    row.setApprovedShifts(rs.getInt("Shifts"));
                    row.setTotalHours(hours.setScale(1, RoundingMode.HALF_UP).doubleValue());
                    row.setHourlyRate(minutes == 0 ? BigDecimal.ZERO
                            : salary.divide(hours, 2, RoundingMode.HALF_UP));
                    row.setSalary(salary);
                    out.add(row);
                }
            }
        }
        return out;
    }

    private static final String MONTHLY_SELECT =
        "SELECT sa.WorkDate, sa.ShiftName, sa.StartTime, sa.EndTime, " +
        "sa.CheckInAt, sa.CheckOutAt, sa.AttendanceStatus AS Status ";

    private static final String MONTHLY_FROM = "FROM hr.ShiftAssignment sa ";
    private static final String MONTHLY_ORDER =
        "ORDER BY sa.WorkDate DESC, sa.StartTime, sa.ShiftAssignmentId ";

    public List<MonthlyAttendanceRow> findMonthlyByUser(
            Connection conn, int userId, int branchId,
            LocalDate monthStart, LocalDate monthEndExclusive) throws SQLException {
        List<MonthlyAttendanceRow> out = new ArrayList<>();
        final String sql =
                MONTHLY_SELECT + MONTHLY_FROM + monthlyWhere(null, null) + MONTHLY_ORDER;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindMonthlyFilters(ps, 1, userId, branchId,
                    monthStart, monthEndExclusive, null);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapMonthly(rs));
            }
        }
        return out;
    }

    public List<MonthlyAttendanceRow> findMonthlyPageByUser(
            Connection conn, int userId, int branchId,
            LocalDate monthStart, LocalDate monthEndExclusive,
            String query, String state, int offset, int pageSize) throws SQLException {
        List<MonthlyAttendanceRow> out = new ArrayList<>();
        final String sql = MONTHLY_SELECT + MONTHLY_FROM +
                monthlyWhere(query, state) + MONTHLY_ORDER +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = bindMonthlyFilters(ps, 1, userId, branchId,
                    monthStart, monthEndExclusive, query);
            ps.setInt(index++, Math.max(0, offset));
            ps.setInt(index, Math.max(1, pageSize));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapMonthly(rs));
            }
        }
        return out;
    }

    public int countMonthlyByUser(
            Connection conn, int userId, int branchId,
            LocalDate monthStart, LocalDate monthEndExclusive,
            String query, String state) throws SQLException {
        final String sql =
                "SELECT COUNT(*) " + MONTHLY_FROM + monthlyWhere(query, state);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindMonthlyFilters(ps, 1, userId, branchId,
                    monthStart, monthEndExclusive, query);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static String monthlyWhere(String query, String state) {
        StringBuilder where = new StringBuilder(
                "WHERE sa.UserId=? AND sa.BranchId=? " +
                "AND sa.WorkDate>=? AND sa.WorkDate<? ");
        if (hasText(query)) {
            where.append("AND (sa.ShiftName LIKE ? ESCAPE '\\' ")
                 .append("OR CONVERT(varchar(10),sa.WorkDate,103) LIKE ? ESCAPE '\\') ");
        }
        where.append(monthlyStateCondition(state));
        return where.toString();
    }

    private static final String MONTHLY_CLOSED =
            "AND sa.CheckInAt IS NOT NULL AND sa.CheckOutAt IS NOT NULL ";

    private static String monthlyStateCondition(String state) {
        if (state == null) return "";
        return switch (state) {
            case "ABSENT" ->
                    "AND (sa.AttendanceStatus IS NULL OR sa.CheckInAt IS NULL) ";
            case "OPEN" ->
                    "AND sa.AttendanceStatus IS NOT NULL " +
                    "AND sa.CheckInAt IS NOT NULL AND sa.CheckOutAt IS NULL ";
            case "APPROVED" ->
                    MONTHLY_CLOSED + "AND sa.AttendanceStatus='APPROVED' ";
            case "REJECTED" ->
                    MONTHLY_CLOSED + "AND sa.AttendanceStatus='REJECTED' ";
            case "PENDING" ->
                    MONTHLY_CLOSED + "AND sa.AttendanceStatus='PENDING' ";
            default -> "";
        };
    }

    private static int bindMonthlyFilters(
            PreparedStatement ps, int index, int userId, int branchId,
            LocalDate monthStart, LocalDate monthEndExclusive, String query)
            throws SQLException {
        ps.setInt(index++, userId);
        ps.setInt(index++, branchId);
        ps.setDate(index++, Date.valueOf(monthStart));
        ps.setDate(index++, Date.valueOf(monthEndExclusive));
        if (hasText(query)) {
            String pattern = "%" + query.replace("\\", "\\\\")
                    .replace("%", "\\%").replace("_", "\\_") + "%";
            ps.setNString(index++, pattern);
            ps.setNString(index++, pattern);
        }
        return index;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ShiftAssignment map(ResultSet rs) throws SQLException {
        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setShiftAssignmentId(rs.getInt("ShiftAssignmentId"));
        assignment.setBranchId(rs.getInt("BranchId"));
        assignment.setUserId(rs.getInt("UserId"));
        Date workDate = rs.getDate("WorkDate");
        if (workDate != null) assignment.setWorkDate(workDate.toLocalDate());
        assignment.setShiftName(rs.getString("ShiftName"));
        Time start = rs.getTime("StartTime");
        Time end = rs.getTime("EndTime");
        if (start != null) assignment.setStartTime(start.toLocalTime());
        if (end != null) assignment.setEndTime(end.toLocalTime());
        assignment.setHourlyRateSnapshot(rs.getBigDecimal("HourlyRateSnapshot"));
        Timestamp checkIn = rs.getTimestamp("CheckInAt");
        Timestamp checkOut = rs.getTimestamp("CheckOutAt");
        if (checkIn != null) assignment.setCheckInAt(checkIn.toLocalDateTime());
        if (checkOut != null) assignment.setCheckOutAt(checkOut.toLocalDateTime());
        assignment.setAttendanceStatus(rs.getString("AttendanceStatus"));
        int approvedBy = rs.getInt("ApprovedBy");
        if (!rs.wasNull()) assignment.setApprovedBy(approvedBy);
        Timestamp approvedAt = rs.getTimestamp("ApprovedAt");
        if (approvedAt != null) assignment.setApprovedAt(approvedAt.toLocalDateTime());
        assignment.setUserName(rs.getString("UserName"));
        assignment.setUserPhone(rs.getString("UserPhone"));
        assignment.setRoleCode(rs.getString("RoleCode"));
        assignment.setBranchName(rs.getString("BranchName"));
        assignment.setApproverName(rs.getString("ApproverName"));
        return assignment;
    }

    private MonthlyAttendanceRow mapMonthly(ResultSet rs) throws SQLException {
        MonthlyAttendanceRow row = new MonthlyAttendanceRow();
        Date workDate = rs.getDate("WorkDate");
        if (workDate != null) row.setWorkDate(workDate.toLocalDate());
        row.setShiftName(rs.getString("ShiftName"));
        Time start = rs.getTime("StartTime");
        Time end = rs.getTime("EndTime");
        if (start != null) row.setShiftStart(start.toLocalTime());
        if (end != null) row.setShiftEnd(end.toLocalTime());
        Timestamp checkIn = rs.getTimestamp("CheckInAt");
        Timestamp checkOut = rs.getTimestamp("CheckOutAt");
        if (checkIn != null) row.setCheckInAt(checkIn.toLocalDateTime());
        if (checkOut != null) row.setCheckOutAt(checkOut.toLocalDateTime());
        row.setStatus(rs.getString("Status"));
        return row;
    }

    private static String roleNameCase(String column) {
        return "CASE " + column +
                " WHEN 'ADMIN' THEN N'Quản trị hệ thống'" +
                " WHEN 'BRANCH_MANAGER' THEN N'Quản lý chi nhánh'" +
                " WHEN 'CASHIER' THEN N'Thu ngân'" +
                " WHEN 'BARISTA' THEN N'Pha chế' ELSE " + column + " END";
    }
}
