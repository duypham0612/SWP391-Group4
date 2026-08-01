package com.cafe.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Một dòng lịch đi làm trong tháng của chính nhân viên đang đăng nhập. */
public class MonthlyAttendanceRow {
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

}
