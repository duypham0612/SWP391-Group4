package com.cafe.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Dòng bảng lương tháng — giờ từ chấm công APPROVED + lương/giờ (override được), thành tiền (M4). */
public class PayrollRow {
    private int userId;
    private String userName;
    private String roleName;
    private int approvedShifts;
    private double totalHours;                          // giờ làm hiệu lực (override nếu Manager đã chốt)
    private double computedHours;                       // giờ tính từ chấm công (tham chiếu)
    private BigDecimal hourlyRate = BigDecimal.ZERO;    // lương/giờ
    private boolean overridden;                         // đã chốt/sửa trong hr.Payroll chưa

    public int getUserId() { return userId; }
    public void setUserId(int v) { this.userId = v; }

    public String getUserName() { return userName; }
    public void setUserName(String v) { this.userName = v; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String v) { this.roleName = v; }

    public int getApprovedShifts() { return approvedShifts; }
    public void setApprovedShifts(int v) { this.approvedShifts = v; }

    public double getTotalHours() { return totalHours; }
    public void setTotalHours(double v) { this.totalHours = v; }

    public double getComputedHours() { return computedHours; }
    public void setComputedHours(double v) { this.computedHours = v; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal v) { this.hourlyRate = v == null ? BigDecimal.ZERO : v; }

    public boolean isOverridden() { return overridden; }
    public void setOverridden(boolean v) { this.overridden = v; }

    /** Thành tiền = giờ làm × lương/giờ. */
    public BigDecimal getSalary() {
        return BigDecimal.valueOf(totalHours).multiply(hourlyRate).setScale(0, RoundingMode.HALF_UP);
    }
}
