package com.cafe.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Một dòng lịch đi làm trong tháng của chính nhân viên đang đăng nhập. */
public class MonthlyAttendanceRow {
    private LocalDate workDate;
    private String shiftName;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private double workHours;
    private String status;

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate v) { this.workDate = v; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String v) { this.shiftName = v; }

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

    /** Không có mốc vào ca = coi như vắng; assignment chưa chấm công có các cột attendance NULL. */
    public boolean isAbsent() { return status == null || checkInAt == null; }

    /** Vào ca rồi nhưng quên bấm tan ca — giờ chưa chốt được. */
    public boolean isOpen() { return status != null && checkInAt != null && checkOutAt == null; }

}
