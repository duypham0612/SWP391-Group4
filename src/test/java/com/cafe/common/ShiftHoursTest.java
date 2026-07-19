package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiftHoursTest {

    private static LocalTime t(String s) { return LocalTime.parse(s); }

    @Test
    void hours_returns_duration_in_hours() {
        assertEquals(5.0, ShiftHours.hours(t("07:00"), t("12:00")));
    }

    @Test
    void daily_boundary_allows_exact_limit() {
        assertFalse(ShiftHours.exceedsDaily(8.0));
    }

    @Test
    void daily_boundary_blocks_above_limit() {
        assertTrue(ShiftHours.exceedsDaily(8.5));
    }

    @Test
    void weekly_boundary_allows_exact_limit() {
        assertFalse(ShiftHours.exceedsWeekly(48.0));
    }

    @Test
    void weekly_boundary_blocks_above_limit() {
        assertTrue(ShiftHours.exceedsWeekly(48.5));
    }

    @Test
    void worked_returns_zero_when_missing_or_negative() {
        LocalDateTime start = LocalDateTime.parse("2026-07-19T08:00:00");

        assertEquals(0d, ShiftHours.worked(null, start));
        assertEquals(0d, ShiftHours.worked(start, start.minusHours(1)));
    }

    @Test
    void worked_rounds_to_one_decimal() {
        LocalDateTime start = LocalDateTime.parse("2026-07-19T08:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-07-19T12:20:00");

        assertEquals(4.3, ShiftHours.worked(start, end));
    }
}
