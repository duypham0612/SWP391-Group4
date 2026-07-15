package com.cafe.model;

import java.math.BigDecimal;

/** hr.Payroll — chốt lương 1 nhân viên theo tháng (giờ làm + lương/giờ, override được). */
public class Payroll {
    private int payrollId;
    private int branchId;
    private int userId;
    private String payMonth;            // 'yyyy-MM'
    private BigDecimal workedHours = BigDecimal.ZERO;
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    public int getPayrollId() { return payrollId; }
    public void setPayrollId(int v) { this.payrollId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public int getUserId() { return userId; }
    public void setUserId(int v) { this.userId = v; }

    public String getPayMonth() { return payMonth; }
    public void setPayMonth(String v) { this.payMonth = v; }

    public BigDecimal getWorkedHours() { return workedHours; }
    public void setWorkedHours(BigDecimal v) { this.workedHours = v; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal v) { this.hourlyRate = v; }
}
