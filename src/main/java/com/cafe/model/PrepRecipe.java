package com.cafe.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** catalog.PrepRecipe - định mức nguyên liệu thô cho nguyên liệu pha sẵn. */
public class PrepRecipe {
    private int prepRecipeId;
    private int preppedIngredientId;
    private int rawIngredientId;
    private BigDecimal quantity;
    private BigDecimal yieldQty; // trường tương thích dữ liệu cũ, Admin không nhập

    // join
    private String rawIngredientName;
    private String rawIngredientUnit;

    public int getPrepRecipeId() { return prepRecipeId; }
    public void setPrepRecipeId(int prepRecipeId) { this.prepRecipeId = prepRecipeId; }

    public int getPreppedIngredientId() { return preppedIngredientId; }
    public void setPreppedIngredientId(int preppedIngredientId) { this.preppedIngredientId = preppedIngredientId; }

    public int getRawIngredientId() { return rawIngredientId; }
    public void setRawIngredientId(int rawIngredientId) { this.rawIngredientId = rawIngredientId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    /** Cho JSP — bỏ .000 thừa. */
    public String getQuantityDisplay() { return com.cafe.common.QuantityFormat.plain(quantity); }

    /** Admin recipe forms only accept and display whole-number quantities. */
    public String getQuantityIntegerDisplay() {
        return quantity == null ? "" : quantity.setScale(0, RoundingMode.DOWN).toPlainString();
    }

    public BigDecimal getYieldQty() { return yieldQty; }
    public void setYieldQty(BigDecimal yieldQty) { this.yieldQty = yieldQty; }

    /** Hiển thị gọn cho pha chế: 1000.000 → 1000, 250.500 → 250.5. */
    public String getYieldDisplay() { return com.cafe.common.QuantityFormat.plain(yieldQty); }

    public String getRawIngredientName() { return rawIngredientName; }
    public void setRawIngredientName(String rawIngredientName) { this.rawIngredientName = rawIngredientName; }

    public String getRawIngredientUnit() { return rawIngredientUnit; }
    public void setRawIngredientUnit(String rawIngredientUnit) { this.rawIngredientUnit = rawIngredientUnit; }
}
