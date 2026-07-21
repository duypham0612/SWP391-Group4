package com.cafe.service.barista;
import com.cafe.service.shared.BranchMenuService;
import com.cafe.service.shared.OrderService;

import com.cafe.model.KdsTicket;
import com.cafe.model.BrewGroup;
import com.cafe.model.OrderItem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** B1/B2 · KdsService — màn bếp (Barista). Uỷ thác OrderService; auto-deduct nằm ở markReady. */
public class KdsService {

    private final OrderService orderService = new OrderService();
    private final BranchMenuService branchMenuService = new BranchMenuService();

    public List<OrderItem> getQueue(int branchId) throws SQLException { return orderService.getKdsQueue(branchId); }

    /**
     * Board cũ hai cột, giữ để tương thích dashboard/test. Quầy mới dùng getWorkbenchBoard.
     * Mỗi cột giữ thứ tự FIFO (getQueue đã sắp theo giờ vào đơn). Đơn có cả 2 loại món sẽ
     * xuất hiện ở cả hai cột (mỗi cột chỉ chứa món đúng trạng thái của nó).
     * Trả Map với khoá "waiting" và "making".
     */
    public Map<String, List<KdsTicket>> getQueueBoard(int branchId) throws SQLException {
        Map<Integer, KdsTicket> waiting = new LinkedHashMap<>();
        Map<Integer, KdsTicket> making = new LinkedHashMap<>();
        Map<Integer, Integer> pendingByOrder = new LinkedHashMap<>();   // tổng WAITING+MAKING của mỗi đơn
        for (OrderItem item : getQueue(branchId)) {
            pendingByOrder.merge(item.getOrderId(), 1, Integer::sum);
            Map<Integer, KdsTicket> target = "MAKING".equals(item.getStatus()) ? making : waiting;
            KdsTicket ticket = target.get(item.getOrderId());
            if (ticket == null) target.put(item.getOrderId(), new KdsTicket(item));
            else ticket.addItem(item);
        }
        // Cùng orderId có thể có ticket ở CẢ hai cột → gắn tổng pending cả đơn cho mỗi ticket,
        // để nút "Xong cả đơn" hiển thị đúng số món sẽ bị ảnh hưởng (gồm cả cột còn lại).
        for (KdsTicket t : waiting.values()) t.setOrderPendingCount(pendingByOrder.getOrDefault(t.getOrderId(), t.getItemCount()));
        for (KdsTicket t : making.values())  t.setOrderPendingCount(pendingByOrder.getOrDefault(t.getOrderId(), t.getItemCount()));
        Map<String, List<KdsTicket>> board = new LinkedHashMap<>();
        board.put("waiting", new ArrayList<>(waiting.values()));
        board.put("making", new ArrayList<>(making.values()));
        return board;
    }

    /**
     * Board quầy pha chế, mỗi phần tử là một dòng món độc lập.
     * "blocked" tách riêng khỏi luồng pha: món không pha được thì không nên nằm chung hàng chờ
     * (barista khác sẽ bấm Nhận pha rồi vấp lại đúng vấn đề đó) và cũng không tính vào SLA.
     */
    public Map<String, List<OrderItem>> getWorkbenchBoard(int branchId) throws SQLException {
        return getWorkbenchBoard(branchId, null);
    }

    /** Board của ngày kinh doanh hiện tại; null = không cắt theo ngày (giữ tương thích). */
    public Map<String, List<OrderItem>> getWorkbenchBoard(int branchId,
                                                          java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        return splitWorkbench(orderService.getBaristaWorkbench(branchId, businessDayStartUtc));
    }

    /** Món dang dở từ ngày kinh doanh trước — khu "Đơn treo cần xử lý". */
    public List<OrderItem> getStaleItems(int branchId, java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        return orderService.getStaleItems(branchId, businessDayStartUtc);
    }

    /** Giờ mở cửa chi nhánh — mốc cắt ngày kinh doanh. Chưa khai thì cắt theo nửa đêm. */
    public java.time.LocalTime getBranchOpenTime(int branchId) throws SQLException {
        com.cafe.model.Branch b = getBranch(branchId);
        return b == null ? null : b.getOpenTime();
    }

    /** Chi nhánh — nạp một lần cho cả board. */
    public com.cafe.model.Branch getBranch(int branchId) throws SQLException {
        try (java.sql.Connection conn = com.cafe.config.DBConnection.getConnection()) {
            return new com.cafe.dao.admin.BranchDao().findById(conn, branchId);
        }
    }

    /** Nhóm pha đang vận hành (WAITING/MAKING/READY), đã bỏ BLOCKED ra drawer riêng. */
    public List<BrewGroup> getWorkbenchGroups(int branchId, java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        return groupWorkbenchItems(orderService.getBaristaWorkbench(branchId, businessDayStartUtc));
    }

    /** Món BLOCKED riêng cho drawer "Cần xử lý". */
    public List<OrderItem> getBlockedItems(int branchId, java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        List<OrderItem> blocked = new ArrayList<>();
        for (OrderItem item : orderService.getBaristaWorkbench(branchId, businessDayStartUtc)) {
            if ("BLOCKED".equals(item.getStatus())) blocked.add(item);
        }
        return blocked;
    }

    /**
     * Snapshot nhất quán cho một lần render board: chỉ đọc workbench MỘT lần rồi tách
     * nhóm vận hành và drawer BLOCKED từ đúng cùng dữ liệu đó.
     */
    public WorkbenchSnapshot getWorkbenchSnapshot(int branchId, java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        return snapshotOf(orderService.getBaristaWorkbench(branchId, businessDayStartUtc));
    }

