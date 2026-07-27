package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quy tắc "còn đang trực quầy" — quyết định người khác có được thu hồi món đang pha hay không.
 * Trọng tâm: người bỏ về mà KHÔNG bấm tan ca vẫn phải rơi khỏi trạng thái còn trực sau giờ tan
 * theo lịch, nếu không món họ giữ bị khoá tới tận ngày hôm sau.
 */
class ShiftDutyTest {

    private static final LocalDate DAY = LocalDate.of(2026, 7, 25);
    private static final LocalTime START = LocalTime.of(7, 0);
    private static final LocalTime END = LocalTime.of(15, 0);

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(DAY, LocalTime.of(hour, minute));
    }

    @Test
    void checked_in_and_still_within_the_shift_is_on_duty() {
        assertTrue(ShiftWindow.onDuty(DAY, START, END, true, false, at(11, 0)));
    }

    @Test
    void checked_out_is_never_on_duty() {
        assertFalse(ShiftWindow.onDuty(DAY, START, END, true, true, at(11, 0)));
    }

    @Test
    void not_checked_in_is_never_on_duty() {
        assertFalse(ShiftWindow.onDuty(DAY, START, END, false, false, at(11, 0)));
    }

    /** Tan ca trễ ít phút vẫn đang dọn quầy — chưa được giật món khỏi tay họ. */
    @Test
    void just_past_the_scheduled_end_is_still_on_duty_within_grace() {
        assertTrue(ShiftWindow.onDuty(DAY, START, END, true, false, at(15, 20)));
    }

    /** Quá ân hạn mà vẫn "chưa tan ca" = đã bỏ về không bấm nút → món phải được thu hồi. */
    @Test
    void long_past_the_scheduled_end_is_off_duty_even_without_clock_out() {
        assertFalse(ShiftWindow.onDuty(DAY, START, END, true, false, at(15, 31)));
        assertFalse(ShiftWindow.onDuty(DAY, START, END, true, false, at(23, 0)));
    }

    /** Ca đêm 22:00–06:00 kết thúc SANG NGÀY HÔM SAU — so thẳng LocalTime sẽ sai ở đây. */
    @Test
    void overnight_shift_stays_on_duty_after_midnight() {
        LocalTime nightStart = LocalTime.of(22, 0);
        LocalTime nightEnd = LocalTime.of(6, 0);
        LocalDateTime twoAm = LocalDateTime.of(DAY.plusDays(1), LocalTime.of(2, 0));
        LocalDateTime sevenAm = LocalDateTime.of(DAY.plusDays(1), LocalTime.of(7, 0));

        assertTrue(ShiftWindow.onDuty(DAY, nightStart, nightEnd, true, false, twoAm));
        assertFalse(ShiftWindow.onDuty(DAY, nightStart, nightEnd, true, false, sevenAm));
    }

    /** Thiếu dữ liệu lịch ca thì tin chấm công, không được ngầm coi là đã rời quầy. */
    @Test
    void missing_schedule_falls_back_to_the_clock_record() {
        assertTrue(ShiftWindow.onDuty(null, null, null, true, false, at(11, 0)));
        assertFalse(ShiftWindow.onDuty(null, null, null, true, true, at(11, 0)));
    }
}
