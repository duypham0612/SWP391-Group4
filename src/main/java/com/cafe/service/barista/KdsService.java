package com.cafe.service.barista;
import com.cafe.service.shared.OrderService;

import com.cafe.model.OrderItem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** B1/B2 · KdsService — màn bếp (Barista). Uỷ thác OrderService; auto-deduct nằm ở markReady. */
public class KdsService {

    private final OrderService orderService = new OrderService();

    public List<OrderItem> getQueue(int branchId) throws SQLException { return orderService.getKdsQueue(branchId); }

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
        return splitWorkbench(getWorkbenchQueue(branchId, businessDayStartUtc));
    }

    /**
     * Hàng chờ PHẲNG theo đúng thứ tự pha mà truy vấn trả về: món làm lại lên đầu, phần còn lại
     * FIFO theo giờ vào đơn. Màn quầy pha chế dựng danh sách một cột từ đây; các con số thống kê
     * vẫn phân giỏ lại bằng {@link #splitWorkbench} trên chính danh sách này, không truy vấn lại.
     */
    public List<OrderItem> getWorkbenchQueue(int branchId, java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        return orderService.getBaristaWorkbench(branchId, businessDayStartUtc);
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

    /** Chi nhánh (giờ mở cửa + ngưỡng cao điểm) — nạp một lần cho cả board. */
    public com.cafe.model.Branch getBranch(int branchId) throws SQLException {
        try (java.sql.Connection conn = com.cafe.config.DBConnection.getConnection()) {
            return new com.cafe.dao.admin.BranchDao().findById(conn, branchId);
        }
    }

    /**
     * Cao điểm khi số ly đang chờ+đang pha chạm ngưỡng của chi nhánh (0 = dùng mặc định).
     * Ở cao điểm, mọi card đều "trễ" nếu tính theo đồng hồ chờ song song — nên bảng chuyển
     * sang xếp thứ tự pha thay vì tô đỏ hàng loạt (số ly đỏ chỉ đo lượng khách, không đo năng lực).
     */
    public static boolean isPeak(int queueCups, int branchThresholdCups) {
        int threshold = branchThresholdCups > 0
                ? branchThresholdCups : com.cafe.common.Constants.PEAK_THRESHOLD_CUPS;
        return queueCups >= threshold;
    }

    /**
     * Ước tính ly cuối hàng còn phải đợi bao lâu: tổng giây pha của mọi ly đang chờ+đang pha
     * chia cho số barista đang thực sự pha (tối thiểu 1). Cố ý bỏ qua phần đã pha dở của món
     * MAKING → ước hơi cao còn hơn hứa hão với khách.
     */
    public static int estimateLastWaitSeconds(int totalPrepSeconds, int baristaCount) {
        return Math.max(0, totalPrepSeconds) / Math.max(1, baristaCount);
    }

    /** Phân giỏ thuần theo trạng thái — tách khỏi truy vấn DB để test được. */
    public static Map<String, List<OrderItem>> splitWorkbench(List<OrderItem> items) {
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
            if ("BLOCKED".equals(item.getStatus())
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
            Integer holder = "MAKING".equals(status) ? item.getBaristaId()
                    : "READY".equals(status) ? item.getPreparedBy() : null;
            return currentUserId != null && currentUserId.equals(holder);
        }
        if ("unassigned".equals(owner)) return "WAITING".equals(status);
        return true;
    }

    /**
     * Cắt trang trên danh sách ĐÃ sắp thứ tự pha và ĐÃ đánh số thứ tự — số trên dòng vì thế là
     * vị trí thật trong cả hàng chờ, không bị đánh lại theo từng trang. Số trang ngoài phạm vi
     * được kéo về biên: sau mỗi thao tác hàng chờ ngắn đi, trang đang xem có thể không còn nữa.
     */
    public static QueuePage paginate(List<OrderItem> items, int page, int pageSize) {
        return new QueuePage(items, page, pageSize);
    }

    /** Một trang của hàng chờ — thuần phép cắt danh sách, không truy vấn lại DB. */
    public static class QueuePage {
        private final List<OrderItem> items;
        private final int total;
        private final int page;
        private final int pageSize;

        QueuePage(List<OrderItem> all, int page, int pageSize) {
            this.total = all.size();
            this.pageSize = Math.max(1, pageSize);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / this.pageSize));
            this.page = Math.min(Math.max(1, page), totalPages);
            int from = Math.min((this.page - 1) * this.pageSize, total);
            this.items = new ArrayList<>(all.subList(from, Math.min(from + this.pageSize, total)));
        }

        public List<OrderItem> getItems() { return items; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return Math.max(1, (int) Math.ceil((double) total / pageSize)); }
        public boolean isHasPrevious() { return page > 1; }
        public boolean isHasNext() { return page < getTotalPages(); }
        public int getStartRow() { return total == 0 ? 0 : (page - 1) * pageSize + 1; }
        public int getEndRow() { return Math.min(page * pageSize, total); }

        /** Tối đa 5 số trang quanh trang hiện tại để pager không phình khi hàng chờ dài. */
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

    public boolean startItem(int orderItemId, Integer userId, int branchId) throws SQLException { return orderService.startItem(orderItemId, userId, branchId); }
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
}
