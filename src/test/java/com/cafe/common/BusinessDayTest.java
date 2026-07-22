package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mốc ngày kinh doanh — logic thuần, không đụng DB.
 * Trọng tâm: ca đêm (sau nửa đêm nhưng chưa tới giờ mở cửa) vẫn phải thuộc ngày hôm trước,
 * nếu không toàn bộ món đang pha lúc 0h sẽ biến mất khỏi quầy.
 */
class BusinessDayTest {

    /** Đổi mốc UTC trả về thành giờ Việt Nam cho dễ đối chiếu. */
    private static LocalDateTime asVn(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(BusinessDay.VN_ZONE).toLocalDateTime();
    }

    @Test
    void after_opening_hour_cuts_at_today_opening() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 18, 14, 30);   // 14:30, quán mở 07:00
        assertEquals(LocalDateTime.of(2026, 7, 18, 7, 0),
                asVn(BusinessDay.startUtc(LocalTime.of(7, 0), now)));
    }

    /** 04:03 sáng mà quán mở 07:00 → vẫn đang là ca mở từ hôm qua. */
    @Test
    void before_opening_hour_still_belongs_to_yesterday_shift() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 18, 4, 3);
        assertEquals(LocalDateTime.of(2026, 7, 17, 7, 0),
                asVn(BusinessDay.startUtc(LocalTime.of(7, 0), now)));
    }

    /** Đúng khoảnh khắc mở cửa đã tính là ngày mới. */
    @Test
    void exactly_at_opening_hour_starts_new_day() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 18, 7, 0);
        assertEquals(LocalDateTime.of(2026, 7, 18, 7, 0),
                asVn(BusinessDay.startUtc(LocalTime.of(7, 0), now)));
    }

    /** Chi nhánh chưa khai giờ mở cửa → cắt theo nửa đêm. */
    @Test
    void null_opening_hour_falls_back_to_midnight() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 18, 4, 3);
        assertEquals(LocalDateTime.of(2026, 7, 18, 0, 0),
                asVn(BusinessDay.startUtc(null, now)));
    }

    /** Mốc trả về phải là UTC (VN = UTC+7) để so thẳng với cột DATETIME2 trong DB. */
    @Test
    void returned_instant_is_utc() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 18, 14, 30);
        assertEquals(LocalDateTime.of(2026, 7, 18, 0, 0),
                BusinessDay.startUtc(LocalTime.of(7, 0), now));
    }

    @Test
    void utc_evening_is_next_calendar_day_in_vietnam() {
        assertEquals(java.time.LocalDate.of(2026, 7, 23),
                BusinessDay.vnDate(Instant.parse("2026-07-22T17:00:00Z")));
        assertEquals(java.time.LocalDate.of(2026, 7, 23),
                BusinessDay.vnDate(Instant.parse("2026-07-22T23:59:59Z")));
    }
}
