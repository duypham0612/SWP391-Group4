package com.cafe.service.barista;

import com.cafe.common.ShiftWindow;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.BranchDao;
import com.cafe.dao.manager.ShiftAssignmentDao;
import com.cafe.dao.manager.ShiftHandoverDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.model.Branch;
import com.cafe.model.OrderItem;
import com.cafe.model.ShiftAssignment;
import com.cafe.model.ShiftHandover;
import com.cafe.model.ShiftHandoverTask;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.shared.InventoryService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bàn giao ca có người nhận, xác nhận tiếp nhận và đầu việc theo dõi tới khi hoàn tất. */
public class HandoverService {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String ROLE_BARISTA = "BARISTA";
    /** Trần số việc tồn gợi ý chuyển tiếp — nhiều hơn thì form tạo bàn giao dài quá không đọc nổi. */
    private static final int CARRY_OVER_LIMIT = 20;
    private static final int MANAGER_FALLBACK_LIMIT = 50;
    private final ShiftHandoverDao handoverDao = new ShiftHandoverDao();
    private final ShiftAssignmentDao assignmentDao = new ShiftAssignmentDao();
    private final BranchDao branchDao = new BranchDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final InventoryService inventoryService = new InventoryService();
    private final AttendanceService attendanceService = new AttendanceService();

