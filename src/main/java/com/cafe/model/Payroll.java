package com.cafe.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/** hr.Payroll — chốt lương một nhân viên theo tháng. */
public class Payroll {
    private int payrollId;
    private int branchId;
    private int userId;
    private YearMonth payrollMonth;
    private BigDecimal workedHours = BigDecimal.ZERO;
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    public int getPayrollId() { return payrollId; }
    public void setPayrollId(int v) { this.payrollId = v; }
    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }
    public int getUserId() { return userId; }
    public void setUserId(int v) { this.userId = v; }
    public YearMonth getPayrollMonth() { return payrollMonth; }
    public void setPayrollMonth(YearMonth v) { this.payrollMonth = v; }
    public BigDecimal getWorkedHours() { return workedHours; }
    public void setWorkedHours(BigDecimal v) { this.workedHours = v; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal v) { this.hourlyRate = v; }
}
