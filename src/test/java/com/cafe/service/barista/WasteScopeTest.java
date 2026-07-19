package com.cafe.service.barista;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * B5 · Test cửa sổ thời gian lọc hao hụt (WasteService.WasteScope) — logic thuần, không đụng DB.
 * Kiểm: TODAY = đúng 1 ngày theo giờ VN quy về UTC; ca đang mở/đã tan giữ nguyên mốc check-in/out.
 */
class WasteScopeTest {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** TODAY: cửa sổ đúng 24h và mốc đầu = 00:00 giờ VN (quy về UTC). */
    @Test
    void today_spans_one_vietnam_day_in_utc() {
        WasteService.WasteScope scope = WasteService.WasteScope.today();

        assertEquals("TODAY", scope.getKind());
        assertEquals("Hôm nay", scope.getLabel());

        LocalDateTime fromUtc = scope.getFromUtc();
        LocalDateTime toUtc = scope.getToUtc();
        // Đúng một ngày.
        assertEquals(Duration.ofDays(1), Duration.between(fromUtc, toUtc));
        // Mốc đầu quy ngược về giờ VN phải là nửa đêm.
        LocalTime vnStart = fromUtc.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(VN_ZONE).toLocalTime();
        assertEquals(LocalTime.MIDNIGHT, vnStart);
    }

    /** Ca đang mở: từ mốc check-in, chưa có mốc kết thúc (to = null → lọc mở tới hiện tại). */
    @Test
    void openShift_keeps_checkIn_and_null_end() {
        LocalDateTime checkIn = LocalDateTime.of(2026, 7, 15, 1, 30);  // giờ UTC
        WasteService.WasteScope scope = WasteService.WasteScope.openShift(checkIn);

        assertEquals("OPEN_SHIFT", scope.getKind());
        assertEquals(checkIn, scope.getFromUtc());
        assertNull(scope.getToUtc());
    }

    /** Ca đã tan: giữ nguyên cặp mốc check-in/check-out. */
    @Test
    void closedShift_keeps_both_bounds() {
        LocalDateTime checkIn = LocalDateTime.of(2026, 7, 15, 1, 30);
        LocalDateTime checkOut = LocalDateTime.of(2026, 7, 15, 9, 45);
        WasteService.WasteScope scope = WasteService.WasteScope.closedShift(checkIn, checkOut);

        assertEquals("CLOSED_SHIFT", scope.getKind());
        assertEquals(checkIn, scope.getFromUtc());
        assertEquals(checkOut, scope.getToUtc());
    }
}
