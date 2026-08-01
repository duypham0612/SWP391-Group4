package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Header công thức cho một nguyên liệu PREPPED. */
public class PrepRecipe {
    private int prepRecipeId;
    private int preppedIngredientId;
    private BigDecimal yieldQty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<PrepRecipeIngredient> ingredients = new ArrayList<>();

    public int getPrepRecipeId() { return prepRecipeId; }
    public void setPrepRecipeId(int prepRecipeId) { this.prepRecipeId = prepRecipeId; }

    public int getPreppedIngredientId() { return preppedIngredientId; }
    public void setPreppedIngredientId(int preppedIngredientId) { this.preppedIngredientId = preppedIngredientId; }

    public BigDecimal getYieldQty() { return yieldQty; }
    public void setYieldQty(BigDecimal yieldQty) { this.yieldQty = yieldQty; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<PrepRecipeIngredient> getIngredients() { return ingredients; }
}
