package com.cafe.model;

import com.cafe.common.BusinessDay;
import com.cafe.common.ShiftWindow;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** hr.ShiftAssignment — ca làm được nhập trực tiếp khi phân công (+ join để hiển thị). */
public class ShiftAssignment {
    private int shiftAssignmentId;
    private int branchId;
    private int userId;
    private LocalDate workDate;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal hourlyRateSnapshot;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String attendanceStatus; // NULL = chưa chấm công; PENDING | APPROVED | REJECTED
    private Integer approvedBy;
    private LocalDateTime approvedAt;

    // join hiển thị
    private String userName;
    private String roleCode;
    private String userPhone;
    private String branchName;
    private String approverName;

    public int getShiftAssignmentId() { return shiftAssignmentId; }
    public void setShiftAssignmentId(int v) { this.shiftAssignmentId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public int getUserId() { return userId; }
    public void setUserId(int v) { this.userId = v; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate v) { this.workDate = v; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String v) { this.shiftName = v; }

    public LocalDateTime getCheckInAt() { return checkInAt; }
    public void setCheckInAt(LocalDateTime v) { this.checkInAt = v; }

    public LocalDateTime getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(LocalDateTime v) { this.checkOutAt = v; }

    public String getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(String v) { this.attendanceStatus = v; }

    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer v) { this.approvedBy = v; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime v) { this.approvedAt = v; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime v) { this.startTime = v; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime v) { this.endTime = v; }

    public BigDecimal getHourlyRateSnapshot() { return hourlyRateSnapshot; }
    public void setHourlyRateSnapshot(BigDecimal v) { this.hourlyRateSnapshot = v; }

    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String v) { this.roleCode = v; }

    public String getRoleName() {
        if (roleCode == null) return null;
        return switch (roleCode) {
            case "ADMIN" -> "Quản trị hệ thống";
            case "BRANCH_MANAGER" -> "Quản lý chi nhánh";
            case "CASHIER" -> "Thu ngân";
            case "BARISTA" -> "Pha chế";
            default -> roleCode;
        };
    }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String v) { this.userPhone = v; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String v) { this.branchName = v; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String v) { this.approverName = v; }

    public double getWorkHours() {
        if (checkInAt == null || checkOutAt == null) return 0d;
        long minutes = Duration.between(checkInAt, checkOutAt).toMinutes();
        if (minutes < 0) return 0d;
        return Math.round(minutes / 60.0 * 10) / 10.0;
    }

    public long getLateMinutes() {
        LocalDateTime scheduledStart = ShiftWindow.scheduledStart(workDate, startTime);
        LocalDateTime actualCheckIn = BusinessDay.toVn(checkInAt);
        if (scheduledStart == null || actualCheckIn == null) return 0L;
        long minutes = Duration.between(scheduledStart, actualCheckIn).toMinutes();
        return minutes > 0 ? minutes : 0L;
    }

    public long getEarlyArrivalMinutes() {
        LocalDateTime scheduledStart = ShiftWindow.scheduledStart(workDate, startTime);
        LocalDateTime actualCheckIn = BusinessDay.toVn(checkInAt);
        if (scheduledStart == null || actualCheckIn == null) return 0L;
        long minutes = Duration.between(actualCheckIn, scheduledStart).toMinutes();
        return minutes > 0 ? minutes : 0L;
    }

    public long getEarlyLeaveMinutes() {
        LocalDateTime scheduledEnd = ShiftWindow.scheduledEnd(workDate, startTime, endTime);
        LocalDateTime actualCheckOut = BusinessDay.toVn(checkOutAt);
        if (scheduledEnd == null || actualCheckOut == null) return 0L;
        long minutes = Duration.between(actualCheckOut, scheduledEnd).toMinutes();
        return minutes > 0 ? minutes : 0L;
    }

    public boolean isLate() { return getLateMinutes() > 0; }
    public boolean isEarlyArrival() { return getEarlyArrivalMinutes() > 0; }
    public boolean isEarlyLeave() { return getEarlyLeaveMinutes() > 0; }
}