    static WorkbenchSnapshot snapshotOf(List<OrderItem> items) {
        List<OrderItem> active = new ArrayList<>();
        List<OrderItem> blocked = new ArrayList<>();
        for (OrderItem item : items) {
            if ("BLOCKED".equals(item.getStatus())) blocked.add(item);
            else active.add(item);
        }
        return new WorkbenchSnapshot(BrewGroup.from(active), blocked);
    }

    /** Tách BLOCKED trước khi gom: đây là drawer cảnh báo, không phải khối pha vận hành. */
    static List<BrewGroup> groupWorkbenchItems(List<OrderItem> items) {
        return snapshotOf(items).getGroups();
    }

    public static final class WorkbenchSnapshot {
        private final List<BrewGroup> groups;
        private final List<OrderItem> blockedItems;

        private WorkbenchSnapshot(List<BrewGroup> groups, List<OrderItem> blockedItems) {
            this.groups = groups;
            this.blockedItems = blockedItems;
        }

        public List<BrewGroup> getGroups() { return groups; }
        public List<OrderItem> getBlockedItems() { return blockedItems; }
    }

    /** Phân giỏ thuần theo trạng thái — tách khỏi truy vấn DB để test được. */
    static Map<String, List<OrderItem>> splitWorkbench(List<OrderItem> items) {
        Map<String, List<OrderItem>> board = new LinkedHashMap<>();
        board.put("waiting", new ArrayList<>());
        board.put("inProgress", new ArrayList<>());
        board.put("ready", new ArrayList<>());
        board.put("blocked", new ArrayList<>());
        for (OrderItem item : items) {
            if ("WAITING".equals(item.getStatus())) board.get("waiting").add(item);
            else if ("MAKING".equals(item.getStatus())) board.get("inProgress").add(item);
            else if ("READY".equals(item.getStatus())) board.get("ready").add(item);
            else if ("BLOCKED".equals(item.getStatus())) board.get("blocked").add(item);
        }
        return board;
    }

    /** Món READY của chi nhánh — dùng cho thẻ tóm tắt "Sẵn giao" ở dashboard barista. */
    public List<OrderItem> getReadyItems(int branchId) throws SQLException { return orderService.getReadyItems(branchId); }

    public boolean startItem(int orderItemId, Integer userId, int branchId) throws SQLException { return orderService.startItem(orderItemId, userId, branchId); }
    public void bump(int orderItemId, int branchId) throws SQLException { orderService.bumpItem(orderItemId, branchId); }
    public boolean markReady(int orderItemId, Integer userId, int branchId) throws SQLException { return orderService.markItemReady(orderItemId, userId, branchId); }
    public boolean markReady(int orderItemId, Integer userId, int branchId, String handoverLocation) throws SQLException { return orderService.markItemReady(orderItemId, userId, branchId, handoverLocation); }

    public boolean returnToQueue(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.returnItemToQueue(orderItemId, userId, branchId);
    }

    public boolean reportIssue(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderService.reportItemIssue(orderItemId, reason, userId, branchId);
    }

    /** Món không pha được (hỏng máy, ngừng bán) → BLOCKED, rời hàng chờ. */
    public boolean blockItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderService.blockItem(orderItemId, reason, userId, branchId);
    }

    /** Hết nguyên liệu → kiểm kê nguyên liệu về 0 qua sổ cái + chặn món, trong cùng một transaction. */
    public boolean blockItemForDepletedIngredients(int orderItemId, java.util.List<Integer> ingredientIds,
                                                   String reason, Integer userId, int branchId) throws SQLException {
        return orderService.blockItemForDepletedIngredients(orderItemId, ingredientIds, reason, userId, branchId);
    }

    /** BLOCKED → WAITING khi nguyên liệu/máy đã có lại. */
    public boolean unblockItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.unblockItem(orderItemId, userId, branchId);
    }

    /** BLOCKED → WAITING kèm kiểm kê nhanh tồn thật cho các nguyên liệu vừa có lại. */
    public OrderService.UnblockResult unblockItem(int orderItemId,
                                                  java.util.List<com.cafe.model.StockAdjustment> recounts,
                                                  Integer userId, int branchId) throws SQLException {
        return orderService.unblockItem(orderItemId, recounts, userId, branchId);
    }

    /** Nguyên liệu trong công thức của món — dựng danh sách chọn ở modal "Hết nguyên liệu". */
    public java.util.List<com.cafe.model.ProductRecipe> getRecipeIngredients(int productId) throws SQLException {
        return orderService.getRecipeIngredients(productId);
    }

    /** Nguyên liệu trong công thức đang cạn tại chi nhánh — dựng modal kiểm kê khi bỏ chặn. */
    public java.util.List<com.cafe.model.ProductRecipe> getDepletedRecipeIngredients(int branchId, int productId)
            throws SQLException {
        return orderService.getDepletedRecipeIngredients(branchId, productId);
    }

    public boolean remakeItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderService.remakeItem(orderItemId, reason, userId, branchId);
    }

    /** Đánh dấu hết món (86) — khoá sản phẩm khỏi POS + QR ở chi nhánh; ghi audit ai bật 86. */
    public void set86(int branchId, int productId, boolean is86, java.time.LocalDateTime backInEta, Integer userId) throws SQLException {
        branchMenuService.set86(branchId, productId, is86, backInEta, userId);
    }
}
