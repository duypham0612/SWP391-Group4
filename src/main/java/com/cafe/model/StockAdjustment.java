package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** inventory.StockAdjustment — điều chỉnh tồn sau kiểm kê (DiffQty computed). */
public class StockAdjustment {
    private int stockAdjustmentId;
    private int branchId;
    /** UUID nhóm các dòng cùng phiên; null = điều chỉnh lẻ (Barista báo hết / đếm lại). */
    private String countBatchId;
    private LocalDateTime countedAt;
    private Integer countedBy;
    private String countNote;
    private int ingredientId;
    private BigDecimal systemBaseQty;
    private BigDecimal actualBaseQty;
    /** Lựa chọn tạm từ form: 0=đơn vị gốc, 1=đơn vị mua; không lưu DB. */
    private int unitChoice;
    private BigDecimal countedQuantity;
    private String unitNameAtCount;
    private BigDecimal factorToBaseAtCount;
    private BigDecimal diffQty;
    private String reason;
    private int adjustedBy;
    private LocalDateTime adjustedAt;

    // join
    private String ingredientName;
    private String ingredientUnit;
    private String adjustedByName;

    public int getStockAdjustmentId() { return stockAdjustmentId; }
    public void setStockAdjustmentId(int stockAdjustmentId) { this.stockAdjustmentId = stockAdjustmentId; }

    public String getCountBatchId() { return countBatchId; }
    public void setCountBatchId(String v) { this.countBatchId = v; }
    public LocalDateTime getCountedAt() { return countedAt; }
    public void setCountedAt(LocalDateTime v) { this.countedAt = v; }
    public Integer getCountedBy() { return countedBy; }
    public void setCountedBy(Integer v) { this.countedBy = v; }
    public String getCountNote() { return countNote; }
    public void setCountNote(String v) { this.countNote = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getSystemBaseQty() { return systemBaseQty; }
    public void setSystemBaseQty(BigDecimal value) { systemBaseQty = value; }

    public BigDecimal getActualBaseQty() { return actualBaseQty; }
    public void setActualBaseQty(BigDecimal value) { actualBaseQty = value; }
    public int getUnitChoice() { return unitChoice; }
    public void setUnitChoice(int value) { unitChoice = value; }
    public BigDecimal getCountedQuantity() { return countedQuantity; }
    public void setCountedQuantity(BigDecimal value) { countedQuantity = value; }
    public String getUnitNameAtCount() { return unitNameAtCount; }
    public void setUnitNameAtCount(String value) { unitNameAtCount = value; }
    public BigDecimal getFactorToBaseAtCount() { return factorToBaseAtCount; }
    public void setFactorToBaseAtCount(BigDecimal value) { factorToBaseAtCount = value; }

    public BigDecimal getDiffQty() { return diffQty; }
    public void setDiffQty(BigDecimal diffQty) { this.diffQty = diffQty; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

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
