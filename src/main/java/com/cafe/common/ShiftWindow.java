package com.cafe.common;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Cửa sổ ca còn hiệu lực để chấm công.
 *
 * <p>Ca xếp theo {@code WorkDate}, nhưng ca đêm 22:00–06:00 của ngày N chỉ kết thúc vào sáng ngày
 * N+1. Nếu chỉ lấy ca có {@code WorkDate} đúng bằng ngày hôm nay thì sau nửa đêm nhân viên bị báo
 * "chưa được xếp ca": không tan ca được và mọi màn khoá theo trực ca cũng coi như đã ngoài ca.
 */
public final class ShiftWindow {

    /** Tan ca trễ vẫn chấm được trong khoảng này sau giờ kết thúc — dọn quầy, chốt sổ, bàn giao. */
    public static final Duration CLOCK_OUT_GRACE = Duration.ofHours(6);

    private ShiftWindow() {}

    /** Ca qua đêm khi giờ kết thúc không sau giờ bắt đầu (22:00–06:00). */
    public static boolean isOvernight(LocalTime start, LocalTime end) {
        return start != null && end != null && !end.isAfter(start);
    }

    /** Mốc kết thúc theo lịch; ca qua đêm rơi sang ngày hôm sau. */
    public static LocalDateTime scheduledEnd(LocalDate workDate, LocalTime start, LocalTime end) {
        if (workDate == null || end == null) return null;
        return LocalDateTime.of(isOvernight(start, end) ? workDate.plusDays(1) : workDate, end);
    }

    /**
     * Ca có còn được chấm công tại thời điểm {@code nowVn} không.
     * Ca của chính ngày kinh doanh luôn tính; ca của hôm trước chỉ tính khi là ca đêm và chưa quá
     * giờ kết thúc cộng {@code grace} — hết khoảng đó thì ca cũ rơi ra, không treo trực ca vô hạn.
     */
    public static boolean isClockable(LocalDate workDate, LocalTime start, LocalTime end,
                                      LocalDate businessDate, LocalDateTime nowVn, Duration grace) {
        if (workDate == null || businessDate == null) return false;
        if (workDate.equals(businessDate)) return true;
        if (!workDate.equals(businessDate.minusDays(1)) || !isOvernight(start, end)) return false;
        if (nowVn == null) return false;
        return nowVn.isBefore(scheduledEnd(workDate, start, end).plus(grace == null ? Duration.ZERO : grace));
    }
}
