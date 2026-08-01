package com.cafe.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Nhóm món nhân viên đã nhận khỏi quầy và đang mang tới cùng một bàn. */
public class PickedUpGroup {
    private final String tableNumber;
    private final List<OrderItem> items = new ArrayList<>();
    private final Set<Integer> orderIds = new LinkedHashSet<>();
    private final Set<String> pickupCodes = new LinkedHashSet<>();

    public PickedUpGroup(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void add(OrderItem item) {
        if (item == null) return;
        items.add(item);
        orderIds.add(item.getOrderId());
        if (item.getPickupCode() != null && !item.getPickupCode().isBlank()) {
            pickupCodes.add(item.getPickupCode());
        }
    }

    public String getTableNumber() { return tableNumber; }
    public List<OrderItem> getItems() { return items; }
    public List<Integer> getOrderIds() { return new ArrayList<>(orderIds); }
    public Set<String> getPickupCodes() { return new LinkedHashSet<>(pickupCodes); }
    public int getOrderCount() { return orderIds.size(); }
    public int getItemCount() { return items.size(); }
    public int getCupCount() {
        int total = 0;
        for (OrderItem item : items) total += item.getQuantity();
        return total;
    }

    /** Nút giao tất cả chỉ xuất hiện khi thật sự có nhiều dòng món cùng một bàn. */
    public boolean isCanServeAll() {
        return tableNumber != null && !tableNumber.isBlank() && items.size() > 1;
    }
}
