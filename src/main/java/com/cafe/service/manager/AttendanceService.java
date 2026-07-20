package com.cafe.service.manager;

import com.cafe.config.DBConnection;
import com.cafe.common.BusinessDay;
import com.cafe.common.BusinessException;
import com.cafe.common.ShiftHours;
import com.cafe.dao.manager.AttendanceDao;
import com.cafe.dao.manager.PayrollDao;
import com.cafe.model.Attendance;
import com.cafe.model.MonthlyAttendanceRow;
import com.cafe.model.MonthlyWorkSummary;
import com.cafe.model.Payroll;
import com.cafe.model.ShiftAssignment;
import com.cafe.model.ShiftClockStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/** M3 · AttendanceService — duyệt chấm công. */
public class AttendanceService {

    private final AttendanceDao dao = new AttendanceDao();
    private final PayrollDao payrollDao = new PayrollDao();

    public List<Attendance> getPendingAttendance(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByStatus(c, branchId, "PENDING"); }
    }

    public List<Attendance> getAttendanceByStatus(int branchId, String status) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByStatus(c, branchId, status); }
    }

    /** Tất cả chấm công của chi nhánh (1 màn gộp). */
    public List<Attendance> getBranchAttendance(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByBranch(c, branchId); }
    }

    /**
     * Chấm công bằng tickbox: với mỗi bản ghi hiển thị (shownIds, không tính REJECTED),
     * tick = APPROVED (ghi người duyệt), bỏ tick = PENDING (xoá người duyệt). Tất cả 1 transaction.
     */
    public void setApprovalStates(int branchId, List<Integer> shownIds, Set<Integer> checkedIds, int approverId) throws SQLException {
        if (approverId <= 0) throw new BusinessException("Không xác định được người duyệt.");
        txVoid(c -> {
            for (Integer id : shownIds) {
                int changed = checkedIds.contains(id)
                        ? dao.updateApproval(c, id, branchId, "APPROVED", approverId)
                        : dao.updateApproval(c, id, branchId, "PENDING", null);
                if (changed != 1) throw new BusinessException("Bản ghi chấm công không thuộc chi nhánh của bạn.");
            }
        });
    }

    /** Mở lại bản ghi đã từ chối → PENDING. */
    public void reopenAttendance(int branchId, int id) throws SQLException {
        txVoid(c -> { if (dao.updateApproval(c, id, branchId, "PENDING", null) != 1) throw new BusinessException("Bản ghi chấm công không thuộc chi nhánh của bạn."); });
    }

    public Attendance getAttendance(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findById(c, id); }
    }

    public void approveAttendance(int branchId, int id, int approverId) throws SQLException {
        txVoid(c -> { if (dao.updateApproval(c, id, branchId, "APPROVED", approverId) != 1) throw new BusinessException("Bản ghi chấm công không thuộc chi nhánh của bạn."); });
    }

    public void rejectAttendance(int branchId, int id, int approverId) throws SQLException {
        txVoid(c -> { if (dao.updateApproval(c, id, branchId, "REJECTED", approverId) != 1) throw new BusinessException("Bản ghi chấm công không thuộc chi nhánh của bạn."); });
    }

    /** Manager sửa giờ check-in/out tay. */
    public void updateAttendance(int branchId, int id, LocalDateTime checkIn, LocalDateTime checkOut) throws SQLException {
        if (checkIn == null) throw new BusinessException("Giờ vào ca không được để trống.");
        if (checkOut != null && !checkOut.isAfter(checkIn)) throw new BusinessException("Giờ tan ca phải sau giờ vào ca.");
        Timestamp ci = checkIn == null ? null : Timestamp.valueOf(checkIn);
        Timestamp co = checkOut == null ? null : Timestamp.valueOf(checkOut);
        txVoid(c -> { if (dao.update(c, id, branchId, ci, co) != 1) throw new BusinessException("Bản ghi chấm công không thuộc chi nhánh của bạn."); });
    }

    /** Số giờ làm của 1 bản ghi chấm công (đọc lại từ DB). */
    public double computeWorkHours(int attendanceId) throws SQLException {
        Attendance a = getAttendance(attendanceId);
        return a == null ? 0d : a.getWorkHours();
    }

    /** Trạng thái chấm công hôm nay của nhân viên đang đăng nhập. */
    public ShiftClockStatus getMyShiftStatus(int userId, int branchId, LocalDate date) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<ShiftAssignment> assignments = dao.findTodayAssignments(c, userId, branchId, date);
            return buildStatus(c, assignments, date);
        }
    }

    /** Lịch đi làm 1 tháng của chính nhân viên đang đăng nhập. */
    public List<MonthlyAttendanceRow> getMyMonthlyHistory(int userId, int branchId, YearMonth ym)
            throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<MonthlyAttendanceRow> rows = dao.findMonthlyByUser(
                    c, userId, branchId, ym.atDay(1), ym.plusMonths(1).atDay(1));
            for (MonthlyAttendanceRow r : rows) {
                r.setWorkHours(ShiftHours.worked(r.getCheckInAt(), r.getCheckOutAt()));
            }
            return rows;
        }
    }

    /** Tổng hợp tháng. Nhận rows từ caller để không truy vấn lại lần hai. */
    public MonthlyWorkSummary getMyMonthlySummary(int userId, int branchId, YearMonth ym,
                                                   List<MonthlyAttendanceRow> rows)
            throws SQLException {
        MonthlyWorkSummary s = summarize(rows);
        try (Connection c = DBConnection.getConnection()) {
            Payroll p = payrollDao.findByMonth(c, branchId, ym.toString()).get(userId);
            if (p != null) {
                s.setLockedHours(p.getWorkedHours());
                s.setHourlyRate(p.getHourlyRate());
            }
        }
        return s;
    }

    static MonthlyWorkSummary summarize(List<MonthlyAttendanceRow> rows) {
        MonthlyWorkSummary s = new MonthlyWorkSummary();
        BigDecimal approved = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        BigDecimal rejected = BigDecimal.ZERO;
        if (rows == null) rows = List.of();
        for (MonthlyAttendanceRow r : rows) {
            if (r.isAbsent()) {
                s.setAbsentCount(s.getAbsentCount() + 1);
                continue;
            }
            if (r.isOpen()) {
                s.setOpenCount(s.getOpenCount() + 1);
                continue;
            }
            s.setShiftsWorked(s.getShiftsWorked() + 1);
            BigDecimal hours = BigDecimal.valueOf(r.getWorkHours());
            if ("APPROVED".equals(r.getStatus())) approved = approved.add(hours);
            else if ("REJECTED".equals(r.getStatus())) rejected = rejected.add(hours);
            else pending = pending.add(hours);
        }
        s.setApprovedHours(round1(approved));
        s.setPendingHours(round1(pending));
        s.setRejectedHours(round1(rejected));
        return s;
    }

    private static double round1(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Vào ca: yêu cầu đã được xếp ca hôm nay, idempotent nếu đã có bản đang mở. */
    public void clockIn(int userId, int branchId) throws SQLException {
        txVoid(c -> clockIn(c, userId, branchId));
    }

    /** Lõi vào ca chạy trong transaction của caller. */
    public void clockIn(Connection c, int userId, int branchId) throws SQLException {
        List<ShiftAssignment> assignments = dao.findTodayAssignments(c, userId, branchId, LocalDate.now());
        if (assignments.isEmpty()) throw new IllegalStateException("Hôm nay bạn chưa được xếp ca.");

        ShiftAssignment target = chooseForClockIn(c, assignments);
        Attendance existing = dao.findByAssignment(c, target.getShiftAssignmentId());
        if (existing == null) {
            dao.insert(c, target.getShiftAssignmentId(), dao.currentUtc(c), null, "PENDING");
            return;
        }
        if (existing.getCheckOutAt() != null) {
            throw new IllegalStateException("Ca này đã tan, không thể vào lại.");
        }
        if (existing.getCheckInAt() != null) return;

        dao.update(c, existing.getAttendanceId(), branchId, dao.currentUtc(c), null);
        dao.updateApproval(c, existing.getAttendanceId(), branchId, "PENDING", null);
    }

    /** Tan ca: chỉ cập nhật bản đang mở, giữ luồng duyệt Manager qua PENDING. */
    public void clockOut(int userId, int branchId) throws SQLException {
        txVoid(c -> clockOut(c, userId, branchId));
    }

    /** Lõi tan ca chạy trong transaction của caller. */
    public void clockOut(Connection c, int userId, int branchId) throws SQLException {
        List<ShiftAssignment> assignments = dao.findTodayAssignments(c, userId, branchId, LocalDate.now());
        if (assignments.isEmpty()) throw new IllegalStateException("Hôm nay bạn chưa được xếp ca.");

        ShiftAssignment target = chooseOpenAssignment(c, assignments);
        if (target == null) throw new IllegalStateException("Bạn chưa vào ca.");

        Attendance existing = dao.findByAssignment(c, target.getShiftAssignmentId());
        if (existing == null || existing.getCheckInAt() == null || existing.getCheckOutAt() != null) {
            throw new IllegalStateException("Bạn chưa vào ca.");
        }
        dao.update(c, existing.getAttendanceId(), branchId, Timestamp.valueOf(existing.getCheckInAt()), dao.currentUtc(c));
        dao.updateApproval(c, existing.getAttendanceId(), branchId, "PENDING", null);
    }

    private ShiftClockStatus buildStatus(Connection c, List<ShiftAssignment> assignments, LocalDate date) throws SQLException {
        if (assignments.isEmpty()) {
            ShiftClockStatus status = new ShiftClockStatus();
            status.setWorkDate(date);
            status.setStatusText("Hôm nay bạn chưa được xếp ca.");
            return status;
        }

        ShiftAssignment firstUnclocked = null;
        ShiftAssignment lastClosed = null;
        Attendance lastClosedAttendance = null;
        for (ShiftAssignment assignment : assignments) {
            Attendance attendance = dao.findByAssignment(c, assignment.getShiftAssignmentId());
            if (attendance != null && attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
                return statusFor(c, assignment, attendance);
            }
            if ((attendance == null || attendance.getCheckInAt() == null) && firstUnclocked == null) {
                firstUnclocked = assignment;
            }
            if (attendance != null && attendance.getCheckOutAt() != null) {
                lastClosed = assignment;
                lastClosedAttendance = attendance;
            }
        }

        if (firstUnclocked != null) return statusFor(c, firstUnclocked, null);
        return statusFor(c, lastClosed, lastClosedAttendance);
    }

    private ShiftClockStatus statusFor(Connection c, ShiftAssignment assignment, Attendance attendance) throws SQLException {
        ShiftClockStatus status = new ShiftClockStatus();
        status.setHasAssignment(true);
        status.setTemplateName(assignment.getTemplateName());
        status.setWorkDate(assignment.getWorkDate());
        status.setStartTime(assignment.getStartTime());
        status.setEndTime(assignment.getEndTime());

        if (attendance == null || attendance.getCheckInAt() == null) {
            status.setCanClockIn(true);
            status.setStatusText("Chưa vào ca.");
            return status;
        }

        status.setCheckInAt(attendance.getCheckInAt());
        if (attendance.getCheckOutAt() == null) {
            status.setCanClockOut(true);
            status.setStatusText("Đang trong ca từ " + BusinessDay.fmtTimeVn(attendance.getCheckInAt()) + ".");
            status.setWorkHours(hoursBetween(attendance.getCheckInAt(), dao.currentUtc(c).toLocalDateTime()));
            return status;
        }

        status.setClockedOut(true);
        status.setCheckOutAt(attendance.getCheckOutAt());
        status.setWorkHours(hoursBetween(attendance.getCheckInAt(), attendance.getCheckOutAt()));
        status.setStatusText("Đã tan ca.");
        return status;
    }

    private ShiftAssignment chooseForClockIn(Connection c, List<ShiftAssignment> assignments) throws SQLException {
        ShiftAssignment lastClosed = assignments.get(assignments.size() - 1);
        for (ShiftAssignment assignment : assignments) {
            Attendance attendance = dao.findByAssignment(c, assignment.getShiftAssignmentId());
            if (attendance != null && attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) return assignment;
            if (attendance == null || attendance.getCheckInAt() == null) return assignment;
            lastClosed = assignment;
        }
        return lastClosed;
    }

    private ShiftAssignment chooseOpenAssignment(Connection c, List<ShiftAssignment> assignments) throws SQLException {
        for (ShiftAssignment assignment : assignments) {
            Attendance attendance = dao.findByAssignment(c, assignment.getShiftAssignmentId());
            if (attendance != null && attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) return assignment;
        }
        return null;
    }

    private double hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ShiftHours.worked(start, end);
    }

    private interface V{ void run(Connection c) throws SQLException; }
    private void txVoid(V v) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                v.run(c);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
