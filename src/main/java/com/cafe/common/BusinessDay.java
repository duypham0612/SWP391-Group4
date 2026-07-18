package com.cafe.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

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
