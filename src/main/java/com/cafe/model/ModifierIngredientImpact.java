package com.cafe.model;

import java.math.BigDecimal;

/** catalog.ModifierIngredientImpact — option ảnh hưởng định mức nguyên liệu (QtyDelta +/-). */
public class ModifierIngredientImpact {
    private int impactId;
    private int modifierOptionId;
    private int ingredientId;
    private BigDecimal qtyDelta;

    // join
    private String ingredientName;
    private String ingredientUnit;
    private String ingredientType;

    public int getImpactId() { return impactId; }
    public void setImpactId(int impactId) { this.impactId = impactId; }

    public int getModifierOptionId() { return modifierOptionId; }
    public void setModifierOptionId(int modifierOptionId) { this.modifierOptionId = modifierOptionId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getQtyDelta() { return qtyDelta; }
    public void setQtyDelta(BigDecimal qtyDelta) { this.qtyDelta = qtyDelta; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String ingredientUnit) { this.ingredientUnit = ingredientUnit; }

    public String getIngredientType() { return ingredientType; }
    public void setIngredientType(String ingredientType) { this.ingredientType = ingredientType; }
}
