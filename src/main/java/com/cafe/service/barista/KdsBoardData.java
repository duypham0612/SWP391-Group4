package com.cafe.service.barista;

/**
 * Dữ liệu một lần vẽ màn Quầy pha chế — chỉ đọc, dựng xong là đẩy thẳng ra JSP.
 *
 * <p>Thống kê đếm theo SỐ LY ({@code *Count}) vì đó mới là khối lượng việc pha thật; số đơn đang mở
 * chỉ là thông tin phụ. Bốn con số trạng thái tính trên TOÀN hàng chờ, không phải trên trang đang
 * xem, nên đổi bộ lọc hay lật trang không làm chúng nhảy.
 *
 * <p>Cố ý KHÔNG chuyển sang {@code record}: dự án chạy Jakarta EE 9 (Servlet 5.0 / JSP 3.0) tức
 * EL 4.0, mà EL chỉ đọc được accessor kiểu record từ EL 6.0. Chuyển sang record là gãy toàn bộ
 * {@code ${board.waitingCount}} trong JSP.
 */
public final class KdsBoardData {

    private final boolean peakMode;
    private final int peakQueueCups;
    private final int queueTotal;
    private final QueuePage queuePage;
    private final String filterOwner;
    private final String filterStation;
    private final String filterOrderType;
    private final int waitingCount;
    private final int makingCount;
    private final int readyCount;
    private final int blockedCount;
    private final int openOrderCount;
    private final int currentUserId;

    KdsBoardData(boolean peakMode, int peakQueueCups, int queueTotal, QueuePage queuePage,
                 String filterOwner, String filterStation, String filterOrderType,
                 int waitingCount, int makingCount, int readyCount, int blockedCount,
                 int openOrderCount, Integer currentUserId) {
        this.peakMode = peakMode;
        this.peakQueueCups = peakQueueCups;
        this.queueTotal = queueTotal;
        this.queuePage = queuePage;
        this.filterOwner = filterOwner;
        this.filterStation = filterStation;
        this.filterOrderType = filterOrderType;
        this.waitingCount = waitingCount;
        this.makingCount = makingCount;
        this.readyCount = readyCount;
        this.blockedCount = blockedCount;
        this.openOrderCount = openOrderCount;
        this.currentUserId = currentUserId == null ? 0 : currentUserId;
    }

    public boolean isPeakMode() { return peakMode; }
    public int getPeakQueueCups() { return peakQueueCups; }
    public int getQueueTotal() { return queueTotal; }
    public QueuePage getQueuePage() { return queuePage; }
    public String getFilterOwner() { return filterOwner; }
    public String getFilterStation() { return filterStation; }
    public String getFilterOrderType() { return filterOrderType; }
    public int getWaitingCount() { return waitingCount; }
    public int getMakingCount() { return makingCount; }
    public int getReadyCount() { return readyCount; }
    public int getBlockedCount() { return blockedCount; }
    public int getOpenOrderCount() { return openOrderCount; }
    public int getCurrentUserId() { return currentUserId; }
}
