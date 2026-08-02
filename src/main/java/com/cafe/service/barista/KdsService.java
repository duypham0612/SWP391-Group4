package com.cafe.service.barista;

import com.cafe.common.BusinessDay;
import com.cafe.common.Constants;
import com.cafe.common.OrderItemStatus;
import com.cafe.model.Branch;
import com.cafe.model.OrderGroupInfo;
import com.cafe.model.OrderItem;
import com.cafe.model.Recipe;
import com.cafe.model.StockAdjustment;
import com.cafe.service.admin.BranchService;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.shared.OrderService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** B1/B2 · KdsService — màn bếp (Barista). Uỷ thác OrderService; auto-deduct nằm ở markReady. */
public class KdsService {

    /**
     * Số dòng mỗi trang hàng chờ. Chọn 12 để một trang vừa khít khung hàng chờ trên màn quầy
     * phổ thông mà không phải cuộn — barista liếc một lần là thấy trọn việc của trang.
     */
    private static final int QUEUE_PAGE_SIZE = 12;
    private static final Set<String> OWNER_FILTERS = Set.of("all", "mine", "unassigned");
    private static final Set<String> STATION_FILTERS = Set.of("all", "COFFEE", "TEA", "BLENDER");
    private static final Set<String> ORDER_TYPE_FILTERS = Set.of("all", "DINE_IN", "TAKEAWAY");

    private final OrderService orderService;
    private final BranchService branchService;
    private final AttendanceService attendanceService;

    public KdsService() {
        this(new OrderService(), new BranchService(), new AttendanceService());
    }

    KdsService(OrderService orderService, BranchService branchService, AttendanceService attendanceService) {
        this.orderService = Objects.requireNonNull(orderService, "orderService");
        this.branchService = Objects.requireNonNull(branchService, "branchService");
        this.attendanceService = Objects.requireNonNull(attendanceService, "attendanceService");
    }

    /**
     * Hàng chờ PHẲNG theo đúng thứ tự pha mà truy vấn trả về: món làm lại lên đầu, phần còn lại
     * FIFO theo giờ vào đơn. Màn quầy pha chế dựng danh sách một cột từ đây; các con số thống kê
     * vẫn phân giỏ lại bằng {@link #splitWorkbench} trên chính danh sách này, không truy vấn lại.
     */
    public List<OrderItem> getWorkbenchQueue(int branchId, LocalDateTime businessDayStartUtc)
            throws SQLException {
        return orderService.getBaristaWorkbench(branchId, businessDayStartUtc);
    }

    /** Toàn bộ query/use case của board; Controller chỉ bind KdsBoardQuery và render kết quả. */
    public KdsBoardData loadBoard(int branchId, KdsBoardQuery query) throws SQLException {
        KdsBoardQuery safe = query == null ? new KdsBoardQuery("all", "all", "all", 1, null) : query;
        Branch branch = branchService.getBranch(branchId);
        LocalTime openTime = branch == null ? null : branch.getOpenTime();
        int peakThreshold = branch == null ? 0 : branch.getPeakThresholdCups();
        List<OrderItem> queue = getWorkbenchQueue(branchId, BusinessDay.startUtc(openTime));

        annotateOrderLines(queue, safe.currentUserId());
        Set<Integer> onDuty = attendanceService.getOnDutyUserIds(branchId);
        for (OrderItem item : queue) {
            item.setOwnerOffDuty(is(item, OrderItemStatus.MAKING) && item.getBaristaId() != null
                    && !onDuty.contains(item.getBaristaId()));
        }

        Map<String, List<OrderItem>> buckets = splitWorkbench(queue);
        List<OrderItem> waiting = buckets.get("waiting");
        List<OrderItem> making = buckets.get("inProgress");
        List<OrderItem> ready = buckets.get("ready");
        List<OrderItem> blocked = buckets.get("blocked");
        int waitingCount = cups(waiting);
        int makingCount = cups(making);
        int readyCount = cups(ready);
        int blockedCount = cups(blocked);
        int queueCups = waitingCount + makingCount;

        List<OrderItem> ordered = sortForBrewing(queue);
        int sequence = 1;
        for (OrderItem item : ordered) if (!is(item, OrderItemStatus.READY)) item.setSeqNo(sequence++);
        List<OrderItem> visible = filterWorkbench(ordered, safe.owner(), safe.station(),
                safe.orderType(), safe.currentUserId());
        QueuePage page = paginate(visible, safe.page(), QUEUE_PAGE_SIZE);
        markGroupStarts(page.getItems());
        return new KdsBoardData(isPeak(queueCups, peakThreshold), queueCups, ordered.size(), page,
                safe.owner(), safe.station(), safe.orderType(),
                waitingCount, makingCount, readyCount, blockedCount,
                distinctOrders(waiting, making, ready, blocked), safe.currentUserId());
    }

