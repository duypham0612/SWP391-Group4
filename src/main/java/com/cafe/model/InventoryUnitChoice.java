package com.cafe.model;

import java.math.BigDecimal;

/** Lựa chọn đơn vị nhập/đếm dựng từ Ingredient.Unit hoặc đơn vị mua phụ. */
public class InventoryUnitChoice {
    private int choiceCode;
    private int ingredientId;
    private String unitName;
    private BigDecimal factorToBase;
    private boolean baseUnit;

    public int getChoiceCode() { return choiceCode; }
    public void setChoiceCode(int choiceCode) { this.choiceCode = choiceCode; }
    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public BigDecimal getFactorToBase() { return factorToBase; }
    public void setFactorToBase(BigDecimal factorToBase) { this.factorToBase = factorToBase; }
    public boolean isBaseUnit() { return baseUnit; }
    public void setBaseUnit(boolean baseUnit) { this.baseUnit = baseUnit; }
}
