package com.cafe.service.barista;

/**
 * Data for a single render of the Barista Counter screen — read-only, built once and pushed
 * straight to the JSP.
 *
 * <p>Stats are counted by CUP COUNT ({@code *Count}) since that's the real making workload; the
 * open order count is just supplementary info. The four status counts are computed over the
 * WHOLE queue, not just the page being viewed, so changing the filter or flipping pages doesn't
 * make them jump around.
 *
 * <p>Deliberately NOT converted to a {@code record}: the project runs on Jakarta EE 9 (Servlet
 * 5.0 / JSP 3.0), i.e. EL 4.0, and EL only reads record-style accessors from EL 6.0 onward.
 * Switching to a record would break every {@code ${board.waitingCount}} in the JSPs.
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
