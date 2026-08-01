package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** inventory.StockAdjustment — điều chỉnh tồn sau kiểm kê (DiffQty computed). */
public class StockAdjustment {
    private int stockAdjustmentId;
    private int branchId;
    /** Biên bản kiểm kê chứa dòng này; null = điều chỉnh lẻ (Barista báo hết / đếm lại). */
    private Integer stockCountId;
    private int ingredientId;
    private BigDecimal systemBaseQty;
    private BigDecimal actualBaseQty;
    private int ingredientUnitConversionId;
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

    public Integer getStockCountId() { return stockCountId; }
    public void setStockCountId(Integer v) { this.stockCountId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getSystemBaseQty() { return systemBaseQty; }
    public void setSystemBaseQty(BigDecimal value) { systemBaseQty = value; }

    public BigDecimal getActualBaseQty() { return actualBaseQty; }
    public void setActualBaseQty(BigDecimal value) { actualBaseQty = value; }
    public int getIngredientUnitConversionId() { return ingredientUnitConversionId; }
    public void setIngredientUnitConversionId(int value) { ingredientUnitConversionId = value; }
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
