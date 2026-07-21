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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        if (t == null || t.getBranchId() <= 0 || t.getName() == null || t.getName().isBlank())
            throw new BusinessException("Thông tin mẫu ca không hợp lệ.");
        if (t.getName().trim().length() > 80) throw new BusinessException("Tên ca không được vượt quá 80 ký tự.");
        if (t.getStartTime() == null || t.getEndTime() == null || !t.getStartTime().isBefore(t.getEndTime()))
            throw new BusinessException("Giờ kết thúc phải sau giờ bắt đầu.");
        t.setName(t.getName().trim());
        return tx(c -> templateDao.insert(c, t));
    }

    public void deleteShiftTemplate(int branchId, int templateId) throws SQLException {
        txVoid(c -> {
            ShiftTemplate template = templateDao.findById(c, templateId);
            if (template == null || template.getBranchId() != branchId) throw new BusinessException("Mẫu ca không thuộc chi nhánh của bạn.");
            if (assignmentDao.templateHasAssignments(c, templateId, branchId)) throw new BusinessException("Mẫu ca đang có nhân viên được phân công. Hãy gỡ các ca tương lai trước khi xóa mẫu ca.");
            if (templateDao.delete(c, templateId, branchId) != 1) throw new BusinessException("Không thể xóa mẫu ca này.");
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
    public int assignShift(int branchId, int templateId, int userId, LocalDate date) throws SQLException {
        if (date == null) throw new BusinessException("Ngày làm việc không hợp lệ.");
        return tx(c -> {
            ShiftTemplate target = templateDao.findById(c, templateId);
            if (target == null || target.getBranchId() != branchId) throw new ShiftConflictException("Ca làm không thuộc chi nhánh của bạn.");
            if (!isActiveBranchUser(c, userId, branchId)) throw new BusinessException("Nhân viên không thuộc chi nhánh của bạn hoặc đã bị khóa.");

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

    public void unassignShift(int branchId, int assignmentId) throws SQLException {
        txVoid(c -> {
            ShiftAssignment assignment = assignmentDao.findById(c, assignmentId);
            if (assignment == null) throw new BusinessException("Phân công không tồn tại.");
            ShiftTemplate template = templateDao.findById(c, assignment.getShiftTemplateId());
            if (template == null || template.getBranchId() != branchId) throw new BusinessException("Phân công không thuộc chi nhánh của bạn.");
            LocalDate today = LocalDate.now(com.cafe.common.BusinessDay.VN_ZONE);
            if (!assignment.getWorkDate().isAfter(today)) throw new BusinessException("Ca làm đã hoặc đang diễn ra nên không thể gỡ nhân viên.");
            if (assignmentDao.hasAttendance(c, assignmentId)) throw new BusinessException("Nhân viên đã chấm công cho ca này nên không thể gỡ.");
            if (assignmentDao.delete(c, assignmentId, branchId) != 1) throw new BusinessException("Không thể gỡ phân công này.");
        });
    }

    private boolean isActiveBranchUser(Connection c, int userId, int branchId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM iam.[User] WHERE UserId=? AND BranchId=? AND Status='ACTIVE'")) {
            ps.setInt(1, userId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
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
