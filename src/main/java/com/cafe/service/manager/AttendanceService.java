package com.cafe.service.manager;

import com.cafe.config.DBConnection;
import com.cafe.dao.manager.AttendanceDao;
import com.cafe.model.Attendance;
import com.cafe.model.ShiftAssignment;
import com.cafe.model.ShiftClockStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/** M3 · AttendanceService — duyệt chấm công. */
public class AttendanceService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceDao dao = new AttendanceDao();

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
    public void setApprovalStates(List<Integer> shownIds, Set<Integer> checkedIds, int approverId) throws SQLException {
        txVoid(c -> {
            for (Integer id : shownIds) {
                if (checkedIds.contains(id)) dao.updateApproval(c, id, "APPROVED", approverId);
                else dao.updateApproval(c, id, "PENDING", null);
            }
        });
    }

    /** Mở lại bản ghi đã từ chối → PENDING. */
    public void reopenAttendance(int id) throws SQLException {
        txVoid(c -> dao.updateApproval(c, id, "PENDING", null));
    }

    public Attendance getAttendance(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findById(c, id); }
    }

    public void approveAttendance(int id, int approverId) throws SQLException {
        txVoid(c -> dao.updateStatus(c, id, "APPROVED", approverId));
    }

    public void rejectAttendance(int id, int approverId) throws SQLException {
        txVoid(c -> dao.updateStatus(c, id, "REJECTED", approverId));
    }

    /** Manager sửa giờ check-in/out tay. */
    public void updateAttendance(int id, LocalDateTime checkIn, LocalDateTime checkOut) throws SQLException {
        Timestamp ci = checkIn == null ? null : Timestamp.valueOf(checkIn);
        Timestamp co = checkOut == null ? null : Timestamp.valueOf(checkOut);
        txVoid(c -> dao.update(c, id, ci, co));
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

        dao.update(c, existing.getAttendanceId(), dao.currentUtc(c), null);
        dao.updateApproval(c, existing.getAttendanceId(), "PENDING", null);
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
        dao.update(c, existing.getAttendanceId(), Timestamp.valueOf(existing.getCheckInAt()), dao.currentUtc(c));
        dao.updateApproval(c, existing.getAttendanceId(), "PENDING", null);
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
            status.setStatusText("Đang trong ca từ " + attendance.getCheckInAt().toLocalTime().format(TIME_FMT) + ".");
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
        if (start == null || end == null) return 0d;
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 0) return 0d;
        return Math.round(minutes / 60.0 * 10) / 10.0;
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