    public record KdsBoardQuery(String owner, String station, String orderType,
                                int page, Integer currentUserId) {
        public KdsBoardQuery {
            owner = normalize(owner, OWNER_FILTERS);
            station = normalize(station, STATION_FILTERS);
            orderType = normalize(orderType, ORDER_TYPE_FILTERS);
            page = Math.max(1, page);
        }

        private static String normalize(String value, Set<String> allowed) {
            return value != null && allowed.contains(value) ? value : "all";
        }
    }


    /**
     * Thứ tự danh sách một cột: việc còn phải làm (chờ pha · đang pha · cần xử lý) giữ nguyên
     * thứ tự pha do truy vấn trả về (làm lại trước, rồi FIFO theo giờ đặt); món ĐÃ pha xong dồn
     * xuống cuối vì chúng chỉ còn chờ người giao, không phải việc của quầy.
     */
    private static List<OrderItem> sortForBrewing(List<OrderItem> queue) {
        List<OrderItem> out = new ArrayList<>(queue.size());
        for (OrderItem item : queue) if (!is(item, OrderItemStatus.READY)) out.add(item);
        for (OrderItem item : queue) if (is(item, OrderItemStatus.READY)) out.add(item);
        return out;
    }

    @SafeVarargs
    private static int distinctOrders(List<OrderItem>... buckets) {
        Set<Integer> ids = new HashSet<>();
        for (List<OrderItem> bucket : buckets) for (OrderItem item : bucket) ids.add(item.getOrderId());
        return ids.size();
    }

    private static int cups(List<OrderItem> items) {
        int total = 0;
        for (OrderItem item : items) total += item.getQuantity();
        return total;
    }

    /**
     * Cao điểm khi số ly đang chờ+đang pha chạm ngưỡng của chi nhánh (0 = dùng mặc định).
     * Ở cao điểm, mọi card đều "trễ" nếu tính theo đồng hồ chờ song song — nên bảng chuyển
     * sang xếp thứ tự pha thay vì tô đỏ hàng loạt (số ly đỏ chỉ đo lượng khách, không đo năng lực).
     */
    public static boolean isPeak(int queueCups, int branchThresholdCups) {
        int threshold = branchThresholdCups > 0
                ? branchThresholdCups : Constants.PEAK_THRESHOLD_CUPS;
        return queueCups >= threshold;
    }

    /**
     * So khớp trạng thái dòng món qua {@link OrderItemStatus} thay vì rải chuỗi literal khắp file —
     * gõ sai một ký tự trong literal thì compiler không bắt được, còn hằng enum thì có.
     *
     * <p>Trạng thái null trả false, đúng như cách so chuỗi literal trước đây.
     */
    private static boolean is(OrderItem item, OrderItemStatus status) {
        return is(item.getStatus(), status);
    }

    private static boolean is(String status, OrderItemStatus expected) {
        return expected.name().equals(status);
    }

    /** Phân giỏ thuần theo trạng thái — tách khỏi truy vấn DB để test được. */
    public static Map<String, List<OrderItem>> splitWorkbench(List<OrderItem> items) {
        Map<String, List<OrderItem>> board = new LinkedHashMap<>();
        board.put("waiting", new ArrayList<>());
        board.put("inProgress", new ArrayList<>());
        board.put("ready", new ArrayList<>());
        board.put("blocked", new ArrayList<>());
        for (OrderItem item : items) {
            if (is(item, OrderItemStatus.WAITING)) board.get("waiting").add(item);
            else if (is(item, OrderItemStatus.MAKING)) board.get("inProgress").add(item);
            else if (is(item, OrderItemStatus.READY)) board.get("ready").add(item);
            else if (is(item, OrderItemStatus.BLOCKED)) board.get("blocked").add(item);
        }
        return board;
    }

