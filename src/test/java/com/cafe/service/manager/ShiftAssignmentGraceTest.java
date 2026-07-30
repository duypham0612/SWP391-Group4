package com.cafe.service.manager;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiftAssignmentGraceTest {

    private static final LocalDate WORK_DATE = LocalDate.of(2026, 7, 30);
    private static final LocalTime START_TIME = LocalTime.of(8, 0);

    @Test
    void permits_assignment_before_shift_starts() {
        assertTrue(ShiftService.canAssign(
                WORK_DATE, START_TIME, LocalDateTime.of(2026, 7, 30, 7, 59)));
    }

    @Test
    void permits_assignment_exactly_ten_minutes_after_start() {
        assertTrue(ShiftService.canAssign(
                WORK_DATE, START_TIME, LocalDateTime.of(2026, 7, 30, 8, 10)));
    }

    @Test
    void rejects_assignment_after_the_ten_minute_boundary() {
        assertFalse(ShiftService.canAssign(
                WORK_DATE, START_TIME, LocalDateTime.of(2026, 7, 30, 8, 10, 1)));
    }

    @Test
    void rejects_assignment_for_an_old_work_date() {
        assertFalse(ShiftService.canAssign(
                WORK_DATE, START_TIME, LocalDateTime.of(2026, 7, 31, 7, 0)));
    }
}
