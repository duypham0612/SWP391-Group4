package com.cafe.common;

import java.time.LocalTime;

/**
 * ★ Shift Conflict Resolver — logic thuần (không DB) để dễ unit-test.
 *
 * Một nhân viên KHÔNG được xếp 2 ca chồng giờ trong cùng một ngày.
 * Hai khoảng giờ [s1,e1) và [s2,e2) chồng nhau khi: s1 < e2 && s2 < e1.
 * (Chạm biên — ca này kết thúc đúng lúc ca kia bắt đầu — KHÔNG tính là chồng.)
 *
 * Giả định: ca trong ngày (start < end). Ca qua đêm chưa hỗ trợ (schema dùng TIME).
 */
public final class ShiftConflict {
    private ShiftConflict() {}

    public static boolean overlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        if (s1 == null || e1 == null || s2 == null || e2 == null) return false;
        return s1.isBefore(e2) && s2.isBefore(e1);
    }
}
