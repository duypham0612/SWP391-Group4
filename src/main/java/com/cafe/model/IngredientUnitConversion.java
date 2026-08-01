package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** catalog.IngredientUnitConversion — quy đổi đơn vị nhập/đếm về đơn vị gốc Ingredient.Unit. */
public class IngredientUnitConversion {
    private int ingredientUnitConversionId;
    private int ingredientId;
    private String unitName;
    private BigDecimal factorToBase;
    private boolean baseUnit;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer updatedBy;

    public int getIngredientUnitConversionId() { return ingredientUnitConversionId; }
    public void setIngredientUnitConversionId(int value) { ingredientUnitConversionId = value; }
    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int value) { ingredientId = value; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String value) { unitName = value; }
    public BigDecimal getFactorToBase() { return factorToBase; }
    public void setFactorToBase(BigDecimal value) { factorToBase = value; }
    public boolean isBaseUnit() { return baseUnit; }
    public void setBaseUnit(boolean value) { baseUnit = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
    public Integer getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Integer value) { updatedBy = value; }
}
