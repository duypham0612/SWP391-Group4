package com.cafe.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** catalog.ProductRecipe — 1 dòng định mức nguyên liệu của 1 product. */
public class ProductRecipe {
    private int productRecipeId;
    private int productId;
    private int ingredientId;
    private BigDecimal quantity;

    // join
    private String ingredientName;
    private String ingredientUnit;
    private String ingredientType;
    private BigDecimal branchQuantityOnHand;

    public int getProductRecipeId() { return productRecipeId; }
    public void setProductRecipeId(int productRecipeId) { this.productRecipeId = productRecipeId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    /** Cho JSP — bỏ .000 thừa. */
    public String getQuantityDisplay() { return com.cafe.common.QuantityFormat.plain(quantity); }

    /** Admin recipe forms only accept and display whole-number quantities. */
    public String getQuantityIntegerDisplay() {
        return quantity == null ? "" : quantity.setScale(0, RoundingMode.DOWN).toPlainString();
    }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String ingredientUnit) { this.ingredientUnit = ingredientUnit; }

    public String getIngredientType() { return ingredientType; }
    public void setIngredientType(String ingredientType) { this.ingredientType = ingredientType; }

    public BigDecimal getBranchQuantityOnHand() { return branchQuantityOnHand; }
    public void setBranchQuantityOnHand(BigDecimal branchQuantityOnHand) { this.branchQuantityOnHand = branchQuantityOnHand; }
}
