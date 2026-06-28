package com.cafe.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** hr.Attendance — chấm công 1 phân công ca (+ join để hiển thị & tính giờ). */
public class Attendance {
    private int attendanceId;
    private int shiftAssignmentId;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String status;          // PENDING | APPROVED | REJECTED
    private Integer approvedBy;

    // join hiển thị
    private LocalDate workDate;
    private String templateName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String userName;
    private int userId;
    private String approverName;

    public int getAttendanceId() { return attendanceId; }
    public void setAttendanceId(int v) { this.attendanceId = v; }

    public int getShiftAssignmentId() { return shiftAssignmentId; }
    public void setShiftAssignmentId(int v) { this.shiftAssignmentId = v; }

    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime v) { this.checkInAt = v; }

    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime v) { this.checkOutAt = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer v) { this.approvedBy = v; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate v) { this.workDate = v; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String v) { this.templateName = v; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime v) { this.startTime = v; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime v) { this.endTime = v; }

    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }

    public int getUserId() { return userId; }
    public void setUserId(int v) { this.userId = v; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String v) { this.approverName = v; }

    /** Số giờ làm thực tế = checkOut - checkIn (0 nếu thiếu). Hiển thị 1 chữ số thập phân. */
    public double getWorkHours() {
        if (checkInAt == null || checkOutAt == null) return 0d;
        long minutes = Duration.between(checkInAt, checkOutAt).toMinutes();
        if (minutes < 0) return 0d;
        return Math.round(minutes / 60.0 * 10) / 10.0;
    }
}
