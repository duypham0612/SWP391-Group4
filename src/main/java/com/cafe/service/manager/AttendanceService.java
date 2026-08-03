package com.cafe.service.manager;

import com.cafe.config.DBConnection;
import com.cafe.common.BusinessDay;
import com.cafe.common.BusinessException;
import com.cafe.common.ShiftHours;
import com.cafe.common.ShiftWindow;
import com.cafe.dao.manager.AttendanceDao;
import com.cafe.model.MonthlyAttendanceRow;
import com.cafe.model.MonthlyWorkSummary;
import com.cafe.model.ShiftAssignment;
import com.cafe.model.ShiftClockStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** M3 · AttendanceService — duyệt chấm công. */
public class AttendanceService {

    private final AttendanceDao dao;
    public AttendanceService() { this(new AttendanceDao()); }
    public AttendanceService(AttendanceDao dao) {
        this.dao = java.util.Objects.requireNonNull(dao);
    }

    public List<ShiftAssignment> getPendingAttendance(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByStatus(c, branchId, "PENDING"); }
    }

    public List<ShiftAssignment> getAttendanceByStatus(int branchId, String status) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByStatus(c, branchId, status); }
    }

    /** Tất cả chấm công của chi nhánh (1 màn gộp). */
    public List<ShiftAssignment> getBranchAttendance(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByBranch(c, branchId); }
    }

    /**
     * Chấm công bằng tickbox: với mỗi bản ghi hiển thị (shownIds, không tính REJECTED),
     * tick = APPROVED (ghi người duyệt), bỏ tick = PENDING (xoá người duyệt). Tất cả 1 transaction.
     */
    public void setApprovalStates(List<Integer> shownIds, Set<Integer> checkedIds,
                                  int approverId, int branchId) throws SQLException {
        txVoid(c -> {
            for (Integer id : shownIds) {
                int rows = checkedIds.contains(id)
                        ? approve(c, id, approverId, branchId)
                        : dao.updateApprovalByBranch(c, id, branchId, "PENDING", null);
                requireScopedUpdate(rows);
            }
        });
    }

    /** Mở lại bản ghi đã từ chối → PENDING. */
    public void reopenAttendance(int id, int branchId) throws SQLException {
        txVoid(c -> requireScopedUpdate(
                dao.updateApprovalByBranch(c, id, branchId, "PENDING", null)));
    }

    public ShiftAssignment getAttendance(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findById(c, id); }
    }

    public void approveAttendance(int id, int approverId, int branchId) throws SQLException {
        txVoid(c -> requireScopedUpdate(approve(c, id, approverId, branchId)));
    }

    public void rejectAttendance(int id, int approverId, int branchId) throws SQLException {
        txVoid(c -> requireScopedUpdate(
                dao.updateApprovalByBranch(c, id, branchId, "REJECTED", approverId)));
    }

    /** Manager sửa giờ check-in/out tay. */
    public void updateAttendance(int id, int branchId,
                                 LocalDateTime checkIn, LocalDateTime checkOut) throws SQLException {
        Timestamp ci = checkIn == null ? null : Timestamp.valueOf(checkIn);
        Timestamp co = checkOut == null ? null : Timestamp.valueOf(checkOut);
        txVoid(c -> requireScopedUpdate(dao.updateByBranch(c, id, branchId, ci, co)));
    }

    private static void requireScopedUpdate(int rows) {
        if (rows != 1) {
            throw new BusinessException("Bản chấm công không thuộc chi nhánh hiện tại.");
        }
    }

    private int approve(Connection c, int id, int approverId, int branchId)
            throws SQLException {
        if (!dao.canApproveWithSnapshot(c, id, branchId)) {
            throw new BusinessException(
                    "Chưa thiết lập lương theo giờ cho nhân viên nên không thể duyệt chấm công.");
        }
        return dao.updateApprovalByBranch(c, id, branchId, "APPROVED", approverId);
    }

    /** Số giờ làm của 1 bản ghi chấm công (đọc lại từ DB). */
    public double computeWorkHours(int assignmentId) throws SQLException {
        ShiftAssignment a = getAttendance(assignmentId);
        return a == null ? 0d : a.getWorkHours();
    }

    /** Trạng thái chấm công hôm nay của nhân viên đang đăng nhập. */
    public ShiftClockStatus getMyShiftStatus(int userId, int branchId, LocalDate date) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<ShiftAssignment> assignments = clockableAssignments(c, userId, branchId, date, ShiftWindow.CLOCK_OUT_GRACE);
            return buildStatus(c, assignments, date);
        }
    }

    /**
     * Ca còn hiệu lực chấm công lúc này: ca trong ngày kinh doanh, cộng ca đêm hôm trước chưa hết giờ.
     * Chỉ lấy đúng ngày hôm nay thì ca đêm tan sau nửa đêm sẽ bị báo "chưa được xếp ca".
     */
    private List<ShiftAssignment> clockableAssignments(Connection c, int userId, int branchId,
                                                       LocalDate businessDate, Duration grace) throws SQLException {
        LocalDateTime nowVn = LocalDateTime.now(BusinessDay.VN_ZONE);
        List<ShiftAssignment> out = new ArrayList<>();
        for (ShiftAssignment a : dao.findClockAssignments(c, userId, branchId, businessDate)) {
            if (ShiftWindow.isClockable(a.getWorkDate(), a.getStartTime(), a.getEndTime(), businessDate, nowVn, grace)) out.add(a);
        }
        return out;
    }

    /**
     * Những người CÒN đang trực quầy ở chi nhánh lúc này (đã vào ca, chưa tan, chưa quá giờ tan
     * theo lịch + ân hạn ngắn). Quầy pha chế dùng tập này để biết món đang pha có còn chủ hay không.
     *
     * <p>Một truy vấn cho cả chi nhánh rồi lọc bằng {@link ShiftWindow#onDuty} — không hỏi từng người,
     * vì màn quầy pha chế gọi nó cho cả bảng mỗi lần làm mới.
     */
    public Set<Integer> getOnDutyUserIds(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            LocalDateTime nowVn = LocalDateTime.now(BusinessDay.VN_ZONE);
            Set<Integer> out = new java.util.HashSet<>();
            for (ShiftAssignment a : dao.findOpenByBranch(c, branchId, currentVnDate())) {
                if (ShiftWindow.onDuty(a.getWorkDate(), a.getStartTime(), a.getEndTime(),
                        a.getCheckInAt() != null, a.getCheckOutAt() != null, nowVn)) {
                    out.add(a.getUserId());
                }
            }
            return out;
        }
    }

    /** Ca còn hiệu lực chấm công của một người — dùng chung cho màn trực ca của thu ngân. */
    public List<ShiftAssignment> currentShiftAssignments(Connection c, int userId, int branchId) throws SQLException {
        return clockableAssignments(c, userId, branchId, currentVnDate(), ShiftWindow.CLOCK_OUT_GRACE);
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

    /**
     * Một trang lịch đi làm trong tháng — tìm kiếm, bộ lọc trạng thái và phân trang đều chạy ở database.
     * Phần tổng hợp phía trên màn vẫn đọc cả tháng qua {@link #getMyMonthlyHistory} nên số liệu không
     * đổi theo từ khoá đang gõ.
     */
    public MonthlyAttendancePage getMyMonthlyHistoryPage(int userId, int branchId, YearMonth ym,
                                                          String query, String state,
                                                          int requestedPage, int pageSize) throws SQLException {
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEndExclusive = ym.plusMonths(1).atDay(1);
        try (Connection c = DBConnection.getConnection()) {
            int total = dao.countMonthlyByUser(c, userId, branchId, monthStart, monthEndExclusive, query, state);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
            int page = Math.max(1, Math.min(requestedPage, totalPages));
            List<MonthlyAttendanceRow> rows = dao.findMonthlyPageByUser(c, userId, branchId,
                    monthStart, monthEndExclusive, query, state, (page - 1) * pageSize, pageSize);
            for (MonthlyAttendanceRow r : rows) {
                r.setWorkHours(ShiftHours.worked(r.getCheckInAt(), r.getCheckOutAt()));
            }
            return new MonthlyAttendancePage(rows, total, page, pageSize);
        }
    }

    public static class MonthlyAttendancePage {
        private final List<MonthlyAttendanceRow> rows;
        private final int total;
        private final int page;
        private final int pageSize;

        public MonthlyAttendancePage(List<MonthlyAttendanceRow> rows, int total, int page, int pageSize) {
            this.rows = rows;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<MonthlyAttendanceRow> getRows() { return rows; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return Math.max(1, (int) Math.ceil((double) total / pageSize)); }
        public boolean isHasPrevious() { return page > 1; }
        public boolean isHasNext() { return page < getTotalPages(); }
        public int getStartRow() { return total == 0 ? 0 : (page - 1) * pageSize + 1; }
        public int getEndRow() { return Math.min(page * pageSize, total); }

        /** Tối đa 5 số trang quanh trang hiện tại để pager không phình khi tháng có nhiều ca. */
        public List<Integer> getVisiblePages() {
            List<Integer> pages = new ArrayList<>();
            int totalPages = getTotalPages();
            int start = Math.max(1, page - 2);
            int end = Math.min(totalPages, start + 4);
            start = Math.max(1, end - 4);
            for (int value = start; value <= end; value++) pages.add(value);
            return pages;
        }
    }

    /** Tổng hợp tháng. Nhận rows từ caller để không truy vấn lại lần hai. */
    public MonthlyWorkSummary getMyMonthlySummary(int userId, int branchId, YearMonth ym,
                                                   List<MonthlyAttendanceRow> rows)
            throws SQLException {
        return summarize(rows);
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

    /** Vào ca: yêu cầu đã được xếp ca hôm nay, idempotent nếu assignment đã mở. */
    public void clockIn(int userId, int branchId) throws SQLException {
        txVoid(c -> clockIn(c, userId, branchId));
    }

    /** Lõi vào ca chạy trong transaction của caller. */
    public void clockIn(Connection c, int userId, int branchId) throws SQLException {
        // Vào ca không có biên trễ: ca đêm hôm trước chỉ tính khi còn đang chạy, tránh chấm nhầm vào ca đã qua.
        List<ShiftAssignment> assignments = clockableAssignments(c, userId, branchId, currentVnDate(), Duration.ZERO);
        if (assignments.isEmpty()) throw new IllegalStateException("Hôm nay bạn chưa được xếp ca.");

        ShiftAssignment target = chooseForClockIn(c, assignments);
        ShiftAssignment existing = dao.findByAssignmentForUpdate(c, target.getShiftAssignmentId());
        if (existing == null) throw new IllegalStateException("Không tìm thấy ca đã phân công.");
        if (existing.getCheckOutAt() != null) {
            throw new IllegalStateException("Ca này đã tan, không thể vào lại.");
        }
        if (existing.getCheckInAt() != null) return;
        if (dao.clockIn(c, existing.getShiftAssignmentId(), dao.currentUtc(c)) != 1) {
            throw new IllegalStateException("Không thể ghi nhận giờ vào ca.");
        }
    }

    /** Tan ca: chỉ cập nhật bản đang mở, giữ luồng duyệt Manager qua PENDING. */
    public void clockOut(int userId, int branchId) throws SQLException {
        txVoid(c -> clockOut(c, userId, branchId));
    }

    /**
     * Cửa sổ dò NGƯỢC chỉ để chọn đúng câu báo lỗi khi đã hết hạn chấm công — không cho phép
     * tan ca muộn hơn {@link ShiftWindow#CLOCK_OUT_GRACE}.
     */
    private static final Duration LATE_CLOCK_OUT_LOOKBACK = Duration.ofDays(2);

    /** Lõi tan ca chạy trong transaction của caller. */
    public void clockOut(Connection c, int userId, int branchId) throws SQLException {
        LocalDate businessDate = currentVnDate();
        List<ShiftAssignment> assignments = clockableAssignments(c, userId, branchId, businessDate, ShiftWindow.CLOCK_OUT_GRACE);
        if (assignments.isEmpty()) {
            // Ca CÓ tồn tại nhưng đã rơi khỏi cửa sổ chấm công: nói đúng vấn đề để nhân viên biết
            // phải nhờ Quản lý chốt giờ, thay vì tưởng mình chưa từng được xếp ca.
            if (!clockableAssignments(c, userId, branchId, businessDate, LATE_CLOCK_OUT_LOOKBACK).isEmpty()) {
                throw new IllegalStateException(
                        "Ca đã quá hạn chấm công. Bản ghi vẫn đang mở — nhờ Quản lý chốt giờ tan ca giúp bạn.");
            }
            throw new IllegalStateException("Hôm nay bạn chưa được xếp ca.");
        }

        ShiftAssignment target = chooseOpenAssignment(c, assignments);
        if (target == null) throw new IllegalStateException("Bạn chưa vào ca.");

        clockOutAssignment(c, target.getShiftAssignmentId());
    }

    /**
     * Tan ca cho đúng ca mà caller đã xác định (vd bàn giao ca đã chốt được ca nguồn).
     * Không dò lại theo ngày nên không đóng nhầm sang ca khác khi một người có nhiều ca mở.
     * Caller phải bảo đảm assignment thuộc về chính người đang thao tác.
     */
    public void clockOutAssignment(Connection c, int shiftAssignmentId) throws SQLException {
        // Khoá dòng như clockIn: hai tab bấm tan ca cùng lúc thì tab sau đọc được CheckOutAt đã ghi và bị chặn.
        ShiftAssignment existing = dao.findByAssignmentForUpdate(c, shiftAssignmentId);
        if (existing == null || existing.getCheckInAt() == null || existing.getCheckOutAt() != null) {
            throw new IllegalStateException("Bạn chưa vào ca.");
        }
        dao.update(c, existing.getShiftAssignmentId(), Timestamp.valueOf(existing.getCheckInAt()), dao.currentUtc(c));
        dao.updateApproval(c, existing.getShiftAssignmentId(), "PENDING", null);
    }

    private ShiftClockStatus buildStatus(Connection c, List<ShiftAssignment> assignments, LocalDate date) throws SQLException {
        if (assignments.isEmpty()) {
            ShiftClockStatus status = new ShiftClockStatus();
            status.setWorkDate(date);
            return status;
        }

        ShiftAssignment firstUnclocked = null;
        ShiftAssignment lastClosed = null;
        ShiftAssignment lastClosedAttendance = null;
        for (ShiftAssignment assignment : assignments) {
            ShiftAssignment attendance = dao.findByAssignment(c, assignment.getShiftAssignmentId());
            if (attendance != null && attendance.getAttendanceStatus() != null
                    && attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) {
                return statusFor(c, assignment, attendance);
            }
            if ((attendance == null || attendance.getAttendanceStatus() == null
                    || attendance.getCheckInAt() == null) && firstUnclocked == null) {
                firstUnclocked = assignment;
            }
            if (attendance != null && attendance.getAttendanceStatus() != null
                    && attendance.getCheckOutAt() != null) {
                lastClosed = assignment;
                lastClosedAttendance = attendance;
            }
        }

        if (firstUnclocked != null) return statusFor(c, firstUnclocked, null);
        return statusFor(c, lastClosed, lastClosedAttendance);
    }

    private ShiftClockStatus statusFor(Connection c, ShiftAssignment assignment, ShiftAssignment attendance) throws SQLException {
        ShiftClockStatus status = new ShiftClockStatus();
        status.setHasAssignment(true);
        status.setTemplateName(assignment.getShiftName());
        status.setWorkDate(assignment.getWorkDate());
        status.setStartTime(assignment.getStartTime());
        status.setEndTime(assignment.getEndTime());

        if (attendance == null || attendance.getAttendanceStatus() == null
                || attendance.getCheckInAt() == null) {
            status.setCanClockIn(true);
            return status;
        }

        status.setCheckInAt(attendance.getCheckInAt());
        if (attendance.getCheckOutAt() == null) {
            status.setCanClockOut(true);
            status.setWorkHours(hoursBetween(attendance.getCheckInAt(), dao.currentUtc(c).toLocalDateTime()));
            return status;
        }

        status.setClockedOut(true);
        status.setCheckOutAt(attendance.getCheckOutAt());
        status.setWorkHours(hoursBetween(attendance.getCheckInAt(), attendance.getCheckOutAt()));
        return status;
    }

    private ShiftAssignment chooseForClockIn(Connection c, List<ShiftAssignment> assignments) throws SQLException {
        ShiftAssignment lastClosed = assignments.get(assignments.size() - 1);
        for (ShiftAssignment assignment : assignments) {
            ShiftAssignment attendance = dao.findByAssignment(c, assignment.getShiftAssignmentId());
            if (attendance != null && attendance.getAttendanceStatus() != null
                    && attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) return assignment;
            if (attendance == null || attendance.getAttendanceStatus() == null
                    || attendance.getCheckInAt() == null) return assignment;
            lastClosed = assignment;
        }
        return lastClosed;
    }

    private ShiftAssignment chooseOpenAssignment(Connection c, List<ShiftAssignment> assignments) throws SQLException {
        for (ShiftAssignment assignment : assignments) {
            ShiftAssignment attendance = dao.findByAssignment(c, assignment.getShiftAssignmentId());
            if (attendance != null && attendance.getAttendanceStatus() != null
                    && attendance.getCheckInAt() != null && attendance.getCheckOutAt() == null) return assignment;
        }
        return null;
    }

    private double hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ShiftHours.worked(start, end);
    }

    /** Một nguồn ngày dùng chung cho mọi thao tác chấm công, độc lập timezone của server. */
    static LocalDate currentVnDate() {
        return BusinessDay.todayVn();
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
