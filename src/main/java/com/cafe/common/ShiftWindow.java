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

    /** Nhân viên có thể chuẩn bị quầy và vào ca sớm tối đa 15 phút. */
    public static final Duration CLOCK_IN_EARLY_GRACE = Duration.ofMinutes(15);

    /** Tan ca trễ vẫn chấm được trong khoảng này sau giờ kết thúc — dọn quầy, chốt sổ, bàn giao. */
    public static final Duration CLOCK_OUT_GRACE = Duration.ofHours(6);

    /**
     * Quá giờ tan ca theo lịch bấy nhiêu mà chưa bấm tan ca thì coi như đã rời quầy — dùng cho
     * quyền thu hồi món đang pha của người khác.
     *
     * <p>Cố ý NGẮN hơn nhiều so với {@link #CLOCK_OUT_GRACE}: ân hạn chấm công dài là để nhân viên
     * không mất giờ công, còn ở đây mỗi phút chờ là một ly khách đang đợi mà không ai được pha tiếp.
     */
    public static final Duration OFF_DUTY_GRACE = Duration.ofMinutes(30);

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

    /** Mốc bắt đầu theo lịch, dùng giờ tường Việt Nam. */
    public static LocalDateTime scheduledStart(LocalDate workDate, LocalTime start) {
        return workDate == null || start == null ? null : LocalDateTime.of(workDate, start);
    }

    /**
     * Chỉ cho vào ca từ 15 phút trước giờ bắt đầu cho tới trước giờ kết thúc.
     * Ca qua đêm tự kéo mốc kết thúc sang ngày hôm sau.
     */
    public static boolean canClockIn(LocalDate workDate, LocalTime start, LocalTime end,
                                     LocalDateTime nowVn) {
        LocalDateTime scheduledStart = scheduledStart(workDate, start);
        LocalDateTime scheduledEnd = scheduledEnd(workDate, start, end);
        if (scheduledStart == null || scheduledEnd == null || nowVn == null) return false;
        LocalDateTime opensAt = scheduledStart.minus(CLOCK_IN_EARLY_GRACE);
        return !nowVn.isBefore(opensAt) && nowVn.isBefore(scheduledEnd);
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

    /**
     * Người này còn đang trực quầy tại thời điểm {@code nowVn} không — dùng để quyết định có cho
     * người khác thu hồi món họ đang giữ hay không.
     *
     * <p>Còn trực = đã vào ca, chưa bấm tan ca, VÀ chưa quá giờ tan theo lịch cộng
     * {@link #OFF_DUTY_GRACE}. Vế cuối là vế quan trọng: người bỏ về mà không bấm tan ca sẽ mãi
     * "chưa tan ca" theo dữ liệu chấm công, thiếu nó thì món họ giữ bị khoá tới tận hôm sau.
     */
    public static boolean onDuty(LocalDate workDate, LocalTime start, LocalTime end,
                                 boolean checkedIn, boolean checkedOut, LocalDateTime nowVn) {
        if (!checkedIn || checkedOut) return false;
        LocalDateTime end0 = scheduledEnd(workDate, start, end);
        if (end0 == null || nowVn == null) return checkedIn && !checkedOut;   // thiếu lịch → tin chấm công
        return nowVn.isBefore(end0.plus(OFF_DUTY_GRACE));
    }
}
