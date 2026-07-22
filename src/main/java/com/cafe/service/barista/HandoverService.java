package com.cafe.service.barista;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.BranchDao;
import com.cafe.dao.manager.ShiftAssignmentDao;
import com.cafe.dao.manager.ShiftHandoverDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.model.Branch;
import com.cafe.model.OrderItem;
import com.cafe.model.ShiftAssignment;
import com.cafe.model.ShiftHandover;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.shared.InventoryService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Bàn giao ca có người nhận, xác nhận tiếp nhận và đầu việc theo dõi tới khi hoàn tất. */
public class HandoverService {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ROLE_BARISTA = "BARISTA";
    private final ShiftHandoverDao handoverDao = new ShiftHandoverDao();
    private final ShiftAssignmentDao assignmentDao = new ShiftAssignmentDao();
    private final BranchDao branchDao = new BranchDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final InventoryService inventoryService = new InventoryService();
    private final AttendanceService attendanceService = new AttendanceService();

    public List<ShiftHandover> getHandovers(int branchId, int currentUserId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return handoverDao.findByBranch(conn, branchId, currentUserId); }
    }

    public List<ShiftHandover> getManagerFallbacks(int branchId, int managerUserId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return handoverDao.findManagerFallbacks(conn, branchId, managerUserId); }
    }

    public int countUnacknowledgedForUser(int branchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return handoverDao.countUnacknowledgedForUser(conn, branchId, userId); }
    }

    /** Preview người nhận trước khi barista gửi. Chỉ hợp lệ khi đang có attendance mở. */
    public ReceiverPlan previewReceiver(int branchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ShiftAssignment source = handoverDao.findOpenSourceAssignment(conn, userId, branchId);
            if (source == null) throw new IllegalStateException("Bạn cần đang trong ca để lập bàn giao.");
            return resolveReceiver(conn, branchId, source);
        }
    }

    public int createHandover(int branchId, int userId, String note, List<String> taskContents) throws SQLException {
        return create(branchId, userId, note, taskContents, false);
    }

    /** Lưu bàn giao và tan ca cùng một transaction, không để sinh dữ liệu nửa chừng. */
    public int createHandoverAndClockOut(int branchId, int userId, String note, List<String> taskContents) throws SQLException {
        return create(branchId, userId, note, taskContents, true);
    }

    private int create(int branchId, int userId, String note, List<String> taskContents, boolean clockOut) throws SQLException {
        List<String> tasks = normalizeTasks(taskContents);
        if (tasks.isEmpty()) throw new IllegalArgumentException("Cần có ít nhất một việc cần bàn giao.");
        String safeNote = note == null ? "" : note.trim();
        if (safeNote.length() > 1000) throw new IllegalArgumentException("Ghi chú chung tối đa 1000 ký tự.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ShiftAssignment source = handoverDao.findOpenSourceAssignment(conn, userId, branchId);
                if (source == null) throw new IllegalStateException("Bạn cần đang trong ca để lập bàn giao.");
                ReceiverPlan receiver = resolveReceiver(conn, branchId, source);
                int id = handoverDao.insert(conn, branchId, safeNote, userId, source.getShiftAssignmentId());
                for (ShiftAssignment assignment : receiver.assignments) handoverDao.insertRecipient(conn, id, assignment.getUserId(), assignment.getShiftAssignmentId(), "NEXT_SHIFT");
                if (receiver.managerFallbackUserId != null) handoverDao.insertRecipient(conn, id, receiver.managerFallbackUserId, null, "MANAGER_FALLBACK");
                for (String task : tasks) handoverDao.insertTask(conn, id, task);
                if (clockOut) attendanceService.clockOut(conn, userId, branchId);
                conn.commit();
                return id;
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void acknowledge(int branchId, int handoverId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensureHandoverBranch(conn, handoverId, branchId);
                if (!handoverDao.acknowledge(conn, handoverId, userId)) throw new IllegalStateException("Bàn giao này đã nhận hoặc không được gửi cho bạn.");
                handoverDao.refreshOverallStatus(conn, handoverId); conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateTaskStatus(int branchId, int handoverId, int taskId, String status, int userId) throws SQLException {
        if (!("NEW".equals(status) || "IN_PROGRESS".equals(status) || "DONE".equals(status))) throw new IllegalArgumentException("Trạng thái việc không hợp lệ.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensureHandoverBranch(conn, handoverId, branchId);
                if (!handoverDao.isAcknowledgedRecipient(conn, handoverId, userId)) throw new IllegalStateException("Bạn cần xác nhận đã nhận bàn giao trước.");
                if (!handoverDao.updateTaskStatus(conn, taskId, handoverId, status, userId)) throw new IllegalArgumentException("Việc bàn giao không tồn tại.");
                handoverDao.refreshOverallStatus(conn, handoverId); conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private void ensureHandoverBranch(Connection conn, int handoverId, int branchId) throws SQLException {
        // Reuse scoped lookup; this also avoids updating an id from another branch through crafted POST data.
        for (ShiftHandover handover : handoverDao.findByBranch(conn, branchId, 0)) if (handover.getShiftHandoverId() == handoverId) return;
        throw new IllegalArgumentException("Bàn giao không thuộc chi nhánh hiện tại.");
    }

    private ReceiverPlan resolveReceiver(Connection conn, int branchId, ShiftAssignment source) throws SQLException {
        LocalDateTime sourceEnd = scheduledEnd(source);
        List<ShiftAssignment> schedule = assignmentDao.findByBranchRange(conn, branchId, source.getWorkDate(), source.getWorkDate().plusDays(8));
        LocalDateTime nextStart = null;
        List<ShiftAssignment> recipients = new ArrayList<>();
        for (ShiftAssignment candidate : schedule) {
            if (!ROLE_BARISTA.equals(candidate.getRoleCode())) continue;
            LocalDateTime start = scheduledStart(candidate);
            if (!start.isAfter(sourceEnd)) continue;
            if (nextStart == null || start.isBefore(nextStart)) { nextStart = start; recipients.clear(); recipients.add(candidate); }
            else if (start.equals(nextStart)) recipients.add(candidate);
        }
        if (!recipients.isEmpty()) return new ReceiverPlan(recipients, null, labelFor(recipients));
        Branch branch = branchDao.findById(conn, branchId);
        if (branch == null || branch.getManagerUserId() == null) throw new IllegalStateException("Chưa có barista ca tiếp theo và chi nhánh chưa có quản lý nhận bàn giao.");
        return new ReceiverPlan(List.of(), branch.getManagerUserId(), "Quản lý chi nhánh" + (branch.getManagerName() == null ? "" : ": " + branch.getManagerName()));
    }

    static LocalDateTime scheduledStart(ShiftAssignment a) { return LocalDateTime.of(a.getWorkDate(), a.getStartTime()); }
    static LocalDateTime scheduledEnd(ShiftAssignment a) {
        LocalDate endDate = a.getEndTime().isAfter(a.getStartTime()) ? a.getWorkDate() : a.getWorkDate().plusDays(1);
        return LocalDateTime.of(endDate, a.getEndTime());
    }
    private static String labelFor(List<ShiftAssignment> assignments) {
        ShiftAssignment first = assignments.get(0);
        return first.getTemplateName() + " " + first.getStartTime() + "–" + first.getEndTime() + " · " + assignments.size() + " barista";
    }
    private static List<String> normalizeTasks(List<String> incoming) {
        List<String> result = new ArrayList<>();
        if (incoming == null) return result;
        for (String raw : incoming) {
            String task = raw == null ? "" : raw.trim();
            if (task.isEmpty()) continue;
            if (task.length() > 500) throw new IllegalArgumentException("Mỗi việc bàn giao tối đa 500 ký tự.");
            if (result.size() == 10) throw new IllegalArgumentException("Tối đa 10 việc trong một bàn giao.");
            result.add(task);
        }
        return result;
    }

    public int countExpiredActivePrepBatches(int branchId) throws SQLException { return inventoryService.getExpiredActivePrepBatches(branchId).size(); }

    public HandoverKpi getKpi(int branchId) throws SQLException {
        LocalDateTime[] window = todayWindowUtc();
        try (Connection conn = DBConnection.getConnection()) { long[] stats = orderItemDao.leadTimeStats(conn, branchId, window[0], window[1]); return new HandoverKpi(stats[0], stats[1]); }
    }

    /** Giữ contract đọc danh sách ly đã pha của màn cũ và test tích hợp. */
    public List<OrderItem> getBrewHistory(int branchId) throws SQLException {
        LocalDateTime[] window = todayWindowUtc();
        try (Connection conn = DBConnection.getConnection()) { return orderItemDao.findBrewedToday(conn, branchId, window[0], window[1]); }
    }

    public BrewHistoryPage getBrewHistoryPage(int branchId, String query, String status, String orderType, int requestedPage, int pageSize) throws SQLException {
        LocalDateTime[] window = todayWindowUtc(); int safePageSize = pageSize > 0 ? pageSize : 10;
        try (Connection conn = DBConnection.getConnection()) {
            int total = orderItemDao.countBrewedToday(conn, branchId, window[0], window[1], query, status, orderType);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / safePageSize)); int page = Math.max(1, Math.min(requestedPage, totalPages));
            return new BrewHistoryPage(orderItemDao.findBrewedTodayPage(conn, branchId, window[0], window[1], query, status, orderType, (page - 1) * safePageSize, safePageSize), total, page, safePageSize);
        }
    }
    private static LocalDateTime[] todayWindowUtc() { LocalDate today = LocalDate.now(VN_ZONE); return new LocalDateTime[]{today.atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(), today.plusDays(1).atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()}; }

    public static class ReceiverPlan {
        private final List<ShiftAssignment> assignments; private final Integer managerFallbackUserId; private final String label;
        ReceiverPlan(List<ShiftAssignment> assignments, Integer managerFallbackUserId, String label) { this.assignments = assignments; this.managerFallbackUserId = managerFallbackUserId; this.label = label; }
        public String getLabel() { return label; }
        public boolean isManagerFallback() { return managerFallbackUserId != null; }
    }
    public static class HandoverKpi { private final long avgLeadSeconds, cupCount; public HandoverKpi(long avgLeadSeconds, long cupCount) { this.avgLeadSeconds=avgLeadSeconds; this.cupCount=cupCount; } public long getCupCount(){return cupCount;} public boolean isHasLead(){return avgLeadSeconds>=0;} public String getAvgLeadDisplay(){ if(avgLeadSeconds<0)return "—"; long m=avgLeadSeconds/60,s=avgLeadSeconds%60; return (m>0?m+" phút ":"")+s+" giây";} }
    public static class BrewHistoryPage { private final List<OrderItem> items; private final int total,page,pageSize; public BrewHistoryPage(List<OrderItem> items,int total,int page,int pageSize){this.items=items;this.total=total;this.page=page;this.pageSize=pageSize;} public List<OrderItem> getItems(){return items;} public int getTotal(){return total;} public int getPage(){return page;} public int getPageSize(){return pageSize;} public int getTotalPages(){return Math.max(1,(int)Math.ceil((double)total/pageSize));} public boolean isHasPrevious(){return page>1;} public boolean isHasNext(){return page<getTotalPages();} public int getStartRow(){return total==0?0:(page-1)*pageSize+1;} public int getEndRow(){return Math.min(page*pageSize,total);} public List<Integer> getVisiblePages(){List<Integer> pages=new ArrayList<>();int start=Math.max(1,page-2),end=Math.min(getTotalPages(),start+4);start=Math.max(1,end-4);for(int i=start;i<=end;i++)pages.add(i);return pages;} }
}
