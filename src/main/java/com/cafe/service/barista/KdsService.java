package com.cafe.service.barista;
import com.cafe.service.shared.BranchMenuService;
import com.cafe.service.shared.OrderService;

import com.cafe.model.KdsTicket;
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

    /** Nguyên liệu trong công thức của món — dựng danh sách chọn ở modal "Hết nguyên liệu". */
    public java.util.List<com.cafe.model.ProductRecipe> getRecipeIngredients(int productId) throws SQLException {
        return orderService.getRecipeIngredients(productId);
    }

    public boolean remakeItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderService.remakeItem(orderItemId, reason, userId, branchId);
    }

    /** Đánh dấu hết món (86) — khoá sản phẩm khỏi POS + QR ở chi nhánh; ghi audit ai bật 86. */
    public void set86(int branchId, int productId, boolean is86, java.time.LocalDateTime backInEta, Integer userId) throws SQLException {
        branchMenuService.set86(branchId, productId, is86, backInEta, userId);
    }
}
