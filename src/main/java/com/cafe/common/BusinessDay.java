package com.cafe.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Mốc "ngày kinh doanh" của một chi nhánh.
 *
 * <p>Quầy pha chế chỉ được hiển thị việc của ca hiện tại. Không có mốc này thì mọi món chưa
 * SERVED từ trước tới nay đều nằm mãi trên màn, kéo theo cảnh báo trễ lúc nào cũng đỏ —
 * cảnh báo lúc nào cũng bật thì nhân viên sẽ học cách phớt lờ.
 *
 * <p>Ngày kinh doanh bắt đầu từ giờ mở cửa <b>gần nhất đã trôi qua</b>: chi nhánh mở 07:00,
 * lúc 04:03 sáng thì mốc là 07:00 <i>hôm qua</i> (ca đêm vẫn đang chạy). Chi nhánh chưa khai
 * giờ mở cửa thì mặc định cắt theo nửa đêm.
 *
 * <p>DB lưu UTC ({@code SYSUTCDATETIME()}) nên mốc luôn được quy đổi về UTC trước khi so sánh.
 */
public final class BusinessDay {

    /** Toàn hệ thống chạy theo giờ Việt Nam. */
    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private BusinessDay() { }

    /** Chỉ giờ — card KDS, nơi mốc luôn nằm trong ngày kinh doanh hiện tại. */
    private static final DateTimeFormatter TIME_VN = DateTimeFormatter.ofPattern("HH:mm");
    /** Giờ kèm ngày — chấm công, vì ca đêm tan sang ngày hôm sau. */
    private static final DateTimeFormatter DATE_TIME_VN = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    /** Ngày trước giờ — đơn treo nhiều ngày, mắt đọc theo trục ngày nhanh hơn. */
    private static final DateTimeFormatter STAMP_VN = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /** UTC (như DB lưu) → giờ Việt Nam để hiển thị. */
    public static LocalDateTime toVn(LocalDateTime utc) {
        return utc == null ? null
                : utc.atZone(ZoneOffset.UTC).withZoneSameInstant(VN_ZONE).toLocalDateTime();
    }

    public static String fmtTimeVn(LocalDateTime utc) {
        return utc == null ? "" : toVn(utc).format(TIME_VN);
    }

    public static String fmtDateTimeVn(LocalDateTime utc) {
        return utc == null ? "-" : toVn(utc).format(DATE_TIME_VN);
    }

    public static String fmtStampVn(LocalDateTime utc) {
        return utc == null ? "—" : toVn(utc).format(STAMP_VN);
    }

    /** Đầu ngày VN (00:00) quy về UTC, để so với cột DATETIME2 lưu UTC. */
    public static LocalDateTime vnDayStartUtc(LocalDate vnDate) {
        if (vnDate == null) return null;
        return vnDate.atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Đầu ngày VN kế tiếp quy về UTC — mốc kết thúc theo kiểu nửa mở [from, to). */
    public static LocalDateTime vnDayEndExclusiveUtc(LocalDate vnDate) {
        if (vnDate == null) return null;
        return vnDate.plusDays(1).atStartOfDay(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Mốc bắt đầu ngày kinh doanh hiện tại, theo giờ UTC để so trực tiếp với cột DATETIME2. */
    public static LocalDateTime startUtc(LocalTime openTime) {
        return startUtc(openTime, LocalDateTime.now(VN_ZONE));
    }

    /** Bản nhận "bây giờ" để test được mà không phụ thuộc đồng hồ hệ thống. */
    public static LocalDateTime startUtc(LocalTime openTime, LocalDateTime nowVn) {
        LocalTime open = openTime == null ? LocalTime.MIDNIGHT : openTime;
        LocalDate day = nowVn.toLocalDate();
        // Chưa tới giờ mở cửa hôm nay → ca hiện tại vẫn là ca mở từ hôm qua.
        if (nowVn.toLocalTime().isBefore(open)) day = day.minusDays(1);
        return day.atTime(open).atZone(VN_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
