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
    private final String sessionStatus;          // OPEN/CLOSED của phiên bàn (CLOSED = khách đã thanh toán)

    /**
     * Dựng ticket từ danh sách món đã nạp sẵn (1 connection ở OrderService.getPickupTickets).
     * {@code allOpenItems} là TOÀN BỘ món của đơn (mọi trạng thái) để render checklist đối chiếu.
     */
    public PickupTicket(int orderId, String tableNumber, List<OrderItem> readyItems, List<OrderItem> allOpenItems) {
        this.orderId = orderId;
        this.readyItems = readyItems == null ? new ArrayList<>() : readyItems;
        this.tableNumber = tableNumber;

        List<OrderItem> all = new ArrayList<>();
        int r = 0, m = 0, w = 0;
        String sess = null;
        if (allOpenItems != null) {
            for (OrderItem it : allOpenItems) {
                if (sess == null) sess = it.getSessionStatus();
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
        this.sessionStatus = sess;
    }

    public int getOrderId() { return orderId; }
    public String getTableNumber() { return tableNumber; }
    /** Mã gọi món của đơn (lấy từ dòng món đầu tiên) — để thu ngân đọc số gọi khách. */
    public String getPickupCode() {
        if (!readyItems.isEmpty()) return readyItems.get(0).getPickupCode();
        return items.isEmpty() ? null : items.get(0).getPickupCode();
    }
    public List<OrderItem> getReadyItems() { return readyItems; }
    public List<OrderItem> getItems() { return items; }
    public int getReadyCount() { return readyCount; }
    public int getReadyCupCount() { int n=0; for (OrderItem it : readyItems) n+=it.getQuantity(); return n; }
    public int getMakingCount() { return makingCount; }
    public int getWaitingCount() { return waitingCount; }
    /** Số món chưa pha xong (chờ + đang pha) — để hiển thị cảnh báo giao thiếu. */
    public int getPendingCount() { return makingCount + waitingCount; }
    public int getPendingCupCount() { int n=0; for (OrderItem it : items) if ("WAITING".equals(it.getStatus()) || "MAKING".equals(it.getStatus())) n+=it.getQuantity(); return n; }
    public boolean isAllReady() { return allReady; }
    public String getSessionStatus() { return sessionStatus; }
    /** Khách đã thanh toán (phiên bàn đã đóng) → ưu tiên giao ngay. */
    public boolean isPaid() { return "CLOSED".equals(sessionStatus); }
}
