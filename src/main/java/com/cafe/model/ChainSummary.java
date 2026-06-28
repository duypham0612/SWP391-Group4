package com.cafe.model;

import java.math.BigDecimal;

/** Tổng quan doanh thu toàn chuỗi (Phase 7 · Admin). */
public class ChainSummary {
    private int paidBills;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal vat = BigDecimal.ZERO;
    private int todayBills;
    private BigDecimal todayRevenue = BigDecimal.ZERO;

    public int getPaidBills() { return paidBills; }
    public void setPaidBills(int v) { this.paidBills = v; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal v) { this.revenue = v; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal v) { this.discount = v; }

    public BigDecimal getVat() { return vat; }
    public void setVat(BigDecimal v) { this.vat = v; }

    public int getTodayBills() { return todayBills; }
    public void setTodayBills(int v) { this.todayBills = v; }

    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal v) { this.todayRevenue = v; }
}
