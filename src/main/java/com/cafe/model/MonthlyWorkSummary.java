package com.cafe.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tổng hợp giờ làm tháng của chính nhân viên đang đăng nhập. */
public class MonthlyWorkSummary {
    private double approvedHours;
    private double pendingHours;
    private double rejectedHours;
    private int shiftsWorked;
    private int absentCount;
    private int openCount;
    private BigDecimal lockedHours;
    private BigDecimal hourlyRate;

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

    public BigDecimal getLockedHours() { return lockedHours; }
    public void setLockedHours(BigDecimal v) { this.lockedHours = v; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal v) { this.hourlyRate = v; }

    public boolean isPayrollLocked() {
        return lockedHours != null && hourlyRate != null;
    }

    /** Tiền = giờ ĐÃ CHỐT × lương/giờ — cùng công thức PayrollRow.getSalary(). */
    public BigDecimal getLockedPay() {
        return isPayrollLocked()
                ? lockedHours.multiply(hourlyRate).setScale(0, RoundingMode.HALF_UP)
                : null;
    }

    public double getAvgHoursPerShift() {
        return shiftsWorked == 0 ? 0d : Math.round(approvedHours / shiftsWorked * 10) / 10.0;
    }

    /** Manager chốt khác chấm công là hợp lệ (bù ca, phạt muộn) — phải nói ra, không giấu. */
    public boolean isHoursMismatch() {
        return isPayrollLocked() && Math.abs(lockedHours.doubleValue() - approvedHours) > 0.1;
    }
}
