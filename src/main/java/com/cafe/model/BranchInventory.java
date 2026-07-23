package com.cafe.model;

import com.cafe.common.QuantityFormat;

import java.math.BigDecimal;

/** inventory.BranchInventory — số dư tồn (cache); nguồn sự thật là InventoryTransaction. */
public class BranchInventory {
    private int branchId;
    private int ingredientId;
    private BigDecimal quantityOnHand = BigDecimal.ZERO;
    private BigDecimal minThreshold = BigDecimal.ZERO;

    // join
    private String ingredientName;
    private String ingredientUnit;
    private String ingredientType;

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(BigDecimal quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public BigDecimal getMinThreshold() { return minThreshold; }
    public void setMinThreshold(BigDecimal minThreshold) { this.minThreshold = minThreshold; }

    /** Cho JSP — bỏ .000 thừa. So sánh/tính toán vẫn dùng getter BigDecimal ở trên. */
    public String getQuantityOnHandDisplay() { return QuantityFormat.plain(quantityOnHand); }
    public String getMinThresholdDisplay() { return QuantityFormat.plain(minThreshold); }

    public boolean isLow() {
        return quantityOnHand != null && minThreshold != null && quantityOnHand.compareTo(minThreshold) <= 0;
    }

    /** Tồn âm — đã bán/dùng quá tồn lý thuyết, cần kiểm kê đối soát. */
    public boolean isOversold() {
        return quantityOnHand != null && quantityOnHand.signum() < 0;
    }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String ingredientUnit) { this.ingredientUnit = ingredientUnit; }

    public String getIngredientType() { return ingredientType; }
    public void setIngredientType(String ingredientType) { this.ingredientType = ingredientType; }
}