    /**
     * Lọc hàng chờ theo bộ lọc của quầy (người phụ trách · quầy · loại đơn).
     * Món BỊ CHẶN luôn được giữ lại: đó là cảnh báo an toàn, không được để bộ lọc giấu đi.
     * Lọc ở đây (thay vì ẩn dòng bằng JS như trước) để phân trang đếm trên đúng tập đang xem —
     * nếu không, bấm "Món của tôi" ở trang 1 sẽ trống trong khi món nằm ở trang 3.
     */
    public static List<OrderItem> filterWorkbench(List<OrderItem> items, String owner, String station,
                                                  String orderType, Integer currentUserId) {
        List<OrderItem> out = new ArrayList<>(items.size());
        for (OrderItem item : items) {
            if (is(item, OrderItemStatus.BLOCKED)
                    || (ownerMatches(item, owner, currentUserId)
                        && valueMatches(station, item.getStation())
                        && valueMatches(orderType, item.getOrderType()))) {
                out.add(item);
            }
        }
        return out;
    }

    private static boolean valueMatches(String filter, String value) {
        return filter == null || filter.isBlank() || "all".equals(filter) || filter.equals(value);
    }

    private static boolean ownerMatches(OrderItem item, String owner, Integer currentUserId) {
        if (owner == null || owner.isBlank() || "all".equals(owner)) return true;
        String status = item.getStatus();
        if ("mine".equals(owner)) {
            // Món của tôi = tôi đang pha, hoặc chính tôi vừa pha xong.
            Integer holder = is(status, OrderItemStatus.MAKING) ? item.getBaristaId()
                    : is(status, OrderItemStatus.READY) ? item.getPreparedBy() : null;
            return currentUserId != null && currentUserId.equals(holder);
        }
        if ("unassigned".equals(owner)) return is(status, OrderItemStatus.WAITING);
        return true;
    }

    /**
     * Gắn thông tin cấp đơn lên từng dòng: dòng thứ mấy trong đơn, và bộ đếm dùng chung của đơn.
     *
     * <p>Chạy trên hàng chờ ĐẦY ĐỦ, TRƯỚC khi lọc: nếu đếm sau lọc thì nhãn "món 2/3" đổi nghĩa
     * mỗi lần bấm chip lọc, trong khi cái barista cần biết là đơn thật sự có mấy ly.
     *
     * @param currentUserId barista đang đăng nhập — để đếm số món CHÍNH họ đang pha (điều kiện
     *                      hiện nút "Xong cả đơn"); null thì không đếm được món của ai cả.
     */
    public static void annotateOrderLines(List<OrderItem> queue, Integer currentUserId) {
        if (queue == null || queue.isEmpty()) return;
        Map<Integer, OrderGroupInfo> byOrder = new LinkedHashMap<>();
        for (OrderItem item : queue) {
            OrderGroupInfo info = byOrder.computeIfAbsent(item.getOrderId(),
                    id -> new OrderGroupInfo(id, item.getTableNumber(), item.getPickupCode(),
                            item.getOrderType()));
            boolean mine = currentUserId != null && currentUserId.equals(item.getBaristaId());
            info.add(item.getStatus(), mine);
            item.setGroupInfo(info);
            item.setOrderLineNo(info.getLineCount());
        }
    }

    /**
     * Đánh dấu khối trên ĐÚNG danh sách sắp render (sau lọc + cắt trang).
     *
     * <p>Chỉ dựng tiêu đề khi khối có từ 2 dòng LIỀN NHAU trở lên ở chính danh sách này. Một dòng
     * lẻ của đơn nhiều món — ví dụ ly đã pha xong bị dồn xuống đáy, hoặc dòng duy nhất còn sót sau
     * bộ lọc — không được đội tiêu đề riêng: nó đọc như một đơn mới trong khi phần còn lại của đơn
     * nằm ở chỗ khác.
     */
    public static void markGroupStarts(List<OrderItem> items) {
        if (items == null || items.isEmpty()) return;
        int i = 0;
        while (i < items.size()) {
            int end = i + 1;
            while (end < items.size() && items.get(end).getOrderId() == items.get(i).getOrderId()) end++;
            boolean block = (end - i) > 1;
            for (int k = i; k < end; k++) {
                items.get(k).setGroupStart(block && k == i);
                items.get(k).setGroupMember(block);
            }
            i = end;
        }
    }

