package com.cafe.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * ★ Shift Conflict Resolver — logic thuần (không DB) để dễ unit-test.
 *
 * Một nhân viên KHÔNG được xếp 2 ca chồng giờ trong cùng một ngày.
 * Hai khoảng giờ [s1,e1) và [s2,e2) chồng nhau khi: s1 < e2 && s2 < e1.
 * (Chạm biên — ca này kết thúc đúng lúc ca kia bắt đầu — KHÔNG tính là chồng.)
 *
 * Ca qua đêm dùng ngày bắt đầu làm WorkDate và kết thúc ở ngày kế tiếp.
 */
public final class ShiftConflict {
    private ShiftConflict() {}

    public static boolean overlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        if (s1 == null || e1 == null || s2 == null || e2 == null) return false;
        return overlaps(LocalDate.of(2000, 1, 1), s1, e1,
                LocalDate.of(2000, 1, 1), s2, e2);
    }

    public static boolean overlaps(LocalDate d1, LocalTime s1, LocalTime e1,
                                   LocalDate d2, LocalTime s2, LocalTime e2) {
        if (d1 == null || s1 == null || e1 == null
                || d2 == null || s2 == null || e2 == null) return false;
        LocalDateTime start1 = LocalDateTime.of(d1, s1);
        LocalDateTime end1 = LocalDateTime.of(
                e1.isAfter(s1) ? d1 : d1.plusDays(1), e1);
        LocalDateTime start2 = LocalDateTime.of(d2, s2);
        LocalDateTime end2 = LocalDateTime.of(
                e2.isAfter(s2) ? d2 : d2.plusDays(1), e2);
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
