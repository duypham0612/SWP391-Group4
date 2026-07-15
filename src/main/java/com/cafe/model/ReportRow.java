package com.cafe.model;

import java.math.BigDecimal;

/**
 * Dòng báo cáo đa dụng (Phase 7) — dùng cho doanh thu theo chi nhánh / theo ngày /
 * top sản phẩm / theo hình thức thanh toán. label = tên hàng; metric phụ tuỳ ngữ cảnh.
 */
public class ReportRow {
    private String label;          // tên chi nhánh / tên món / ngày / hình thức
    private int count;             // số bill / số ly bán
    private BigDecimal amount;     // doanh thu

    public ReportRow() {}
    public ReportRow(String label, int count, BigDecimal amount) {
        this.label = label; this.count = count; this.amount = amount;
    }

    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }

    public int getCount() { return count; }
    public void setCount(int v) { this.count = v; }

    public BigDecimal getAmount() { return amount == null ? BigDecimal.ZERO : amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
}
