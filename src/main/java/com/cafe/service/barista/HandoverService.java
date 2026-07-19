package com.cafe.service.barista;

import com.cafe.config.DBConnection;
import com.cafe.dao.manager.ShiftHandoverDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.model.OrderItem;
import com.cafe.model.ShiftHandover;
import com.cafe.service.shared.InventoryService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** B7 · HandoverService — bàn giao ca: ghi chú + KPI lead-time (read OrderItem StartedAt/DoneAt). */
public class HandoverService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ShiftHandoverDao handoverDao = new ShiftHandoverDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final InventoryService inventoryService = new InventoryService();

    public List<ShiftHandover> getHandovers(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return handoverDao.findByBranch(conn, branchId);
        }
    }

    public int createHandover(int branchId, String note, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = handoverDao.insert(conn, branchId, note, userId);
                conn.commit();
                return id;
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public HandoverKpi getKpi(int branchId) throws SQLException {
        LocalDateTime[] window = todayWindowUtc();
        try (Connection conn = DBConnection.getConnection()) {
            long[] s = orderItemDao.leadTimeStats(conn, branchId, window[0], window[1]);
            return new HandoverKpi(s[0], s[1]);
        }
    }

    /** Danh sách món đã pha xong hôm nay để bàn giao ca, tính theo ngày giờ Việt Nam. */
    public List<OrderItem> getBrewHistory(int branchId) throws SQLException {
        LocalDateTime[] window = todayWindowUtc();
        try (Connection conn = DBConnection.getConnection()) {
            return orderItemDao.findBrewedToday(conn, branchId, window[0], window[1]);
        }
    }

    public int countExpiredActivePrepBatches(int branchId) throws SQLException {
        return inventoryService.getExpiredActivePrepBatches(branchId).size();
    }

    public BrewHistoryPage getBrewHistoryPage(int branchId, String query, String status, String orderType,
                                              int requestedPage, int pageSize) throws SQLException {
        LocalDateTime[] window = todayWindowUtc();
        int safePageSize = pageSize > 0 ? pageSize : 10;
        try (Connection conn = DBConnection.getConnection()) {
            int total = orderItemDao.countBrewedToday(conn, branchId, window[0], window[1], query, status, orderType);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / safePageSize));
            int page = Math.max(1, Math.min(requestedPage, totalPages));
            List<OrderItem> items = orderItemDao.findBrewedTodayPage(conn, branchId, window[0], window[1],
                    query, status, orderType, (page - 1) * safePageSize, safePageSize);
            return new BrewHistoryPage(items, total, page, safePageSize);
        }
    }


    private static LocalDateTime[] todayWindowUtc() {
        // Mốc "hôm nay" theo giờ VN → UTC (đồng nhất với Waste/Prep; DoneAt lưu UTC).
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalDateTime fromUtc = today.atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime toUtc = today.plusDays(1).atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        return new LocalDateTime[]{fromUtc, toUtc};
    }

    /** KPI hôm nay: lead time TB (giây, -1 = chưa có) + số ly đã pha xong. */
    public static class HandoverKpi {
        private final long avgLeadSeconds;
        private final long cupCount;
        public HandoverKpi(long avgLeadSeconds, long cupCount) {
            this.avgLeadSeconds = avgLeadSeconds; this.cupCount = cupCount;
        }
        public long getAvgLeadSeconds() { return avgLeadSeconds; }
        public long getCupCount() { return cupCount; }
        public boolean isHasLead() { return avgLeadSeconds >= 0; }
        /** Hiển thị "m phút s giây" hoặc "—". */
        public String getAvgLeadDisplay() {
            if (avgLeadSeconds < 0) return "—";
            long m = avgLeadSeconds / 60, s = avgLeadSeconds % 60;
            return (m > 0 ? m + " phút " : "") + s + " giây";
        }
    }

    public static class BrewHistoryPage {
        private final List<OrderItem> items;
        private final int total;
        private final int page;
        private final int pageSize;

        public BrewHistoryPage(List<OrderItem> items, int total, int page, int pageSize) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
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
}
