package com.cafe.service.barista;

import org.junit.jupiter.api.Test;
import com.cafe.web.viewmodel.ViewFormatter;

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
    private final ViewFormatter view = new ViewFormatter();

    /** TODAY: cửa sổ đúng 24h và mốc đầu = 00:00 giờ VN (quy về UTC). */
    @Test
    void today_spans_one_vietnam_day_in_utc() {
        WasteService.WasteScope scope = WasteService.WasteScope.today();

        assertEquals("TODAY", scope.getKind());
        assertEquals("Hôm nay", view.scopeLabel(scope.getKind()));

        LocalDateTime fromUtc = scope.getFromUtc();
        LocalDateTime toUtc = scope.getToUtc();
        // Đúng một ngày.
        assertEquals(Duration.ofDays(1), Duration.between(fromUtc, toUtc));
        // Mốc đầu quy ngược về giờ VN phải là nửa đêm.
        LocalTime vnStart = fromUtc.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(VN_ZONE).toLocalTime();
        assertEquals(LocalTime.MIDNIGHT, vnStart);
    }

    /**
     * Ngày kinh doanh: cửa sổ 24h tính từ giờ MỞ CỬA gần nhất đã trôi qua, dùng chung mốc với
     * Quầy pha chế. Đây là điểm khác then chốt so với TODAY (cắt theo nửa đêm).
     */
    @Test
    void businessDay_starts_at_branch_open_time_not_midnight() {
        LocalTime openTime = LocalTime.of(7, 0);
        WasteService.WasteScope scope = WasteService.WasteScope.businessDay(openTime);

        assertEquals("BUSINESS_DAY", scope.getKind());
        assertEquals(Duration.ofDays(1), Duration.between(scope.getFromUtc(), scope.getToUtc()));
        // Mốc đầu quy ngược về giờ VN phải đúng giờ mở cửa, không phải 00:00.
        LocalTime vnStart = scope.getFromUtc().atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(VN_ZONE).toLocalTime();
        assertEquals(openTime, vnStart);
    }

    /** Chi nhánh chưa khai giờ mở cửa → lùi về đúng hành vi cũ (cắt theo nửa đêm). */
    @Test
    void businessDay_without_open_time_falls_back_to_today() {
        WasteService.WasteScope scope = WasteService.WasteScope.businessDay(null);

        assertEquals("TODAY", scope.getKind());
        LocalTime vnStart = scope.getFromUtc().atOffset(ZoneOffset.UTC)
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
