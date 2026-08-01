package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.ShiftConflict;
import com.cafe.common.ShiftHours;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.UserDao;
import com.cafe.dao.shared.ShiftAssignmentDao;
import com.cafe.dao.manager.ShiftTemplateDao;
import com.cafe.model.ShiftAssignment;
import com.cafe.model.ShiftTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** M2 · ShiftService — ca làm + ★ Shift Conflict Resolver. */
public class ShiftService {

    public static final Duration LATE_ASSIGNMENT_GRACE = Duration.ofMinutes(10);

    private final ShiftTemplateDao templateDao;
    private final ShiftAssignmentDao assignmentDao;
    private final UserDao userDao;

    public ShiftService() { this(new ShiftTemplateDao(), new ShiftAssignmentDao(), new UserDao()); }
    public ShiftService(ShiftTemplateDao templateDao, ShiftAssignmentDao assignmentDao, UserDao userDao) {
        this.templateDao = java.util.Objects.requireNonNull(templateDao);
        this.assignmentDao = java.util.Objects.requireNonNull(assignmentDao);
        this.userDao = java.util.Objects.requireNonNull(userDao);
    }

    public List<ShiftTemplate> getShiftTemplates(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return templateDao.findByBranch(c, branchId); }
    }

    public int createShiftTemplate(ShiftTemplate t) throws SQLException {
        String name = t.getName() == null ? null : t.getName().trim().replaceAll("\\s+", " ");
        if (name == null || name.isBlank()) throw new BusinessException("Tên ca làm không được để trống.");
        t.setName(name);
        try {
            return tx(c -> templateDao.insert(c, t));
        } catch (SQLException e) {
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627)
                throw new BusinessException("Tên ca làm đã tồn tại trong chi nhánh này.");
            throw e;
        }
    }

    public void deleteShiftTemplate(int templateId, int branchId) throws SQLException {
        txVoid(c -> {
            ShiftTemplate template = templateDao.findById(c, templateId);
            if (template == null || template.getBranchId() != branchId) {
                throw new BusinessException("Không tìm thấy ca làm trong chi nhánh.");
            }
            int assignments = assignmentDao.countByTemplate(c, templateId);
            if (assignments > 0) {
                throw new BusinessException(
                        "Không thể xóa ca làm vì đã có " + assignments
                        + " lịch phân công nhân viên. Hãy gỡ các lịch hợp lệ trước.");
            }
            templateDao.delete(c, templateId);
        });
    }

    public List<ShiftAssignment> getWeekSchedule(int branchId, LocalDate weekStart) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return assignmentDao.findByBranchAndWeek(c, branchId, weekStart);
        }
    }

    /**
     * ★ Phát hiện xung đột: nhân viên đã có ca nào CHỒNG GIỜ với template mới trong cùng ngày chưa.
     * @return ca đang chồng (để báo lỗi), hoặc null nếu không xung đột.
     */
    public ShiftAssignment detectConflict(int userId, LocalDate date, int templateId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            ShiftTemplate target = templateDao.findById(c, templateId);
            if (target == null) return null;
            return detectConflict(c, userId, date, target);
        }
    }

    /**
     * Xếp ca cho nhân viên. Chặn trùng giờ (gọi detectConflict trước).
     * @throws ShiftConflictException nếu chồng ca khác.
     */
    public int assignShift(int templateId, int userId, LocalDate date, int branchId) throws SQLException {
        return tx(c -> {
            ShiftTemplate target = templateDao.findById(c, templateId);
            if (target == null || target.getBranchId() != branchId) {
                throw new ShiftConflictException("Ca làm không tồn tại trong chi nhánh hiện tại.");
            }
            if (!userDao.isActiveInBranch(c, userId, branchId)) {
                throw new ShiftConflictException("Nhân viên không hoạt động tại chi nhánh hiện tại.");
            }
            LocalDateTime nowVn = LocalDateTime.now(com.cafe.common.BusinessDay.VN_ZONE);
            if (!canAssign(date, target.getStartTime(), nowVn)) {
                throw new ShiftConflictException(
                        "Chỉ có thể xếp nhân viên trước hoặc trong vòng 10 phút sau khi ca bắt đầu.");
            }

            List<ShiftAssignment> sameDay = assignmentDao.findByUserAndDate(c, userId, date);
            ShiftAssignment conflict = detectConflict(target, sameDay);
            if (conflict != null) {
                throw new ShiftConflictException(
                    "Nhân viên đã có ca \"" + conflict.getTemplateName() + "\" (" +
                    conflict.getStartTime() + "–" + conflict.getEndTime() + ") trùng giờ ngày " + date + ".");
            }

            double newHours = ShiftHours.hours(target.getStartTime(), target.getEndTime());
            double dailyTotal = totalHours(sameDay) + newHours;
            if (ShiftHours.exceedsDaily(dailyTotal)) {
                throw new ShiftConflictException(
                    "Vượt 8 giờ/ngày (" + formatHours(dailyTotal) + "h). Không thể xếp thêm ca này.");
            }

            LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            double weeklyTotal = totalHours(assignmentDao.findByUserAndWeek(c, userId, weekStart)) + newHours;
            if (ShiftHours.exceedsWeekly(weeklyTotal)) {
                throw new ShiftConflictException(
                    "Vượt 48 giờ/tuần (" + formatHours(weeklyTotal) + "h). Không thể xếp thêm ca này.");
            }

            int assignmentId = assignmentDao.insert(c, templateId, userId, date, branchId);
            if (assignmentId == 0) {
                throw new ShiftConflictException("Ca làm hoặc nhân viên không còn hợp lệ tại chi nhánh.");
            }
            return assignmentId;
        });
    }

    public void unassignShift(int assignmentId, int branchId) throws SQLException {
        txVoid(c -> {
            ShiftAssignment assignment = assignmentDao.findById(c, assignmentId);
            if (assignment == null) throw new BusinessException("Lịch phân công không tồn tại.");
            ShiftTemplate template = templateDao.findById(c, assignment.getShiftTemplateId());
            if (template == null || template.getBranchId() != branchId) {
                throw new BusinessException("Lịch phân công không thuộc chi nhánh hiện tại.");
            }

            LocalDate today = LocalDate.now();
            if (assignment.getWorkDate().isBefore(today)) {
                throw new BusinessException("Không thể gỡ nhân viên vì ca làm đã diễn ra.");
            }
            if (assignmentDao.hasOpenAttendance(c, assignmentId)) {
                throw new BusinessException("Nhân viên đang có mặt trong ca làm, không thể gỡ khỏi ca.");
            }
            if (assignmentDao.hasAttendance(c, assignmentId)) {
                throw new BusinessException("Nhân viên đã chấm công cho ca này, không thể gỡ khỏi ca.");
            }
            LocalDateTime shiftStart = LocalDateTime.of(assignment.getWorkDate(), assignment.getStartTime());
            if (!LocalDateTime.now().isBefore(shiftStart)) {
                throw new BusinessException("Ca làm đã bắt đầu hoặc đã diễn ra, không thể gỡ nhân viên.");
            }
            if (assignmentDao.delete(c, assignmentId) == 0) {
                throw new BusinessException("Không tìm thấy lịch phân công cần gỡ.");
            }
        });
    }

    /** Báo lỗi nghiệp vụ khi xung đột ca — servlet bắt để hiển thị. */
    public static class ShiftConflictException extends BusinessException {
        public ShiftConflictException(String msg) { super(msg); }
    }

    /**
     * Cho phép xếp muộn đến đúng mốc 10 phút sau khi ca bắt đầu.
     */
    public static boolean canAssign(LocalDate workDate, java.time.LocalTime startTime, LocalDateTime nowVn) {
        LocalDateTime deadline = LocalDateTime.of(workDate, startTime).plus(LATE_ASSIGNMENT_GRACE);
        return !nowVn.isAfter(deadline);
    }

    private ShiftAssignment detectConflict(Connection c, int userId, LocalDate date, ShiftTemplate target) throws SQLException {
        return detectConflict(target, assignmentDao.findByUserAndDate(c, userId, date));
    }

    private ShiftAssignment detectConflict(ShiftTemplate target, List<ShiftAssignment> existingAssignments) {
        for (ShiftAssignment existing : existingAssignments) {
            if (existing.getShiftTemplateId() == target.getShiftTemplateId()) return existing; // trùng y hệt
            if (ShiftConflict.overlaps(target.getStartTime(), target.getEndTime(),
                    existing.getStartTime(), existing.getEndTime())) {
                return existing;
            }
        }
        return null;
    }

    private double totalHours(List<ShiftAssignment> assignments) {
        double total = 0;
        for (ShiftAssignment assignment : assignments) {
            total += ShiftHours.hours(assignment.getStartTime(), assignment.getEndTime());
        }
        return total;
    }

    private String formatHours(double hours) {
        if (hours == Math.rint(hours)) return String.valueOf((int) hours);
        return String.format(java.util.Locale.US, "%.1f", hours);
    }

    private interface Fn<T>{ T run(Connection c) throws SQLException; }
    private interface V{ void run(Connection c) throws SQLException; }
    private <T> T tx(Fn<T> fn) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { T r = fn.run(c); c.commit(); return r; }
            catch (SQLException | RuntimeException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }
    private void txVoid(V v) throws SQLException { tx(c -> { v.run(c); return null; }); }
}
