package com.cafe.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiftAssignmentAttendanceTimeTest {

    @Test
    void comparesUtcCheckInWithVietnamScheduledTime() {
        ShiftAssignment shift = shift(LocalTime.of(7, 30), LocalTime.of(11, 30));
        // 19:55 UTC ngày hôm trước = 02:55 giờ Việt Nam ngày làm việc.
        shift.setCheckInAt(LocalDateTime.of(2026, 8, 3, 19, 55));

        assertEquals(275, shift.getEarlyArrivalMinutes());
        assertEquals(0, shift.getLateMinutes());
        assertTrue(shift.isEarlyArrival());
        assertFalse(shift.isLate());
    }

    @Test
    void calculatesLateAndEarlyLeaveAcrossVietnamCalendarDates() {
        ShiftAssignment shift = shift(LocalTime.of(22, 0), LocalTime.of(6, 0));
        // 22:10 VN = 15:10 UTC; 05:45 VN hôm sau = 22:45 UTC ngày làm việc.
        shift.setCheckInAt(LocalDateTime.of(2026, 8, 4, 15, 10));
        shift.setCheckOutAt(LocalDateTime.of(2026, 8, 4, 22, 45));

        assertEquals(10, shift.getLateMinutes());
        assertEquals(15, shift.getEarlyLeaveMinutes());
    }

    private ShiftAssignment shift(LocalTime start, LocalTime end) {
        ShiftAssignment shift = new ShiftAssignment();
        shift.setWorkDate(LocalDate.of(2026, 8, 4));
        shift.setStartTime(start);
        shift.setEndTime(end);
        return shift;
    }
}
