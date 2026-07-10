package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.ShiftConflict;
import com.cafe.common.ShiftHours;
import com.cafe.config.DBConnection;
import com.cafe.dao.manager.ShiftAssignmentDao;
import com.cafe.dao.manager.ShiftTemplateDao;
import com.cafe.model.ShiftAssignment;
import com.cafe.model.ShiftTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** M2 · ShiftService — ca làm + ★ Shift Conflict Resolver. */
public class ShiftService {

    private final ShiftTemplateDao templateDao = new ShiftTemplateDao();
    private final ShiftAssignmentDao assignmentDao = new ShiftAssignmentDao();

    public List<ShiftTemplate> getShiftTemplates(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return templateDao.findByBranch(c, branchId); }
    }

    public int createShiftTemplate(ShiftTemplate t) throws SQLException {
        return tx(c -> templateDao.insert(c, t));
    }

    public void deleteShiftTemplate(int templateId) throws SQLException {
        txVoid(c -> templateDao.delete(c, templateId));
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
    public int assignShift(int templateId, int userId, LocalDate date) throws SQLException {
        return tx(c -> {
            ShiftTemplate target = templateDao.findById(c, templateId);
            if (target == null) throw new ShiftConflictException("Ca làm không tồn tại.");

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

            return assignmentDao.insert(c, templateId, userId, date);
        });
    }

    public void unassignShift(int assignmentId) throws SQLException {
        txVoid(c -> assignmentDao.delete(c, assignmentId));
    }

    /** Báo lỗi nghiệp vụ khi xung đột ca — servlet bắt để hiển thị. */
    public static class ShiftConflictException extends BusinessException {
        public ShiftConflictException(String msg) { super(msg); }
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