    /**
     * Cắt trang trên danh sách ĐÃ sắp thứ tự pha và ĐÃ đánh số thứ tự — số trên dòng vì thế là
     * vị trí thật trong cả hàng chờ, không bị đánh lại theo từng trang. Số trang ngoài phạm vi
     * được kéo về biên: sau mỗi thao tác hàng chờ ngắn đi, trang đang xem có thể không còn nữa.
     */
    public static QueuePage paginate(List<OrderItem> items, int page, int pageSize) {
        return new QueuePage(items, page, pageSize);
    }

    /** Nhận pha một món — WAITING → MAKING. */
    public boolean startItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.startItem(orderItemId, userId, branchId);
    }

    /** Pha xong một món — MAKING → READY, kèm trừ tồn tự động theo công thức. */
    public boolean markReady(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.markItemReady(orderItemId, userId, branchId);
    }

    /** Nhận pha mọi món còn chờ của một đơn — đơn nhiều ly thường do một người pha trọn. */
    public int startOrder(int orderId, Integer userId, int branchId) throws SQLException {
        return orderService.startAllInOrder(orderId, userId, branchId);
    }

    /** Hoàn thành mọi món CHÍNH barista này đang pha trong một đơn. */
    public OrderService.BulkReadyResult markOrderReady(int orderId, Integer userId, int branchId) throws SQLException {
        return orderService.markOrderReady(orderId, userId, branchId);
    }

    public boolean returnToQueue(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.returnItemToQueue(orderItemId, userId, branchId);
    }

    /** Thu hồi món đang pha của người đã rời ca — lối gỡ duy nhất ở quầy, nếu không phải nhờ Thu ngân huỷ. */
    public boolean reclaimItem(int orderItemId, Integer actorUserId, int branchId, String actorName,
                               Set<Integer> onDutyUserIds) throws SQLException {
        return orderService.reclaimItem(orderItemId, actorUserId, branchId, actorName, onDutyUserIds);
    }

    public boolean reportIssue(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderService.reportItemIssue(orderItemId, reason, userId, branchId);
    }

    /** Món không pha được (hỏng máy, ngừng bán) → BLOCKED, rời hàng chờ. */
    public boolean blockItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderService.blockItem(orderItemId, reason, userId, branchId);
    }

    /** Hết nguyên liệu → kiểm kê nguyên liệu về 0 qua sổ cái + chặn món, trong cùng một transaction. */
    public boolean blockItemForDepletedIngredients(int orderItemId, List<Integer> ingredientIds,
                                                   String reason, Integer userId, int branchId) throws SQLException {
        return orderService.blockItemForDepletedIngredients(orderItemId, ingredientIds, reason, userId, branchId);
    }

    /** BLOCKED → WAITING khi nguyên liệu/máy đã có lại. */
    public boolean unblockItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.unblockItem(orderItemId, userId, branchId);
    }

    /** BLOCKED → WAITING kèm kiểm kê nhanh tồn thật cho các nguyên liệu vừa có lại. */
    public OrderService.UnblockResult unblockItem(int orderItemId,
                                                  List<StockAdjustment> recounts,
                                                  Integer userId, int branchId) throws SQLException {
        return orderService.unblockItem(orderItemId, recounts, userId, branchId);
    }

    /** Nguyên liệu trong công thức của món — dựng danh sách chọn ở modal "Hết nguyên liệu". */
    public List<Recipe> getRecipeIngredients(int productId) throws SQLException {
        return orderService.getRecipeIngredients(productId);
    }

    /** Nguyên liệu trong công thức đang cạn tại chi nhánh — dựng modal kiểm kê khi bỏ chặn. */
    public List<Recipe> getDepletedRecipeIngredients(int branchId, int productId) throws SQLException {
        return orderService.getDepletedRecipeIngredients(branchId, productId);
    }

    /** Pha lại món (đổ, sai công thức...) — về hàng chờ với ưu tiên lên đầu. */
    public boolean remakeItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderService.remakeItem(orderItemId, reason, userId, branchId);
    }
}
