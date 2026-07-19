package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BusinessDayVnRangeTest {

    @Test
    void vn_day_start_is_previous_utc_evening() {
        assertEquals(LocalDateTime.of(2026, 7, 18, 17, 0),
                BusinessDay.vnDayStartUtc(LocalDate.of(2026, 7, 19)));
    }

    @Test
    void vn_day_end_is_exclusive_next_day_start_in_utc() {
        assertEquals(LocalDateTime.of(2026, 7, 19, 17, 0),
                BusinessDay.vnDayEndExclusiveUtc(LocalDate.of(2026, 7, 19)));
    }

    @Test
    void vn_day_range_is_24_hours() {
        LocalDate day = LocalDate.of(2026, 7, 19);

        assertEquals(24, Duration.between(
                BusinessDay.vnDayStartUtc(day),
                BusinessDay.vnDayEndExclusiveUtc(day)).toHours());
    }

    @Test
    void end_exclusive_equals_next_day_start() {
        LocalDate day = LocalDate.of(2026, 7, 19);

        assertEquals(BusinessDay.vnDayStartUtc(day.plusDays(1)),
                BusinessDay.vnDayEndExclusiveUtc(day));
    }

    @Test
    void null_date_stays_null() {
        assertNull(BusinessDay.vnDayStartUtc(null));
        assertNull(BusinessDay.vnDayEndExclusiveUtc(null));
    }
}
