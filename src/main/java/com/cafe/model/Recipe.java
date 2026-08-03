package com.cafe.model;

import java.math.BigDecimal;

/** Một dòng catalog.Recipe dùng chung cho PRODUCT, PREPPED và MODIFIER. */
public class Recipe {
    public static final String OWNER_PRODUCT = "PRODUCT";
    public static final String OWNER_PREPPED = "PREPPED";
    public static final String OWNER_MODIFIER = "MODIFIER";

    private int recipeId;
    private String ownerType;
    private int ownerId;
    private int ingredientId;
    private BigDecimal quantity;

    // Dữ liệu join phục vụ UI và kiểm tra tồn kho.
    private String ingredientName;
    private String ingredientUnit;
    private String ingredientType;
    private BigDecimal branchQuantityOnHand;
    private BigDecimal prepYieldQty;

    public int getRecipeId() { return recipeId; }
    public void setRecipeId(int recipeId) { this.recipeId = recipeId; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String ingredientUnit) { this.ingredientUnit = ingredientUnit; }
    public String getIngredientType() { return ingredientType; }
    public void setIngredientType(String ingredientType) { this.ingredientType = ingredientType; }
    public BigDecimal getBranchQuantityOnHand() { return branchQuantityOnHand; }
    public void setBranchQuantityOnHand(BigDecimal branchQuantityOnHand) {
        this.branchQuantityOnHand = branchQuantityOnHand;
    }
    public BigDecimal getPrepYieldQty() { return prepYieldQty; }
    public void setPrepYieldQty(BigDecimal prepYieldQty) { this.prepYieldQty = prepYieldQty; }
}
