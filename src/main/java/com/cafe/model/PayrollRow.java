package com.cafe.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Dòng bảng lương runtime từ các ca APPROVED trong tháng. */
public class PayrollRow {
    private int userId;
    private String userName;
    private String roleName;
    private int approvedShifts;
    private double totalHours;
    private BigDecimal hourlyRate = BigDecimal.ZERO; // bình quân gia quyền nếu có nhiều snapshot
    private BigDecimal salary = BigDecimal.ZERO;     // tổng trực tiếp theo từng ca

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

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal v) { this.hourlyRate = v == null ? BigDecimal.ZERO : v; }

    public BigDecimal getSalary() { return salary.setScale(0, RoundingMode.HALF_UP); }
    public void setSalary(BigDecimal v) { this.salary = v == null ? BigDecimal.ZERO : v; }
}
