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
        "SELECT a.AttendanceId, a.ShiftAssignmentId, a.CheckInAt, a.CheckOutAt, a.Status, a.ApprovedBy, a.ApprovedAt, " +
        "       sa.WorkDate, sa.UserId, st.BranchId, st.Name AS TemplateName, st.StartTime, st.EndTime, " +
        "       u.FullName AS UserName, u.Phone AS UserPhone, r.Name AS RoleName, b.Name AS BranchName, " +
        "       ap.FullName AS ApproverName " +
        "FROM hr.Attendance a " +
        "JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId = a.ShiftAssignmentId " +
        "JOIN hr.ShiftTemplate   st ON st.ShiftTemplateId  = sa.ShiftTemplateId " +
        "JOIN iam.UserAccount u          ON u.UserId = sa.UserId " +
        "JOIN iam.Role   r          ON r.RoleId = u.RoleId " +
        "JOIN org.Branch b          ON b.BranchId = st.BranchId " +
        "LEFT JOIN iam.UserAccount ap    ON ap.UserId = a.ApprovedBy ";

    /**
     * Ca dùng để chấm công: thêm ca hôm trước vì ca đêm chỉ kết thúc vào sáng hôm sau.
     * Service lọc tiếp bằng {@link com.cafe.common.ShiftWindow} nên ca cũ đã hết giờ không lọt vào.
     */
    public List<ShiftAssignment> findClockAssignments(Connection conn, int userId, int branchId, LocalDate businessDate) throws SQLException {
        return findAssignmentsBetween(conn, userId, branchId, businessDate.minusDays(1), businessDate);
    }

    private List<ShiftAssignment> findAssignmentsBetween(Connection conn, int userId, int branchId, LocalDate from, LocalDate to) throws SQLException {
        List<ShiftAssignment> out = new ArrayList<>();
        final String sql =
            "SELECT sa.ShiftAssignmentId, sa.ShiftTemplateId, sa.UserId, sa.WorkDate, " +
            "       st.Name AS TemplateName, st.StartTime, st.EndTime, u.FullName AS UserName " +
            "FROM hr.ShiftAssignment sa " +
            "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId = sa.ShiftTemplateId " +
            "JOIN iam.UserAccount u        ON u.UserId = sa.UserId " +
            "WHERE sa.UserId=? AND st.BranchId=? AND sa.WorkDate BETWEEN ? AND ? " +
            "ORDER BY sa.WorkDate, st.StartTime";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, branchId);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(mapAssignment(rs)); }
        }
        return out;
    }

    /** Bản chấm công của một assignment. UQ_Attendance_ShiftAssignment bảo đảm tối đa một dòng. */
    public Attendance findByAssignment(Connection conn, int assignmentId) throws SQLException {
        final String sql = SELECT + "WHERE a.ShiftAssignmentId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /**
     * Khoá dòng (hoặc key-range khi chưa có dòng) đến hết transaction để hai tab không cùng tạo Attendance.
     * Cần unique index UQ_Attendance_ShiftAssignment để SQL Server khóa đúng key-range.
     */
    public Attendance findByAssignmentForUpdate(Connection conn, int assignmentId) throws SQLException {
        final String sql = SELECT.replace("FROM hr.Attendance a ", "FROM hr.Attendance a WITH (UPDLOCK, HOLDLOCK) ") +
            "WHERE a.ShiftAssignmentId=?";
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

    /**
     * Chấm công CHƯA tan ca của chi nhánh quanh ngày kinh doanh — nguồn để biết ai còn đang trực.
     * Lấy cả ca hôm trước vì ca đêm chỉ kết thúc vào sáng hôm sau; service lọc tiếp bằng
     * {@link com.cafe.common.ShiftWindow#onDuty} nên ca đã quá giờ tan không được tính là còn trực.
     */
    public List<Attendance> findOpenByBranch(Connection conn, int branchId, LocalDate businessDate)
            throws SQLException {
        List<Attendance> out = new ArrayList<>();
        final String sql = SELECT + "WHERE st.BranchId=? AND a.CheckOutAt IS NULL "
                + "AND sa.WorkDate >= ? AND sa.WorkDate <= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(businessDate.minusDays(1)));
            ps.setDate(3, Date.valueOf(businessDate));
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
                "UPDATE hr.Attendance SET Status=?, ApprovedBy=?, " +
                "ApprovedAt=CASE WHEN ?='PENDING' THEN NULL ELSE SYSUTCDATETIME() END WHERE AttendanceId=?")) {
            ps.setString(1, status);
            if (approverId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, approverId);
            ps.setString(3, status);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    /** Manager mutation scoped by the branch of the assignment template. */
    public int updateApprovalByBranch(Connection conn, int id, int branchId,
                                      String status, Integer approverId) throws SQLException {
        final String sql = "UPDATE a SET Status=?, ApprovedBy=?, " +
                "ApprovedAt=CASE WHEN ?='PENDING' THEN NULL ELSE SYSUTCDATETIME() END " +
                "FROM hr.Attendance a " +
                "JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=a.ShiftAssignmentId " +
                "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId " +
                "WHERE a.AttendanceId=? AND st.BranchId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (approverId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, approverId);
            ps.setString(3, status);
            ps.setInt(4, id);
            ps.setInt(5, branchId);
            return ps.executeUpdate();
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

    /** Sửa giờ bằng ID nhưng vẫn bắt buộc attendance thuộc chi nhánh của Manager. */
    public int updateByBranch(Connection conn, int id, int branchId,
                              Timestamp checkIn, Timestamp checkOut) throws SQLException {
        final String sql = "UPDATE a SET CheckInAt=?, CheckOutAt=? " +
                "FROM hr.Attendance a " +
                "JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=a.ShiftAssignmentId " +
                "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId " +
                "WHERE a.AttendanceId=? AND st.BranchId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, checkIn);
            ps.setTimestamp(2, checkOut);
            ps.setInt(3, id);
            ps.setInt(4, branchId);
            return ps.executeUpdate();
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
            "JOIN iam.UserAccount u          ON u.UserId=sa.UserId " +
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

    private static final String MONTHLY_SELECT =
        "SELECT sa.WorkDate, st.Name AS TemplateName, st.StartTime, st.EndTime, " +
        "       a.CheckInAt, a.CheckOutAt, a.Status ";

    private static final String MONTHLY_FROM =
        "FROM hr.ShiftAssignment sa " +
        "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId = sa.ShiftTemplateId " +
        "OUTER APPLY ( " +
        "    SELECT TOP 1 att.CheckInAt, att.CheckOutAt, att.Status " +
        "    FROM hr.Attendance att " +
        "    WHERE att.ShiftAssignmentId = sa.ShiftAssignmentId " +
        "    ORDER BY att.AttendanceId DESC " +
        ") a ";

    /** Ca mới nhất trước; ShiftAssignmentId chốt thứ tự để OFFSET/FETCH không trả trùng hoặc sót dòng. */
    private static final String MONTHLY_ORDER = "ORDER BY sa.WorkDate DESC, st.StartTime, sa.ShiftAssignmentId ";

    /** Lịch đi làm 1 tháng của CHÍNH nhân viên — gồm cả ngày được xếp ca mà không đi. */
    public List<MonthlyAttendanceRow> findMonthlyByUser(Connection conn, int userId, int branchId,
                                                         LocalDate monthStart, LocalDate monthEndExclusive)
            throws SQLException {
        List<MonthlyAttendanceRow> out = new ArrayList<>();
        final String sql = MONTHLY_SELECT + MONTHLY_FROM + monthlyWhere(null, null) + MONTHLY_ORDER;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindMonthlyFilters(ps, 1, userId, branchId, monthStart, monthEndExclusive, null);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapMonthly(rs));
            }
        }
        return out;
    }

    /** Một trang lịch đi làm đã tìm/lọc tại DB — màn ca làm không tải cả tháng về trình duyệt. */
    public List<MonthlyAttendanceRow> findMonthlyPageByUser(Connection conn, int userId, int branchId,
                                                             LocalDate monthStart, LocalDate monthEndExclusive,
                                                             String query, String state,
                                                             int offset, int pageSize) throws SQLException {
        List<MonthlyAttendanceRow> out = new ArrayList<>();
        final String sql = MONTHLY_SELECT + MONTHLY_FROM + monthlyWhere(query, state) + MONTHLY_ORDER
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindMonthlyFilters(ps, 1, userId, branchId, monthStart, monthEndExclusive, query);
            ps.setInt(idx++, Math.max(0, offset));
            ps.setInt(idx, Math.max(1, pageSize));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapMonthly(rs));
            }
        }
        return out;
    }

    public int countMonthlyByUser(Connection conn, int userId, int branchId,
                                  LocalDate monthStart, LocalDate monthEndExclusive,
                                  String query, String state) throws SQLException {
        final String sql = "SELECT COUNT(*) " + MONTHLY_FROM + monthlyWhere(query, state);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindMonthlyFilters(ps, 1, userId, branchId, monthStart, monthEndExclusive, query);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static String monthlyWhere(String query, String state) {
        StringBuilder where = new StringBuilder(
                "WHERE sa.UserId=? AND st.BranchId=? AND sa.WorkDate>=? AND sa.WorkDate<? ");
        if (hasText(query)) {
            // Tìm theo tên ca hoặc theo đúng ngày người dùng đang đọc trên bảng (dd/MM/yyyy).
            where.append("AND (st.Name LIKE ? ESCAPE '\\' ")
                 .append("OR CONVERT(varchar(10), sa.WorkDate, 103) LIKE ? ESCAPE '\\') ");
        }
        where.append(monthlyStateCondition(state));
        return where.toString();
    }

    /** Ca đã chốt giờ — điều kiện chung của ba trạng thái duyệt. */
    private static final String MONTHLY_CLOSED = "AND a.CheckInAt IS NOT NULL AND a.CheckOutAt IS NOT NULL ";

    /**
     * Trạng thái hiển thị suy ra từ mốc chấm công chứ không chỉ từ Attendance.Status, nên bộ lọc phải
     * lặp đúng thứ tự ưu tiên của {@link MonthlyAttendanceRow#getStateLabel()}: chưa vào ca = Vắng,
     * vào mà chưa tan = Chưa tan ca, còn lại mới xét trạng thái duyệt. Lệch một nhánh là bộ lọc trả về
     * dòng mang nhãn khác với mục vừa chọn.
     */
    private static String monthlyStateCondition(String state) {
        if (state == null) return "";
        switch (state) {
            case "ABSENT":   return "AND a.CheckInAt IS NULL ";
            case "OPEN":     return "AND a.CheckInAt IS NOT NULL AND a.CheckOutAt IS NULL ";
            case "APPROVED": return MONTHLY_CLOSED + "AND a.Status='APPROVED' ";
            case "REJECTED": return MONTHLY_CLOSED + "AND a.Status='REJECTED' ";
            // Status NULL vẫn là chờ duyệt: getStateLabel() rơi về nhánh mặc định khi không phải APPROVED/REJECTED.
            case "PENDING":  return MONTHLY_CLOSED + "AND (a.Status IS NULL OR a.Status NOT IN ('APPROVED','REJECTED')) ";
            default:         return "";
        }
    }

    private static int bindMonthlyFilters(PreparedStatement ps, int idx, int userId, int branchId,
                                          LocalDate monthStart, LocalDate monthEndExclusive,
                                          String query) throws SQLException {
        ps.setInt(idx++, userId);
        ps.setInt(idx++, branchId);
        ps.setDate(idx++, Date.valueOf(monthStart));
        ps.setDate(idx++, Date.valueOf(monthEndExclusive));
        if (hasText(query)) {
            String pattern = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            ps.setNString(idx++, pattern);
            ps.setNString(idx++, pattern);
        }
        return idx;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
        Timestamp aa = rs.getTimestamp("ApprovedAt");
        if (aa != null) a.setApprovedAt(aa.toLocalDateTime());
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
