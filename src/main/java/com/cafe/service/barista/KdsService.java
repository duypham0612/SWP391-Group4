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
import com.cafe.service.shared.KdsOrderWorkflowService;
import com.cafe.service.shared.OrderIssueService;
import com.cafe.service.shared.OrderQueryService;

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

public class KdsService {

    private static final int QUEUE_PAGE_SIZE = 12;
    private static final Set<String> OWNER_FILTERS = Set.of("all", "mine", "unassigned");
    private static final Set<String> STATION_FILTERS = Set.of("all", "COFFEE", "TEA", "BLENDER");
    private static final Set<String> ORDER_TYPE_FILTERS = Set.of("all", "DINE_IN", "TAKEAWAY");

    private final OrderQueryService orderQuery;
    private final KdsOrderWorkflowService kdsWorkflow;
    private final OrderIssueService orderIssues;
    private final BranchService branchService;
    private final AttendanceService attendanceService;

    public KdsService() {
        this(new OrderQueryService(), new KdsOrderWorkflowService(), new OrderIssueService(),
                new BranchService(), new AttendanceService());
    }

    KdsService(OrderQueryService orderQuery, KdsOrderWorkflowService kdsWorkflow,
               OrderIssueService orderIssues, BranchService branchService,
               AttendanceService attendanceService) {
        this.orderQuery = Objects.requireNonNull(orderQuery, "orderQuery");
        this.kdsWorkflow = Objects.requireNonNull(kdsWorkflow, "kdsWorkflow");
        this.orderIssues = Objects.requireNonNull(orderIssues, "orderIssues");
        this.branchService = Objects.requireNonNull(branchService, "branchService");
        this.attendanceService = Objects.requireNonNull(attendanceService, "attendanceService");
    }

    public List<OrderItem> getWorkbenchQueue(int branchId, LocalDateTime businessDayStartUtc)
            throws SQLException {
        return orderQuery.getBaristaWorkbench(branchId, businessDayStartUtc);
    }

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

    public static boolean isPeak(int queueCups, int branchThresholdCups) {
        int threshold = branchThresholdCups > 0
                ? branchThresholdCups : Constants.PEAK_THRESHOLD_CUPS;
        return queueCups >= threshold;
    }

    private static boolean is(OrderItem item, OrderItemStatus status) {
        return is(item.getStatus(), status);
    }

    private static boolean is(String status, OrderItemStatus expected) {
        return expected.name().equals(status);
    }

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
            Integer holder = is(status, OrderItemStatus.MAKING) ? item.getBaristaId()
                    : is(status, OrderItemStatus.READY) ? item.getPreparedBy() : null;
            return currentUserId != null && currentUserId.equals(holder);
        }
        if ("unassigned".equals(owner)) return is(status, OrderItemStatus.WAITING);
        return true;
    }

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

    public static QueuePage paginate(List<OrderItem> items, int page, int pageSize) {
        return new QueuePage(items, page, pageSize);
    }

    // EN: Claims 1 item WAITING->MAKING. Called by KdsServlet "start" action. -> KdsOrderWorkflowService.startItem()
    public boolean startItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return kdsWorkflow.startItem(orderItemId, userId, branchId);
    }

    // EN: Completes 1 item MAKING->READY + auto stock deduction. Called by "markReady" action. -> KdsOrderWorkflowService.markItemReady()
    public boolean markReady(int orderItemId, Integer userId, int branchId) throws SQLException {
        return kdsWorkflow.markItemReady(orderItemId, userId, branchId);
    }

    // EN: Claims every WAITING item of an order at once. Called by KdsServlet.startOrder(). -> KdsOrderWorkflowService.startAllInOrder()
    public int startOrder(int orderId, Integer userId, int branchId) throws SQLException {
        return kdsWorkflow.startAllInOrder(orderId, userId, branchId);
    }

    // EN: Completes every item the caller owns in an order. Called by KdsServlet.markOrderReady(). -> KdsOrderWorkflowService.markOrderReady()
    public KdsOrderWorkflowService.BulkReadyResult markOrderReady(int orderId, Integer userId, int branchId)
            throws SQLException {
        return kdsWorkflow.markOrderReady(orderId, userId, branchId);
    }

    // EN: Releases 1 item MAKING->WAITING (owner only). Called by "returnQueue" action. -> KdsOrderWorkflowService.returnItemToQueue()
    public boolean returnToQueue(int orderItemId, Integer userId, int branchId) throws SQLException {
        return kdsWorkflow.returnItemToQueue(orderItemId, userId, branchId);
    }

    // EN: Rescues an item from an off-shift owner back to WAITING. Called by KdsServlet.reclaim(). -> KdsOrderWorkflowService.reclaimItem()
    public boolean reclaimItem(int orderItemId, Integer actorUserId, int branchId, String actorName,
                               Set<Integer> onDutyUserIds) throws SQLException {
        return kdsWorkflow.reclaimItem(orderItemId, actorUserId, branchId, actorName, onDutyUserIds);
    }

    // EN: Non-blocking issue flag, item status unchanged. Called by reportIssue() group C. -> OrderIssueService.reportItemIssue()
    public boolean reportIssue(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderIssues.reportItemIssue(orderItemId, reason, userId, branchId);
    }

    // EN: Blocking issue, item leaves the queue -> BLOCKED. Called by reportIssue() group B. -> OrderIssueService.blockItem()
    public boolean blockItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderIssues.blockItem(orderItemId, reason, userId, branchId);
    }

    // EN: Zeroes chosen ingredients' stock + blocks item, in 1 transaction. Called by reportIssue() group A. -> OrderIssueService.blockItemForDepletedIngredients()
    public boolean blockItemForDepletedIngredients(int orderItemId, List<Integer> ingredientIds,
                                                   String reason, Integer userId, int branchId) throws SQLException {
        return orderIssues.blockItemForDepletedIngredients(orderItemId, ingredientIds, reason, userId, branchId);
    }

    // EN: Simple BLOCKED->WAITING, no stock write. Called by unblock() when recount!=1. -> OrderIssueService.unblockItem(int,...)
    public boolean unblockItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderIssues.unblockItem(orderItemId, userId, branchId);
    }

    // EN: BLOCKED->WAITING + writes real recounted stock. Called by unblock() when recount=1. -> OrderIssueService.unblockItem(int,List,...)
    public OrderIssueService.UnblockResult unblockItem(int orderItemId, List<StockAdjustment> recounts,
                                                       Integer userId, int branchId) throws SQLException {
        return orderIssues.unblockItem(orderItemId, recounts, userId, branchId);
    }

    public List<Recipe> getRecipeIngredients(int productId) throws SQLException {
        return orderQuery.getRecipeIngredients(productId);
    }

    public List<Recipe> getDepletedRecipeIngredients(int branchId, int productId) throws SQLException {
        return orderQuery.getDepletedRecipeIngredients(branchId, productId);
    }

    // EN: Redoes item, back to WAITING with priority + reserved waste. Called by "remake" action. -> OrderIssueService.remakeItem()
    public boolean remakeItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderIssues.remakeItem(orderItemId, reason, userId, branchId);
    }
}
