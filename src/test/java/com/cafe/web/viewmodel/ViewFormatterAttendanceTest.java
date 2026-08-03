package com.cafe.web.viewmodel;

import com.cafe.model.ShiftClockStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewFormatterAttendanceTest {
    private final ViewFormatter view = new ViewFormatter();

    @Test
    void rendersUtcAttendanceForVietnamDatetimeLocalInput() {
        assertEquals("2026-08-04T02:55",
                view.dateTimeLocalVn(LocalDateTime.of(2026, 8, 3, 19, 55)));
        assertEquals("", view.dateTimeLocalVn(null));
    }

    @Test
    void rendersLongMinuteDifferencesAsReadableDuration() {
        assertEquals("15 phút", view.attendanceDuration(15));
        assertEquals("4 giờ 35 phút", view.attendanceDuration(275));
        assertEquals("2 giờ", view.attendanceDuration(120));
    }

    @Test
    void explainsWhenClockInWindowWillOpen() {
        ShiftClockStatus status = new ShiftClockStatus();
        status.setHasAssignment(true);
        status.setWaitingForStart(true);
        status.setStartTime(LocalTime.of(7, 30));

        assertEquals("Chưa đến giờ vào ca. Có thể bắt đầu từ 07:15.", view.shiftStatus(status));
    }
}
