package com.cafe.service.manager;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WasteRangeTest {

    @Test
    void empty_params_default_to_last_seven_days() {
        LocalDate today = LocalDate.of(2026, 7, 19);

        WasteReportService.Range range = WasteReportService.resolveRange(null, "", today);

        assertEquals(today.minusDays(6), range.getFromDate());
        assertEquals(today, range.getToDate());
        assertEquals(7, range.getDayCount());
    }

    @Test
    void bad_date_params_do_not_throw() {
        LocalDate today = LocalDate.of(2026, 7, 19);

        WasteReportService.Range range = assertDoesNotThrow(
                () -> WasteReportService.resolveRange("hom-nay", "ngay-mai", today));

        assertEquals(today.minusDays(6), range.getFromDate());
        assertEquals(today, range.getToDate());
    }

    @Test
    void reversed_dates_are_swapped() {
        WasteReportService.Range range = WasteReportService.resolveRange(
                "2026-07-19", "2026-07-10", LocalDate.of(2026, 7, 19));

        assertEquals(LocalDate.of(2026, 7, 10), range.getFromDate());
        assertEquals(LocalDate.of(2026, 7, 19), range.getToDate());
        assertFalse(range.getDayCount() <= 0);
    }

    @Test
    void one_day_range_is_24_hours() {
        WasteReportService.Range range = WasteReportService.resolveRange(
                "2026-07-19", "2026-07-19", LocalDate.of(2026, 7, 19));

        assertEquals(1, range.getDayCount());
        assertEquals(24, Duration.between(range.getFromUtc(), range.getToUtc()).toHours());
    }

    @Test
    void very_large_ranges_are_capped() {
        WasteReportService.Range range = WasteReportService.resolveRange(
                "2025-01-01", "2026-07-19", LocalDate.of(2026, 7, 19));

        assertEquals(92, range.getDayCount());
        assertEquals(LocalDate.of(2026, 7, 19), range.getToDate());
    }
}
