package com.cafe.model;

import java.math.BigDecimal;

/** catalog.Ingredient — RAW (mua về) hoặc PREPPED (pha sẵn tại quán). */
public class Ingredient {
    private int ingredientId;
    private String name;
    private String unit;            // g, ml, cái, kg, L...
    private String ingredientType;  // RAW | PREPPED
    private Integer shelfLifeMinutes;
    private BigDecimal prepYieldQty;
    private String purchaseUnitName;
    private BigDecimal purchaseFactorToBase;
    private boolean active = true;

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getIngredientType() { return ingredientType; }
    public void setIngredientType(String ingredientType) { this.ingredientType = ingredientType; }

    public Integer getShelfLifeMinutes() { return shelfLifeMinutes; }
    public void setShelfLifeMinutes(Integer shelfLifeMinutes) { this.shelfLifeMinutes = shelfLifeMinutes; }
    public BigDecimal getPrepYieldQty() { return prepYieldQty; }
    public void setPrepYieldQty(BigDecimal prepYieldQty) { this.prepYieldQty = prepYieldQty; }
    public String getPurchaseUnitName() { return purchaseUnitName; }
    public void setPurchaseUnitName(String purchaseUnitName) { this.purchaseUnitName = purchaseUnitName; }
    public BigDecimal getPurchaseFactorToBase() { return purchaseFactorToBase; }
    public void setPurchaseFactorToBase(BigDecimal purchaseFactorToBase) {
        this.purchaseFactorToBase = purchaseFactorToBase;
    }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
