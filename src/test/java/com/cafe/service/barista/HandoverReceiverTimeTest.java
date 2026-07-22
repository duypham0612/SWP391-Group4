package com.cafe.service.barista;

import com.cafe.model.ShiftAssignment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Thuật toán mốc ca phải chọn đúng ngày kế tiếp khi ca gửi kết thúc cuối ngày. */
class HandoverReceiverTimeTest {
    @Test
    void normalShiftEndsOnItsWorkDate() {
        ShiftAssignment shift = shift(LocalDate.of(2026, 7, 22), "12:00", "17:00");
        assertEquals("2026-07-22T17:00", HandoverService.scheduledEnd(shift).toString());
    }

    @Test
    void overnightShiftEndsOnFollowingDate() {
        ShiftAssignment shift = shift(LocalDate.of(2026, 7, 22), "22:00", "06:00");
        assertEquals("2026-07-23T06:00", HandoverService.scheduledEnd(shift).toString());
    }

    private ShiftAssignment shift(LocalDate date, String start, String end) {
        ShiftAssignment shift = new ShiftAssignment();
        shift.setWorkDate(date); shift.setStartTime(LocalTime.parse(start)); shift.setEndTime(LocalTime.parse(end));
        return shift;
    }
}
