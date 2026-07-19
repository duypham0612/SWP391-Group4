package com.cafe.model;

import com.cafe.common.BusinessDay;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Trạng thái chấm công ca hôm nay cho Barista/Cashier. */
public class ShiftClockStatus {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private boolean hasAssignment;
    private boolean canClockIn;
    private boolean canClockOut;
    private boolean clockedOut;
    private String statusText;
    private String templateName;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private double workHours;

    public boolean isHasAssignment() { return hasAssignment; }
    public void setHasAssignment(boolean hasAssignment) { this.hasAssignment = hasAssignment; }

    public boolean isCanClockIn() { return canClockIn; }
    public void setCanClockIn(boolean canClockIn) { this.canClockIn = canClockIn; }

    public boolean isCanClockOut() { return canClockOut; }
    public void setCanClockOut(boolean canClockOut) { this.canClockOut = canClockOut; }

    public boolean isClockedOut() { return clockedOut; }
    public void setClockedOut(boolean clockedOut) { this.clockedOut = clockedOut; }

    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime checkInAt) { this.checkInAt = checkInAt; }

    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime checkOutAt) { this.checkOutAt = checkOutAt; }

    public double getWorkHours() { return workHours; }
    public void setWorkHours(double workHours) { this.workHours = workHours; }

    public String getShiftTimeDisplay() {
        if (startTime == null || endTime == null) return "";
        return startTime.format(TIME_FMT) + " - " + endTime.format(TIME_FMT);
    }

    public String getCheckInDisplay() {
        return BusinessDay.fmtDateTimeVn(checkInAt);
    }

    public String getCheckOutDisplay() {
        return BusinessDay.fmtDateTimeVn(checkOutAt);
    }

    public String getWorkHoursDisplay() {
        return String.format(Locale.US, "%.1f", workHours);
    }
}
