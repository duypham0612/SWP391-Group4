package com.cafe.controller.barista;

import com.cafe.model.OrderItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gom món đang mở của Quầy pha chế theo BÀN, phục vụ layout master–detail (danh sách bàn bên
 * trái, chi tiết một bàn bên phải).
 *
 * <p>Trục nhìn của barista đổi từ "món nào ở cột nào" sang "bàn nào cần gì": mỗi bàn là một dòng
 * ở master list, click vào xem trọn 4 mục con (Cần xử lý / Chờ pha / Đang pha / Sẵn sàng) của
 * riêng bàn đó. Card giữ nguyên fragment cũ nên mọi thao tác pha chế vẫn chạy.
 *
 * <p>Khoá gom: đơn tại bàn gom theo số bàn (nhiều đơn cùng bàn dồn về một dòng — đúng ý "1 bàn
 * cần gì"); đơn mang đi/giao không có bàn thì gom theo từng đơn để không dồn hết vào một "bàn" ảo.
 * Chỉ chứa món của ngày kinh doanh hiện tại — đơn treo qua đêm đã được lọc khỏi board từ trước.
 */
public class TableGroup {

    private final String key;               // khoá ổn định cho data-table-key + localStorage
    private final String label;             // nhãn hiển thị: "Bàn 5" hoặc "Mang đi · Đơn #123"
    private final String orderTypeLabel;    // loại đơn của dòng đầu (phụ đề)
    private final String pickupCode;        // mã gọi món của đơn đầu (nếu có) để đối chiếu khi giao

    private final List<OrderItem> waiting = new ArrayList<>();
    private final List<OrderItem> making = new ArrayList<>();
    private final List<OrderItem> ready = new ArrayList<>();
    private final List<OrderItem> blocked = new ArrayList<>();

    private TableGroup(String key, String label, String orderTypeLabel, String pickupCode) {
        this.key = key;
        this.label = label;
        this.orderTypeLabel = orderTypeLabel;
        this.pickupCode = pickupCode;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getOrderTypeLabel() { return orderTypeLabel; }
    public String getPickupCode() { return pickupCode; }

    public List<OrderItem> getWaiting() { return waiting; }
    public List<OrderItem> getMaking() { return making; }
    public List<OrderItem> getReady() { return ready; }
    public List<OrderItem> getBlocked() { return blocked; }

    // Gộp theo món (batching): dồn các ly TRÙNG sản phẩm trong cùng một trạng thái thành một nhóm,
    // để barista pha một lượt (vd 5 Cold Brew). Vẫn giữ từng ly riêng để bấm xong theo đơn.
    public List<ProductBatch> getWaitingBatches() { return batchesOf(waiting); }
    public List<ProductBatch> getMakingBatches() { return batchesOf(making); }
    public List<ProductBatch> getReadyBatches() { return batchesOf(ready); }
    public List<ProductBatch> getBlockedBatches() { return batchesOf(blocked); }

    private static List<ProductBatch> batchesOf(List<OrderItem> items) {
        Map<Integer, ProductBatch> byProduct = new LinkedHashMap<>();
        for (OrderItem it : items) {
            byProduct.computeIfAbsent(it.getProductId(), k -> new ProductBatch(it.getProductName())).add(it);
        }
        return new ArrayList<>(byProduct.values());
    }

    /** Một nhóm các ly cùng sản phẩm trong cùng trạng thái. Thứ tự ly giữ theo bucket (FIFO/ưu tiên). */
    public static class ProductBatch {
        private final String productName;
        private final List<OrderItem> items = new ArrayList<>();

        ProductBatch(String productName) { this.productName = productName; }
        void add(OrderItem it) { items.add(it); }

        public String getProductName() { return productName; }
        public List<OrderItem> getItems() { return items; }
        public int getTotalQty() { return cups(items); }
        public int getOrderCount() {
            java.util.Set<Integer> ids = new java.util.HashSet<>();
            for (OrderItem it : items) ids.add(it.getOrderId());
            return ids.size();
        }
    }

    public int getWaitingCups() { return cups(waiting); }
    public int getMakingCups() { return cups(making); }
    public int getReadyCups() { return cups(ready); }
    public int getBlockedCups() { return cups(blocked); }

    /** Số ly còn việc để làm (chưa pha xong): dùng cho con số tổng ở dòng bàn. Ready đã xong nên không tính. */
    public int getOpenCups() { return getWaitingCups() + getMakingCups() + getBlockedCups(); }

    /** Bàn đã xong: không còn món để pha/xử lý (chỉ còn món đã pha chờ giao) → làm mờ + đẩy xuống cuối. */
    public boolean isDone() { return getOpenCups() == 0; }

    /** Số đơn khác nhau trong nhóm — nhóm "Mang đi" gộp nhiều đơn nên cần cho biết bao nhiêu đơn. */
    public int getOrderCount() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (OrderItem it : waiting) ids.add(it.getOrderId());
        for (OrderItem it : making) ids.add(it.getOrderId());
        for (OrderItem it : ready) ids.add(it.getOrderId());
        for (OrderItem it : blocked) ids.add(it.getOrderId());
        return ids.size();
    }

    public boolean isHasBlocked() { return !blocked.isEmpty(); }

    public boolean isHasLate() {
        for (OrderItem it : waiting) if ("late".equals(it.getSlaTier())) return true;
        for (OrderItem it : making) if ("late".equals(it.getSlaTier())) return true;
        return false;
    }

    /**
     * Bậc khẩn cấp của bàn để tô badge và sắp thứ tự: có món chặn hoặc trễ → "late" (đỏ);
     * có món sắp trễ → "warn" (vàng); còn lại "ok". Bàn chỉ còn món Sẵn sàng cũng là "ok".
     */
    public String getBadgeTier() {
        int rank = severity();
        if (rank >= 3) return "late";
        if (rank == 2) return "warn";
        return "ok";
    }

    /** Điểm khẩn cấp: chặn/trễ = 3, sắp trễ = 2, bình thường = 1. */
    private int severity() {
        int rank = isHasBlocked() ? 3 : 1;
        for (OrderItem it : waiting) rank = Math.max(rank, tierRank(it.getSlaTier()));
        for (OrderItem it : making) rank = Math.max(rank, tierRank(it.getSlaTier()));
        return rank;
    }

    private static int tierRank(String tier) {
        if ("blocked".equals(tier) || "late".equals(tier)) return 3;
        if ("warn".equals(tier)) return 2;
        return 1;
    }

    /** Chờ lâu nhất trong bàn — tiebreak khi cùng bậc khẩn cấp. */
    private int maxWaitedSeconds() {
        int max = 0;
        for (OrderItem it : waiting) max = Math.max(max, it.getWaitedSeconds());
        for (OrderItem it : making) max = Math.max(max, it.getWaitedSeconds());
        return max;
    }

    private static int cups(List<OrderItem> items) {
        int total = 0;
        for (OrderItem it : items) total += it.getQuantity();
        return total;
    }

    /**
     * Gom 4 rổ trạng thái thành danh sách bàn, bàn khẩn cấp nhất lên đầu.
     * Thứ tự món trong mỗi rổ giữ nguyên thứ tự service trả về (đã sắp theo thời gian).
     */
    public static List<TableGroup> from(List<OrderItem> waiting, List<OrderItem> inProgress,
                                        List<OrderItem> ready, List<OrderItem> blocked) {
        Map<String, TableGroup> byTable = new LinkedHashMap<>();
        addAll(byTable, waiting, Bucket.WAITING);
        addAll(byTable, inProgress, Bucket.MAKING);
        addAll(byTable, ready, Bucket.READY);
        addAll(byTable, blocked, Bucket.BLOCKED);

        List<TableGroup> groups = new ArrayList<>(byTable.values());
        // Bàn còn việc lên trước, bàn đã xong (chỉ còn chờ giao) xuống cuối; trong nhóm còn việc:
        // khẩn cấp trước (severity giảm dần), cùng bậc thì bàn chờ lâu nhất lên trên.
        groups.sort(Comparator.comparing(TableGroup::isDone)
                .thenComparing(Comparator.comparingInt(TableGroup::severity).reversed())
                .thenComparing(Comparator.comparingInt(TableGroup::maxWaitedSeconds).reversed()));
        return groups;
    }

    private enum Bucket { WAITING, MAKING, READY, BLOCKED }

    private static void addAll(Map<String, TableGroup> byTable, List<OrderItem> items, Bucket bucket) {
        if (items == null) return;
        for (OrderItem it : items) {
            TableGroup g = byTable.computeIfAbsent(keyOf(it), k -> new TableGroup(
                    k, labelOf(it), it.getOrderTypeLabel(), it.getPickupCode()));
            switch (bucket) {
                case WAITING -> g.waiting.add(it);
                case MAKING -> g.making.add(it);
                case READY -> g.ready.add(it);
                case BLOCKED -> g.blocked.add(it);
            }
        }
    }

    /** Đơn có bàn gom theo bàn; không có bàn (mang đi/giao) gom TẤT CẢ theo loại đơn vào một nhóm
     *  (vd mọi đơn "Mang đi" chung một dòng) để danh sách không phình khi nhiều đơn lẻ. */
    private static String keyOf(OrderItem it) {
        String table = it.getTableNumber();
        return (table == null || table.isBlank())
                ? "type:" + it.getOrderType()
                : "table:" + table.trim();
    }

    private static String labelOf(OrderItem it) {
        String table = it.getTableNumber();
        return (table == null || table.isBlank())
                ? it.getOrderTypeLabel()
                : table.trim();
    }
}
