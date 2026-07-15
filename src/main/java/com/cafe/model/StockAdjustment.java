package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** inventory.StockAdjustment — điều chỉnh tồn sau kiểm kê (DiffQty computed). */
public class StockAdjustment {
    private int stockAdjustmentId;
    private int branchId;
    private int ingredientId;
    private BigDecimal systemQty;
    private BigDecimal actualQty;
    private BigDecimal diffQty;
    private String reason;
    private String unit;            // đơn vị đếm per-line (vd "Túi"); null = dùng đơn vị gốc nguyên liệu
    private int adjustedBy;
    private LocalDateTime adjustedAt;

    // join
    private String ingredientName;
    private String ingredientUnit;
    private String adjustedByName;

    public int getStockAdjustmentId() { return stockAdjustmentId; }
    public void setStockAdjustmentId(int stockAdjustmentId) { this.stockAdjustmentId = stockAdjustmentId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getSystemQty() { return systemQty; }
    public void setSystemQty(BigDecimal systemQty) { this.systemQty = systemQty; }

    public BigDecimal getActualQty() { return actualQty; }
    public void setActualQty(BigDecimal actualQty) { this.actualQty = actualQty; }

    public BigDecimal getDiffQty() { return diffQty; }
    public void setDiffQty(BigDecimal diffQty) { this.diffQty = diffQty; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    /** Đơn vị hiển thị: ưu tiên đơn vị đếm per-line, fallback về đơn vị gốc nguyên liệu. */
    public String getDisplayUnit() { return (unit == null || unit.isBlank()) ? ingredientUnit : unit; }

    public int getAdjustedBy() { return adjustedBy; }
    public void setAdjustedBy(int adjustedBy) { this.adjustedBy = adjustedBy; }

    public LocalDateTime getAdjustedAt() { return adjustedAt; }
    public void setAdjustedAt(LocalDateTime adjustedAt) { this.adjustedAt = adjustedAt; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String ingredientUnit) { this.ingredientUnit = ingredientUnit; }

    public String getAdjustedByName() { return adjustedByName; }
    public void setAdjustedByName(String adjustedByName) { this.adjustedByName = adjustedByName; }
}
