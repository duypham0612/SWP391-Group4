package com.cafe.model;

/** Tổng hợp giờ làm tháng của chính nhân viên đang đăng nhập. */
public class MonthlyWorkSummary {
    private double approvedHours;
    private double pendingHours;
    private double rejectedHours;
    private int shiftsWorked;
    private int absentCount;
    private int openCount;

    public double getApprovedHours() { return approvedHours; }
    public void setApprovedHours(double v) { this.approvedHours = v; }

    public double getPendingHours() { return pendingHours; }
    public void setPendingHours(double v) { this.pendingHours = v; }

    public double getRejectedHours() { return rejectedHours; }
    public void setRejectedHours(double v) { this.rejectedHours = v; }

    public int getShiftsWorked() { return shiftsWorked; }
    public void setShiftsWorked(int v) { this.shiftsWorked = v; }

    public int getAbsentCount() { return absentCount; }
    public void setAbsentCount(int v) { this.absentCount = v; }

    public int getOpenCount() { return openCount; }
    public void setOpenCount(int v) { this.openCount = v; }

    public double getAvgHoursPerShift() {
        return shiftsWorked == 0 ? 0d : Math.round(approvedHours / shiftsWorked * 10) / 10.0;
    }

}
