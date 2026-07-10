package com.cafe.model;

import com.cafe.common.Constants;

import java.util.ArrayList;
import java.util.List;

/** DTO hiển thị KDS: gom các OrderItem cùng một đơn thành một ticket. */
public class KdsTicket {
    private int orderId;
    private String tableNumber;
    private int waitedSeconds;
    private final List<OrderItem> items = new ArrayList<>();

    public KdsTicket(OrderItem firstItem) {
        this.orderId = firstItem.getOrderId();
        this.tableNumber = firstItem.getTableNumber();
        this.waitedSeconds = firstItem.getWaitedSeconds();
        addItem(firstItem);
    }

    public int getOrderId() { return orderId; }

    public String getTableNumber() { return tableNumber; }

    public int getWaitedSeconds() { return waitedSeconds; }

    public String getWaitedDisplay() { return OrderItem.formatDuration(waitedSeconds); }

    public String getAgeTier() {
        if (waitedSeconds >= Constants.KDS_CRIT_SECONDS) return "crit";
        if (waitedSeconds >= Constants.KDS_WARN_SECONDS) return "warn";
        return "ok";
    }

    public List<OrderItem> getItems() { return items; }

    public int getItemCount() { return items.size(); }

    public void addItem(OrderItem item) {
        items.add(item);
        waitedSeconds = Math.max(waitedSeconds, item.getWaitedSeconds());
        if (tableNumber == null || tableNumber.isBlank()) tableNumber = item.getTableNumber();
    }
}
