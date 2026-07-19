package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ★ Test trước cho Shift Conflict Resolver (logic rủi ro). */
class ShiftConflictTest {

    private static LocalTime t(String s) { return LocalTime.parse(s); }

    @Test
    void overlapping_shifts_conflict() {
        // Ca sáng 07:00-12:00 vs ca 11:00-15:00 → chồng 11:00-12:00
        assertTrue(ShiftConflict.overlaps(t("07:00"), t("12:00"), t("11:00"), t("15:00")));
    }

    @Test
    void contained_shift_conflicts() {
        // Ca 10:00-11:00 nằm trọn trong 08:00-16:00
        assertTrue(ShiftConflict.overlaps(t("08:00"), t("16:00"), t("10:00"), t("11:00")));
    }

    @Test
    void touching_boundary_is_not_conflict() {
        // Ca sáng kết thúc 12:00, ca chiều bắt đầu 12:00 → KHÔNG chồng
        assertFalse(ShiftConflict.overlaps(t("07:00"), t("12:00"), t("12:00"), t("17:00")));
    }

    @Test
    void disjoint_shifts_no_conflict() {
        assertFalse(ShiftConflict.overlaps(t("07:00"), t("11:00"), t("13:00"), t("17:00")));
    }

    @Test
    void null_times_no_conflict() {
        assertFalse(ShiftConflict.overlaps(null, t("12:00"), t("11:00"), t("15:00")));
    }
}
