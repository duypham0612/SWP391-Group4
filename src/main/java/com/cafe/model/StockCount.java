package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * inventory.StockCount — một biên bản kiểm kê (header của inventory.StockAdjustment).
 *
 * <p>{@code lineCount} và {@code totalDiffQty} là số ĐỌC RA từ chi tiết, không phải cột lưu
 * trong bảng — tránh thêm một cache nữa có thể lệch với dòng chi tiết.
 */
public class StockCount {
    private int stockCountId;
    private int branchId;
    private int countedBy;
    private LocalDateTime countedAt;
    private String note;

    // join / tính khi đọc
    private String countedByName;
    private int lineCount;
    private BigDecimal totalDiffQty;

    public int getStockCountId() { return stockCountId; }
    public void setStockCountId(int v) { this.stockCountId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public int getCountedBy() { return countedBy; }
    public void setCountedBy(int v) { this.countedBy = v; }

    public LocalDateTime getCountedAt() { return countedAt; }
    public void setCountedAt(LocalDateTime v) { this.countedAt = v; }

    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }

    public String getCountedByName() { return countedByName; }
    public void setCountedByName(String v) { this.countedByName = v; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int v) { this.lineCount = v; }

    public BigDecimal getTotalDiffQty() { return totalDiffQty; }
    public void setTotalDiffQty(BigDecimal v) { this.totalDiffQty = v; }

}
