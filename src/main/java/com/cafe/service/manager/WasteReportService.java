package com.cafe.service.manager;

import com.cafe.common.BusinessDay;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.shared.WasteSummary;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * M · WasteReportService — nhật ký hao hụt toàn chi nhánh cho Quản lý.
 * Khác WasteService của Pha chế: không giới hạn theo ca, xem được mọi người ghi.
 */
public class WasteReportService {
    /** Nạp cả khoảng vào RAM để tổng hợp nên phải chặn khoảng vô hạn. */
    private static final int MAX_DAYS = 92;
    private static final int DEFAULT_DAYS = 7;

    private final InventoryService inventoryService = new InventoryService();

    /** Chuẩn hoá khoảng ngày từ tham số URL. Hàm thuần — test được, không đụng DB. */
    public static Range resolveRange(String fromParam, String toParam, LocalDate todayVn) {
        LocalDate from = parseOrNull(fromParam);
        LocalDate to = parseOrNull(toParam);
        if (from == null && to == null) {
            to = todayVn;
            from = todayVn.minusDays(DEFAULT_DAYS - 1);
        } else if (from == null) {
            from = to.minusDays(DEFAULT_DAYS - 1);
        } else if (to == null) {
            to = from.plusDays(DEFAULT_DAYS - 1);
        }
        if (to.isBefore(from)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) from = to.minusDays(MAX_DAYS - 1);
        return new Range(from, to);
    }

    /** Tổng hợp toàn khoảng — không phân trang, không áp bộ lọc bảng. */
    public WasteSummary summarize(int branchId, Range range) throws SQLException {
        return WasteSummary.from(
                inventoryService.getWasteLogs(branchId, range.getFromUtc(), range.getToUtc()));
    }

    /** Đúng một trang cho bảng; lọc và phân trang đều làm ở DB. */
    public InventoryService.WasteLogPage page(int branchId, Range range, String query,
                                              String wasteType, String status,
                                              int page, int pageSize) throws SQLException {
        return inventoryService.getWasteLogPage(branchId, range.getFromUtc(), range.getToUtc(),
                query, wasteType, status, page, pageSize);
    }

    public java.util.List<com.cafe.model.WasteReview> openReviews(int branchId) throws SQLException {
        return inventoryService.getOpenWasteReviews(branchId);
    }

    public boolean resolveReview(int branchId, long reviewId, int managerId, String note) throws SQLException {
        return inventoryService.resolveWasteReview(branchId, reviewId, managerId, note);
    }

    /** Ngày rác trên URL không được làm 500. */
    private static LocalDate parseOrNull(String s) {
        try {
            return s == null || s.isBlank() ? null : LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Giữ cả mốc VN (render vào input) lẫn mốc UTC (query) để không lẫn múi giờ. */
    public static final class Range {
        private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private final LocalDate fromDate;
        private final LocalDate toDate;

        private Range(LocalDate fromDate, LocalDate toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
        }

        public LocalDate getFromDate() { return fromDate; }
        public LocalDate getToDate() { return toDate; }
        public LocalDateTime getFromUtc() { return BusinessDay.vnDayStartUtc(fromDate); }
        public LocalDateTime getToUtc() { return BusinessDay.vnDayEndExclusiveUtc(toDate); }
        public long getDayCount() { return ChronoUnit.DAYS.between(fromDate, toDate) + 1; }
        public String getLabel() { return fromDate.format(LABEL_FMT) + " – " + toDate.format(LABEL_FMT); }
    }
}
