package com.cafe.service.manager;

import com.cafe.common.BusinessDay;
import com.cafe.common.BusinessException;
import com.cafe.common.ShiftConflict;
import com.cafe.common.ShiftHours;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.UserDao;
import com.cafe.dao.shared.ShiftAssignmentDao;
import com.cafe.model.ShiftAssignment;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Phân ca trực tiếp, không còn ca mẫu lưu trong database. */
public class ShiftService {

    public static final Duration LATE_ASSIGNMENT_GRACE = Duration.ofMinutes(10);

    private final ShiftAssignmentDao assignmentDao;
    private final UserDao userDao;

    public ShiftService() {
        this(new ShiftAssignmentDao(), new UserDao());
    }

    public ShiftService(ShiftAssignmentDao assignmentDao, UserDao userDao) {
        this.assignmentDao = java.util.Objects.requireNonNull(assignmentDao);
        this.userDao = java.util.Objects.requireNonNull(userDao);
    }

    public List<ShiftAssignment> getWeekSchedule(int branchId, LocalDate weekStart) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return assignmentDao.findByBranchAndWeek(c, branchId, weekStart);
        }
    }

    public int assignShift(String shiftName, LocalTime startTime, LocalTime endTime,
                           int userId, LocalDate date, int branchId) throws SQLException {
        ShiftInput input = validateInput(shiftName, startTime, endTime, userId, date, branchId);
        try {
            return tx(c -> {
                validateAssignment(c, input, 0);
                int assignmentId = assignmentDao.insert(c, input.shiftName, input.startTime, input.endTime,
                        input.userId, input.workDate, input.branchId);
                if (assignmentId == 0) {
                    throw new ShiftConflictException(
                            "Nhân viên không còn hoạt động tại chi nhánh hiện tại.");
                }
                return assignmentId;
            });
        } catch (SQLException e) {
            rethrowDuplicate(e);
            throw e;
        }
    }

    public void updateShift(int assignmentId, String shiftName, LocalTime startTime, LocalTime endTime,
                            int userId, LocalDate date, int branchId) throws SQLException {
        if (assignmentId <= 0) throw new BusinessException("Mã phân ca không hợp lệ.");
        ShiftInput input = validateInput(shiftName, startTime, endTime, userId, date, branchId);
        try {
            txVoid(c -> {
                ShiftAssignment current = requireBranchAssignment(c, assignmentId, branchId);
                if (assignmentDao.hasAttendance(c, assignmentId)) {
                    throw new BusinessException(
                            "Nhân viên đã chấm công cho ca này, không thể sửa lịch phân công.");
                }
                validateAssignment(c, input, current.getShiftAssignmentId());
                if (assignmentDao.update(c, assignmentId, input.shiftName, input.startTime, input.endTime,
                        input.userId, input.workDate, input.branchId) != 1) {
                    throw new BusinessException("Không thể cập nhật lịch phân công.");
                }
            });
        } catch (SQLException e) {
            rethrowDuplicate(e);
            throw e;
        }
    }

    public void unassignShift(int assignmentId, int branchId) throws SQLException {
        txVoid(c -> {
            ShiftAssignment assignment = requireBranchAssignment(c, assignmentId, branchId);
            if (assignmentDao.hasOpenAttendance(c, assignmentId)) {
                throw new BusinessException(
                        "Nhân viên đang có mặt trong ca làm, không thể gỡ khỏi ca.");
            }
            if (assignmentDao.hasAttendance(c, assignmentId)) {
                throw new BusinessException(
                        "Nhân viên đã chấm công cho ca này, không thể gỡ khỏi ca.");
            }
            LocalDateTime shiftStart = LocalDateTime.of(
                    assignment.getWorkDate(), assignment.getStartTime());
            if (!LocalDateTime.now(BusinessDay.VN_ZONE).isBefore(shiftStart)) {
                throw new BusinessException(
                        "Ca làm đã bắt đầu hoặc đã diễn ra, không thể gỡ nhân viên.");
            }
            if (assignmentDao.delete(c, assignmentId, branchId) != 1) {
                throw new BusinessException("Không tìm thấy lịch phân công cần gỡ.");
            }
        });
    }

    private void validateAssignment(Connection c, ShiftInput input, int excludedAssignmentId)
            throws SQLException {
        if (!userDao.isActiveInBranch(c, input.userId, input.branchId)) {
            throw new ShiftConflictException(
                    "Nhân viên không hoạt động tại chi nhánh hiện tại.");
        }
        LocalDateTime nowVn = LocalDateTime.now(BusinessDay.VN_ZONE);
        if (!canAssign(input.workDate, input.startTime, nowVn)) {
            throw new ShiftConflictException(
                    "Chỉ có thể xếp nhân viên trước hoặc trong vòng 10 phút sau khi ca bắt đầu.");
        }

        List<ShiftAssignment> sameDay =
                assignmentDao.findByUserAndDate(c, input.userId, input.workDate);
        List<ShiftAssignment> nearby = new ArrayList<>(sameDay);
        nearby.addAll(assignmentDao.findByUserAndDate(
                c, input.userId, input.workDate.minusDays(1)));
        nearby.addAll(assignmentDao.findByUserAndDate(
                c, input.userId, input.workDate.plusDays(1)));
        ShiftAssignment conflict = detectConflict(
                input.workDate, input.startTime, input.endTime,
                nearby, excludedAssignmentId);
        if (conflict != null) {
            throw new ShiftConflictException(
                    "Nhân viên đã có ca \"" + conflict.getShiftName() + "\" (" +
                    conflict.getStartTime() + "–" + conflict.getEndTime() +
                    ") trùng giờ ngày " + input.workDate + ".");
        }

        double newHours = ShiftHours.hours(input.startTime, input.endTime);
        double dailyTotal = totalHours(sameDay, excludedAssignmentId) + newHours;
        if (ShiftHours.exceedsDaily(dailyTotal)) {
            throw new ShiftConflictException(
                    "Vượt 8 giờ/ngày (" + formatHours(dailyTotal) +
                    "h). Không thể xếp ca này.");
        }

        LocalDate weekStart = input.workDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        double weeklyTotal = totalHours(
                assignmentDao.findByUserAndWeek(c, input.userId, weekStart),
                excludedAssignmentId) + newHours;
        if (ShiftHours.exceedsWeekly(weeklyTotal)) {
            throw new ShiftConflictException(
                    "Vượt 48 giờ/tuần (" + formatHours(weeklyTotal) +
                    "h). Không thể xếp ca này.");
        }
    }

    private ShiftInput validateInput(String shiftName, LocalTime startTime, LocalTime endTime,
                                     int userId, LocalDate date, int branchId) {
        String normalizedName = shiftName == null
                ? null : shiftName.trim().replaceAll("\\s+", " ");
        if (normalizedName == null || normalizedName.isBlank()) {
            throw new BusinessException("Tên ca làm không được để trống.");
        }
        if (normalizedName.length() > 60) {
            throw new BusinessException("Tên ca làm tối đa 60 ký tự.");
        }
        if (startTime == null || endTime == null || startTime.equals(endTime)) {
            throw new BusinessException(
                    "Giờ bắt đầu và giờ kết thúc là bắt buộc và không được trùng nhau.");
        }
        if (userId <= 0 || branchId <= 0 || date == null) {
            throw new BusinessException("Thông tin phân ca không hợp lệ.");
        }
        return new ShiftInput(normalizedName, startTime, endTime, userId, date, branchId);
    }

    private ShiftAssignment requireBranchAssignment(Connection c, int assignmentId, int branchId)
            throws SQLException {
        ShiftAssignment assignment = assignmentDao.findById(c, assignmentId);
        if (assignment == null) {
            throw new BusinessException("Lịch phân công không tồn tại.");
        }
        if (assignment.getBranchId() != branchId) {
            throw new BusinessException(
                    "Lịch phân công không thuộc chi nhánh hiện tại.");
        }
        return assignment;
    }

    private ShiftAssignment detectConflict(LocalDate workDate,
                                           LocalTime startTime, LocalTime endTime,
                                           List<ShiftAssignment> existing,
                                           int excludedAssignmentId) {
        for (ShiftAssignment assignment : existing) {
            if (assignment.getShiftAssignmentId() == excludedAssignmentId) continue;
            if (ShiftConflict.overlaps(workDate, startTime, endTime,
                    assignment.getWorkDate(), assignment.getStartTime(),
                    assignment.getEndTime())) {
                return assignment;
            }
        }
        return null;
    }

    public static boolean canAssign(LocalDate workDate, LocalTime startTime,
                                    LocalDateTime nowVn) {
        LocalDateTime deadline = LocalDateTime.of(workDate, startTime)
                .plus(LATE_ASSIGNMENT_GRACE);
        return !nowVn.isAfter(deadline);
    }

    private double totalHours(List<ShiftAssignment> assignments, int excludedAssignmentId) {
        double total = 0d;
        for (ShiftAssignment assignment : assignments) {
            if (assignment.getShiftAssignmentId() == excludedAssignmentId) continue;
            total += ShiftHours.hours(
                    assignment.getStartTime(), assignment.getEndTime());
        }
        return total;
    }

    private String formatHours(double hours) {
        if (hours == Math.rint(hours)) return String.valueOf((int) hours);
        return String.format(java.util.Locale.US, "%.1f", hours);
    }

    private void rethrowDuplicate(SQLException error) {
        for (SQLException current = error; current != null; current = current.getNextException()) {
            if (current.getErrorCode() == 2601 || current.getErrorCode() == 2627) {
                throw new ShiftConflictException(
                        "Nhân viên đã có ca bắt đầu cùng thời điểm trong ngày này.");
            }
        }
    }

    public static class ShiftConflictException extends BusinessException {
        public ShiftConflictException(String message) {
            super(message);
        }
    }

    private record ShiftInput(String shiftName, LocalTime startTime, LocalTime endTime,
                              int userId, LocalDate workDate, int branchId) { }

    private interface Fn<T> { T run(Connection c) throws SQLException; }
    private interface V { void run(Connection c) throws SQLException; }

    private <T> T tx(Fn<T> fn) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = fn.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void txVoid(V action) throws SQLException {
        tx(c -> {
            action.run(c);
            return null;
        });
    }
}
