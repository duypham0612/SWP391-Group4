package com.cafe.common;

import java.time.Duration;
import java.time.LocalTime;

/** Logic thuần tính giới hạn giờ làm khi xếp ca. */
public final class ShiftHours {
    public static final double MAX_DAILY_HOURS = 8;
    public static final double MAX_WEEKLY_HOURS = 48;

    private ShiftHours() {}

    public static double hours(LocalTime start, LocalTime end) {
        return Duration.between(start, end).toMinutes() / 60.0;
    }

    public static boolean exceedsDaily(double totalHours) {
        return totalHours > MAX_DAILY_HOURS;
    }

    public static boolean exceedsWeekly(double totalHours) {
        return totalHours > MAX_WEEKLY_HOURS;
    }
}