    /** Bàn giao gửi tới quản lý — chặn trên bằng số dòng, tránh nạp cả lịch sử chi nhánh. */
    public List<ShiftHandover> getManagerFallbacks(int branchId, int managerUserId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return handoverDao.findManagerFallbacks(conn, branchId, managerUserId, MANAGER_FALLBACK_LIMIT); }
    }

    /**
     * Toàn bộ dữ liệu màn bàn giao trong một kết nối: trang danh sách đã lọc, ba con số tổng quan,
     * người nhận dự kiến và việc tồn có thể chuyển tiếp. Gọi lẻ từng hàm như trước là 5-6 lần mở
     * kết nối cho một lần mở màn, và phần dò ca nguồn bị lặp giữa preview với kiểm tra tan ca.
     */
    public HandoverBoard loadBoard(int branchId, int userId, boolean onShift,
                                   String scope, String status, String query, int requestedPage, int pageSize) throws SQLException {
        int safePageSize = pageSize > 0 ? pageSize : 5;
        try (Connection conn = DBConnection.getConnection()) {
            int total = handoverDao.countByFilter(conn, branchId, userId, scope, status, query);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / safePageSize));
            int page = Math.max(1, Math.min(requestedPage, totalPages));
            HandoverPage pageData = new HandoverPage(
                handoverDao.findPage(conn, branchId, userId, scope, status, query, (page - 1) * safePageSize, safePageSize),
                total, page, safePageSize);
            int[] counts = handoverDao.summaryCounts(conn, branchId, userId);
            HandoverBoard board = new HandoverBoard(pageData,
                new HandoverSummary(counts[0], counts[1], counts[2],
                    handoverDao.countClaimableInBranch(
                            conn, branchId, userId, ShiftHandover.CLAIM_AFTER_HOURS)));

            // Quyền lập bàn giao bám theo "còn ca đang mở", KHÔNG theo cửa sổ chấm công: ca đã quá
            // hạn bấm tan ca vẫn phải giao được việc tồn cho ca sau — giờ công thì nhờ Quản lý chốt.
            ShiftAssignment source = handoverDao.findOpenSourceAssignment(conn, userId, branchId);
            if (source == null) { board.receiverError = "Bạn cần đang trong ca để lập bàn giao."; return board; }
            board.canCreate = true;
            board.canClockOut = onShift;
            board.receiver = resolveReceiver(conn, branchId, source);
            // Đã có bàn giao cho ca này thì tan ca không còn bị chặn nữa — không nhắc lại cảnh báo.
            board.handoverRequired = !handoverDao.existsForSourceAssignment(conn, source.getShiftAssignmentId());
            board.carryOverTasks = handoverDao.findOpenTasksForUser(conn, branchId, userId, CARRY_OVER_LIMIT);
            board.brewTasks = brewSuggestions(conn, branchId);
            return board;
        }
    }

    /** Số dòng gợi ý tối đa từ hàng chờ — form chỉ nhận 10 việc, phải chừa chỗ cho việc gõ tay. */
    private static final int BREW_SUGGESTION_LIMIT = 4;

    /**
     * Việc bàn giao gợi ý sẵn, đọc từ chính hàng chờ quầy pha chế của ngày kinh doanh hiện tại.
     *
     * <p>Trước đây barista phải tự gõ lại "còn mấy ly chưa pha" — mà đúng lúc tan ca thì đó là thứ
     * dễ quên nhất, và quên thì ca sau nhận một quầy không biết đang nợ gì. Món đang pha KHÔNG nằm
     * trong gợi ý: cổng tan ca đã buộc gỡ hết trước khi tới bước này.
     */
    private List<String> brewSuggestions(Connection conn, int branchId) throws SQLException {
        Branch branch = branchDao.findById(conn, branchId);
        LocalDateTime dayStart = com.cafe.common.BusinessDay.startUtc(
                branch == null ? null : branch.getOpenTime());
        int waitingCups = 0;
        int readyCups = 0;
        List<String> blocked = new ArrayList<>();
        for (OrderItem it : orderItemDao.findBaristaWorkbench(conn, branchId, dayStart)) {
            String status = it.getStatus();
            if ("WAITING".equals(status)) waitingCups += it.getQuantity();
            else if ("READY".equals(status)) readyCups += it.getQuantity();
            else if ("BLOCKED".equals(status) && blocked.size() < BREW_SUGGESTION_LIMIT - 2) {
                blocked.add("Món tạm dừng cần xử lý: " + it.getQuantity() + " × " + it.getProductName()
                        + (it.getIssueReason() == null || it.getIssueReason().isBlank()
                           ? "" : " (" + it.getIssueReason() + ")"));
            }
        }
        List<String> out = new ArrayList<>();
        if (waitingCups > 0) out.add("Hàng chờ còn " + waitingCups + " ly chưa pha");
        if (readyCups > 0) out.add(readyCups + " ly đã pha xong đang chờ nhân viên nhận");
        out.addAll(blocked);
        return out;
    }

    public int countUnacknowledgedForUser(int branchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return handoverDao.countUnacknowledgedForUser(conn, branchId, userId); }
    }

    /**
     * True khi barista phải qua màn bàn giao trước khi được tan ca.
     * Chỉ bỏ chặn khi đang ngoài ca (clockOut tự báo lỗi sẵn có) hoặc ca này đã có bàn giao.
     *
     * <p>Trước đây còn bỏ chặn khi không dò được người nhận, và đó chính là lỗ hổng: hôm nào lịch
     * chưa xếp ca sau mà chi nhánh cũng chưa gán quản lý thì ca đó tan sạch, việc tồn biến mất khỏi
     * hệ thống. Nay bàn giao vẫn lập được ở dạng chưa có người nhận nên không cần lối thoát đó nữa.
     */
    public boolean requiresHandoverBeforeClockOut(int branchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ShiftAssignment source = handoverDao.findOpenSourceAssignment(conn, userId, branchId);
            if (source == null) return false;
            return !handoverDao.existsForSourceAssignment(conn, source.getShiftAssignmentId());
        }
    }

    /**
     * Ca sau đứng ra nhận bàn giao mồ côi hoặc đã quá hạn mà chưa ai được chỉ định xác nhận.
     * Nhận là xác nhận luôn — người bấm chính là người sẽ gánh việc, không có bước duyệt ở giữa.
     */
    public void claim(int branchId, int handoverId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ShiftAssignment mine = handoverDao.findOpenSourceAssignment(conn, userId, branchId);
                if (mine == null) throw new IllegalStateException("Bạn cần đang trong ca để tiếp nhận bàn giao này.");
                if (!handoverDao.claimStale(conn, handoverId, branchId, userId,
                        mine.getShiftAssignmentId(), ShiftHandover.CLAIM_AFTER_HOURS))
                    throw new IllegalStateException(
                            "Bàn giao này đã được tiếp nhận, chưa quá hạn hoặc không thuộc chi nhánh của bạn.");
                handoverDao.refreshOverallStatus(conn, handoverId);
                conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
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
                // Cổng tan ca thứ hai: nút "Lưu bàn giao & Tan ca" chấm công thẳng ở đây, KHÔNG đi qua
                // BaristaShift.handleClock — chỉ chặn một bên là barista vẫn tan ca được kèm ly đang pha,
                // và ly đó khoá cứng dưới tên người đã về. Kiểm trước khi ghi để không sinh dữ liệu thừa.
                if (clockOut) {
                    // Ca đã rơi khỏi cửa sổ chấm công thì chỉ lưu bàn giao, không tự đóng giờ công:
                    // clockOutAssignment không tự kiểm cửa sổ nên thiếu chỗ này là nút "Lưu bàn giao
                    // & Tan ca" trở thành đường vòng chấm công trễ vô hạn, qua mặt CLOCK_OUT_GRACE.
                    if (!withinClockOutWindow(source)) throw new IllegalStateException(
                            "Ca đã quá hạn chấm công. Bấm “Lưu bàn giao” để giao việc cho ca sau, rồi nhờ Quản lý chốt giờ tan ca giúp bạn.");
                    int pending = orderItemDao.countMakingByBarista(conn, branchId, userId);
                    if (pending > 0) throw new IllegalStateException("Bạn còn " + pending
                            + " ly đang pha — bấm “Xong” hoặc “Trả lại chờ” cho từng ly ở Quầy pha chế rồi mới tan ca được.");
                }
                ReceiverPlan receiver = resolveReceiver(conn, branchId, source);
                int id = handoverDao.insert(conn, branchId, safeNote, userId, source.getShiftAssignmentId());
                for (ShiftAssignment assignment : receiver.assignments) handoverDao.insertRecipient(conn, id, assignment.getUserId(), assignment.getShiftAssignmentId(), "NEXT_SHIFT");
                if (receiver.managerFallbackUserId != null) handoverDao.insertRecipient(conn, id, receiver.managerFallbackUserId, null, "MANAGER_FALLBACK");
                for (String task : tasks) handoverDao.insertTask(conn, id, task);
                // Tan ca đúng ca vừa bàn giao; để clockOut dò lại theo ngày sẽ đóng nhầm ca khác khi có nhiều ca mở.
                if (clockOut) attendanceService.clockOutAssignment(conn, source.getShiftAssignmentId());
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

    /** Chặn sửa bàn giao của chi nhánh khác qua POST giả mạo id. Kiểm bằng 1 truy vấn, không nạp cả danh sách. */
    private void ensureHandoverBranch(Connection conn, int handoverId, int branchId) throws SQLException {
        if (!handoverDao.existsInBranch(conn, handoverId, branchId))
            throw new IllegalArgumentException("Bàn giao không thuộc chi nhánh hiện tại.");
    }

    private ReceiverPlan resolveReceiver(Connection conn, int branchId, ShiftAssignment source) throws SQLException {
        List<ShiftAssignment> schedule = assignmentDao.findByBranchRange(conn, branchId, source.getWorkDate(), source.getWorkDate().plusDays(8));
        List<ShiftAssignment> recipients = pickNextShift(schedule, scheduledEnd(source));
        if (!recipients.isEmpty()) return new ReceiverPlan(recipients, null, labelFor(recipients), false);
        Branch branch = branchDao.findById(conn, branchId);
        if (branch != null && branch.getManagerUserId() != null)
            return new ReceiverPlan(List.of(), branch.getManagerUserId(), "Quản lý chi nhánh" + (branch.getManagerName() == null ? "" : ": " + branch.getManagerName()), false);
        // Không có barista ca sau và chi nhánh chưa gán quản lý: vẫn lập bàn giao, để trống người nhận.
        // Bỏ qua bước này là ca tan mà việc tồn không đến tay ai; còn chặn tan ca thì barista bị kẹt
        // vì một lỗi xếp lịch họ không sửa được. Người vào ca sau tự bấm tiếp nhận ở màn bàn giao.
        return new ReceiverPlan(List.of(), null, "Chưa xác định — barista vào ca sau sẽ tiếp nhận", true);
    }

    /**
     * Ca nhận bàn giao là ca barista bắt đầu sớm nhất kể từ mốc kết thúc ca nguồn; nếu nhiều
     * barista cùng ca thì tất cả đều nhận.
     *
     * <p>Mốc so sánh là {@code >=}, không phải {@code >}: lịch quán xếp nối đuôi nên ca 12:00–17:00
     * bắt đầu đúng lúc ca 07:00–12:00 kết thúc. So sánh chặt sẽ bỏ qua đúng ca tiếp quản quầy.
     * Hàm thuần này tách khỏi {@link #resolveReceiver} để kiểm thử không cần kết nối cơ sở dữ liệu.
     */
    static List<ShiftAssignment> pickNextShift(List<ShiftAssignment> schedule, LocalDateTime sourceEnd) {
        LocalDateTime nextStart = null;
        List<ShiftAssignment> recipients = new ArrayList<>();
        for (ShiftAssignment candidate : schedule) {
            if (!ROLE_BARISTA.equals(candidate.getRoleCode())) continue;
            LocalDateTime start = scheduledStart(candidate);
            if (start.isBefore(sourceEnd)) continue;
            if (nextStart == null || start.isBefore(nextStart)) { nextStart = start; recipients.clear(); recipients.add(candidate); }
            else if (start.equals(nextStart)) recipients.add(candidate);
        }
        return recipients;
    }

    /** Ca còn bấm tan ca được không — cùng cửa sổ mà {@code AttendanceService.clockOut} dùng. */
    static boolean withinClockOutWindow(ShiftAssignment a) {
        return ShiftWindow.isClockable(a.getWorkDate(), a.getStartTime(), a.getEndTime(),
                com.cafe.common.BusinessDay.todayVn(), LocalDateTime.now(com.cafe.common.BusinessDay.VN_ZONE),
                ShiftWindow.CLOCK_OUT_GRACE);
    }

    static LocalDateTime scheduledStart(ShiftAssignment a) { return LocalDateTime.of(a.getWorkDate(), a.getStartTime()); }
    /** Một nguồn sự thật cho mốc kết thúc ca đêm, dùng chung với cửa sổ chấm công. */
    static LocalDateTime scheduledEnd(ShiftAssignment a) { return ShiftWindow.scheduledEnd(a.getWorkDate(), a.getStartTime(), a.getEndTime()); }
    private static String labelFor(List<ShiftAssignment> assignments) {
        ShiftAssignment first = assignments.get(0);
        return first.getTemplateName() + " " + first.getStartTime() + "–" + first.getEndTime() + " · " + assignments.size() + " barista";
    }
    /** Bỏ trùng: việc tồn tick chuyển tiếp rất dễ trùng với việc barista tự gõ lại, ca sau nhận hai dòng y hệt. */
    private static List<String> normalizeTasks(List<String> incoming) {
        List<String> result = new ArrayList<>();
        if (incoming == null) return result;
        Set<String> seen = new HashSet<>();
        for (String raw : incoming) {
            String task = raw == null ? "" : raw.trim();
            if (task.isEmpty()) continue;
            if (task.length() > 500) throw new IllegalArgumentException("Mỗi việc bàn giao tối đa 500 ký tự.");
            if (!seen.add(task.toLowerCase())) continue;
            if (result.size() == 10) throw new IllegalArgumentException("Tối đa 10 việc trong một bàn giao.");
            result.add(task);
        }
        return result;
    }

    public int countExpiredActivePrepBatches(int branchId) throws SQLException { return inventoryService.getExpiredActivePrepBatches(branchId).size(); }

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

    /** Gói dữ liệu một lần mở màn bàn giao. Các trường phụ do {@link #loadBoard} gán sau khi dựng. */
    public static class HandoverBoard {
        private final HandoverPage page; private final HandoverSummary summary;
        private ReceiverPlan receiver; private String receiverError; private boolean handoverRequired;
        private boolean canCreate; private boolean canClockOut;
        private List<ShiftHandoverTask> carryOverTasks = List.of();
        private List<String> brewTasks = List.of();
        HandoverBoard(HandoverPage page, HandoverSummary summary) { this.page = page; this.summary = summary; }
        public HandoverPage getPage() { return page; }
        public HandoverSummary getSummary() { return summary; }
        public ReceiverPlan getReceiver() { return receiver; }
        public String getReceiverError() { return receiverError; }
        public boolean isHandoverRequired() { return handoverRequired; }
        /** Còn ca đang mở nên lập được bàn giao — kể cả khi đã quá hạn bấm tan ca. */
        public boolean isCanCreate() { return canCreate; }
        /** Ca còn trong cửa sổ chấm công nên nút "Lưu bàn giao & Tan ca" mới có tác dụng. */
        public boolean isCanClockOut() { return canClockOut; }
        public List<ShiftHandoverTask> getCarryOverTasks() { return carryOverTasks; }
        /** Việc gợi ý lấy từ chính hàng chờ quầy — barista chỉ cần tick thay vì gõ tay. */
        public List<String> getBrewTasks() { return brewTasks; }
    }

    public static class HandoverSummary {
        private final int pendingForMe, openTasksForMe, waitingReceipt, claimable;
        HandoverSummary(int pendingForMe, int openTasksForMe, int waitingReceipt, int claimable) { this.pendingForMe = pendingForMe; this.openTasksForMe = openTasksForMe; this.waitingReceipt = waitingReceipt; this.claimable = claimable; }
        public int getPendingForMe() { return pendingForMe; }
        public int getOpenTasksForMe() { return openTasksForMe; }
        public int getWaitingReceipt() { return waitingReceipt; }
        /** Bàn giao mồ côi hoặc quá hạn chưa ai xác nhận mà người đang trực có thể nhận thay. */
        public int getClaimable() { return claimable; }
    }

    public static class HandoverPage { private final List<ShiftHandover> items; private final int total,page,pageSize; public HandoverPage(List<ShiftHandover> items,int total,int page,int pageSize){this.items=items;this.total=total;this.page=page;this.pageSize=pageSize;} public List<ShiftHandover> getItems(){return items;} public int getTotal(){return total;} public int getPage(){return page;} public int getPageSize(){return pageSize;} public int getTotalPages(){return Math.max(1,(int)Math.ceil((double)total/pageSize));} public boolean isHasPrevious(){return page>1;} public boolean isHasNext(){return page<getTotalPages();} public int getStartRow(){return total==0?0:(page-1)*pageSize+1;} public int getEndRow(){return Math.min(page*pageSize,total);} public List<Integer> getVisiblePages(){List<Integer> pages=new ArrayList<>();int start=Math.max(1,page-2),end=Math.min(getTotalPages(),start+4);start=Math.max(1,end-4);for(int i=start;i<=end;i++)pages.add(i);return pages;} }

    public static class ReceiverPlan {
        private final List<ShiftAssignment> assignments; private final Integer managerFallbackUserId; private final String label; private final boolean unassigned;
        ReceiverPlan(List<ShiftAssignment> assignments, Integer managerFallbackUserId, String label, boolean unassigned) { this.assignments = assignments; this.managerFallbackUserId = managerFallbackUserId; this.label = label; this.unassigned = unassigned; }
        public String getLabel() { return label; }
        public boolean isManagerFallback() { return managerFallbackUserId != null; }
        /** Chưa chốt được người nhận — bàn giao vẫn lưu, ca sau vào sẽ tự tiếp nhận. */
        public boolean isUnassigned() { return unassigned; }
    }
    public static class BrewHistoryPage { private final List<OrderItem> items; private final int total,page,pageSize; public BrewHistoryPage(List<OrderItem> items,int total,int page,int pageSize){this.items=items;this.total=total;this.page=page;this.pageSize=pageSize;} public List<OrderItem> getItems(){return items;} public int getTotal(){return total;} public int getPage(){return page;} public int getPageSize(){return pageSize;} public int getTotalPages(){return Math.max(1,(int)Math.ceil((double)total/pageSize));} public boolean isHasPrevious(){return page>1;} public boolean isHasNext(){return page<getTotalPages();} public int getStartRow(){return total==0?0:(page-1)*pageSize+1;} public int getEndRow(){return Math.min(page*pageSize,total);} public List<Integer> getVisiblePages(){List<Integer> pages=new ArrayList<>();int start=Math.max(1,page-2),end=Math.min(getTotalPages(),start+4);start=Math.max(1,end-4);for(int i=start;i<=end;i++)pages.add(i);return pages;} }
}
