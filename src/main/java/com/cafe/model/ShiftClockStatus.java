package com.cafe.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Trạng thái chấm công ca hôm nay cho Barista/Cashier. */
public class ShiftClockStatus {
    private boolean hasAssignment;
    private boolean canClockIn;
    private boolean canClockOut;
    private boolean clockedOut;
    private boolean waitingForStart;
    private boolean clockInExpired;
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

    public boolean isWaitingForStart() { return waitingForStart; }
    public void setWaitingForStart(boolean waitingForStart) { this.waitingForStart = waitingForStart; }

    public boolean isClockInExpired() { return clockInExpired; }
    public void setClockInExpired(boolean clockInExpired) { this.clockInExpired = clockInExpired; }

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

}
