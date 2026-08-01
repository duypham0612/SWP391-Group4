package com.cafe.model;

import java.math.BigDecimal;

/** Một dòng nguyên liệu RAW thuộc header catalog.PrepRecipe. */
public class PrepRecipeIngredient {
    private int prepRecipeIngredientId;
    private int prepRecipeId;
    private int rawIngredientId;
    private BigDecimal quantity;

    // Dữ liệu join phục vụ UI.
    private String rawIngredientName;
    private String rawIngredientUnit;

    public int getPrepRecipeIngredientId() { return prepRecipeIngredientId; }
    public void setPrepRecipeIngredientId(int prepRecipeIngredientId) { this.prepRecipeIngredientId = prepRecipeIngredientId; }

    public int getPrepRecipeId() { return prepRecipeId; }
    public void setPrepRecipeId(int prepRecipeId) { this.prepRecipeId = prepRecipeId; }

    public int getRawIngredientId() { return rawIngredientId; }
    public void setRawIngredientId(int rawIngredientId) { this.rawIngredientId = rawIngredientId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getRawIngredientName() { return rawIngredientName; }
    public void setRawIngredientName(String rawIngredientName) { this.rawIngredientName = rawIngredientName; }

    public String getRawIngredientUnit() { return rawIngredientUnit; }
    public void setRawIngredientUnit(String rawIngredientUnit) { this.rawIngredientUnit = rawIngredientUnit; }
}
