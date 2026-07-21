package com.cafe.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nhóm pha theo lượt bàn (dine-in) hoặc theo đơn (mang đi/giao), xếp theo lượt gọi sớm nhất. */
public class BrewGroup {
    private final String key;
    private final List<OrderItem> items = new ArrayList<>();
    private LocalDateTime earliestCreatedAt;

    private BrewGroup(String key) { this.key = key; }

    public String getKey() { return key; }
    public List<OrderItem> getItems() { return items; }

    private void add(OrderItem item) {
        items.add(item);
        LocalDateTime createdAt = item.getOrderCreatedAt();
        if (createdAt != null && (earliestCreatedAt == null || createdAt.isBefore(earliestCreatedAt))) {
            earliestCreatedAt = createdAt;
        }
    }

    public OrderItem getHead() { return items.get(0); }
    public boolean isDineIn() { return "DINE_IN".equals(getHead().getOrderType()); }
    public String getTableNumber() { return getHead().getTableNumber(); }
    public String getPickupCode() { return getHead().getPickupCode(); }
    public String getOrderTypeLabel() { return getHead().getOrderTypeLabel(); }
    public int getOrderId() { return getHead().getOrderId(); }

    public int getCups() { return cupsOf(null); }
    public int getWaitingCups() { return cupsOf("WAITING"); }
    public int getMakingCups() { return cupsOf("MAKING"); }
    public int getReadyCups() { return cupsOf("READY"); }

    private int cupsOf(String status) {
        int total = 0;
        for (OrderItem item : items) {
            if (status == null || status.equals(item.getStatus())) total += item.getQuantity();
        }
        return total;
    }

    /**
     * Gom input đã sắp FIFO. LinkedHashMap giữ thứ tự xuất hiện đầu tiên, tức MIN(CreatedAt) của nhóm.
     * Item trong từng nhóm giữ nguyên thứ tự FIFO của input.
     */
    public static List<BrewGroup> from(List<OrderItem> fifoItems) {
        Map<String, BrewGroup> groups = new LinkedHashMap<>();
        for (OrderItem item : fifoItems) {
            groups.computeIfAbsent(item.getBrewGroupKey(), BrewGroup::new).add(item);
        }
        return new ArrayList<>(groups.values());
    }
}
