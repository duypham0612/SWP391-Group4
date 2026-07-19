package com.cafe.model;

import com.cafe.common.BusinessDay;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Một dòng lịch đi làm trong tháng của chính nhân viên đang đăng nhập. */
public class MonthlyAttendanceRow {
    private static final DateTimeFormatter D_M = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private LocalDate workDate;
    private String templateName;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private double workHours;
    private String status;

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate v) { this.workDate = v; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String v) { this.templateName = v; }

    public LocalTime getShiftStart() { return shiftStart; }
    public void setShiftStart(LocalTime v) { this.shiftStart = v; }

    public LocalTime getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(LocalTime v) { this.shiftEnd = v; }

    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime v) { this.checkInAt = v; }

    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime v) { this.checkOutAt = v; }

    public double getWorkHours() { return workHours; }
    public void setWorkHours(double v) { this.workHours = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    /** Không có mốc vào ca = coi như vắng, kể cả khi đã có dòng Attendance rỗng. */
    public boolean isAbsent() { return checkInAt == null; }

    /** Vào ca rồi nhưng quên bấm tan ca — giờ chưa chốt được. */
    public boolean isOpen() { return checkInAt != null && checkOutAt == null; }

    public String getStateLabel() {
        if (isAbsent()) return "Vắng";
        if (isOpen()) return "Chưa tan ca";
        if ("APPROVED".equals(status)) return "Đã duyệt";
        if ("REJECTED".equals(status)) return "Bị từ chối";
        return "Chờ duyệt";
    }

    public String getStateBadge() {
        if (isAbsent()) return "badge-served";
        if (isOpen()) return "badge-waiting";
        if ("APPROVED".equals(status)) return "badge-ready";
        if ("REJECTED".equals(status)) return "badge-cancelled";
        return "badge-waiting";
    }

    public String getWorkDateDisplay() {
        return workDate == null ? "" : workDate.format(D_M);
    }

    public String getCheckInDisplay() {
        return BusinessDay.fmtTimeVn(checkInAt);
    }

    public String getCheckOutDisplay() {
        return BusinessDay.fmtTimeVn(checkOutAt);
    }

    public String getShiftTimeDisplay() {
        if (shiftStart == null || shiftEnd == null) return "";
        return shiftStart.format(TIME_FMT) + " - " + shiftEnd.format(TIME_FMT);
    }

    /** "-" thay vì "0.0" khi chưa chốt được giờ — 0.0 trông như đi làm mà không được tính. */
    public String getWorkHoursDisplay() {
        return (isAbsent() || isOpen()) ? "-" : String.format(Locale.US, "%.1f", workHours);
    }
}
