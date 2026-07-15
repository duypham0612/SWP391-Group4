package com.cafe.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO màn "Món sẵn lấy" (B2): gom theo đơn/bàn.
 * Giữ riêng món READY (để giao) và TOÀN BỘ món của đơn kèm trạng thái
 * để barista đối chiếu đủ/đúng trước khi bàn giao.
 */
public class PickupTicket {
    private final int orderId;
    private final String tableNumber;            // null/blank → đem về
    private final List<OrderItem> readyItems;    // các món READY (actionable)
    private final List<OrderItem> items;         // toàn bộ đơn (bỏ CANCELLED) để kiểm tra
    private final int readyCount;
    private final int makingCount;
    private final int waitingCount;
    private final boolean allReady;              // không còn món đang chờ/đang pha

    public PickupTicket(int orderId, List<OrderItem> readyItems, Order fullOrder) {
        this.orderId = orderId;
        this.readyItems = readyItems == null ? new ArrayList<>() : readyItems;
        String table = (!this.readyItems.isEmpty()) ? this.readyItems.get(0).getTableNumber()
                : (fullOrder != null ? fullOrder.getTableNumber() : null);
        this.tableNumber = table;

        List<OrderItem> all = new ArrayList<>();
        int r = 0, m = 0, w = 0;
        if (fullOrder != null && fullOrder.getItems() != null) {
            for (OrderItem it : fullOrder.getItems()) {
                String s = it.getStatus();
                if ("CANCELLED".equals(s)) continue;
                all.add(it);
                if ("READY".equals(s)) r++;
                else if ("MAKING".equals(s)) m++;
                else if ("WAITING".equals(s)) w++;
            }
        }
        this.items = all;
        this.readyCount = r;
        this.makingCount = m;
        this.waitingCount = w;
        this.allReady = (m == 0 && w == 0);
    }

    public int getOrderId() { return orderId; }
    public String getTableNumber() { return tableNumber; }
    public List<OrderItem> getReadyItems() { return readyItems; }
    public List<OrderItem> getItems() { return items; }
    public int getReadyCount() { return readyCount; }
    public int getMakingCount() { return makingCount; }
    public int getWaitingCount() { return waitingCount; }
    /** Số món chưa pha xong (chờ + đang pha) — để hiển thị cảnh báo giao thiếu. */
    public int getPendingCount() { return makingCount + waitingCount; }
    public boolean isAllReady() { return allReady; }
}
