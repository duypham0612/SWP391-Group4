package com.cafe.model;

import java.math.BigDecimal;

/** Dòng checklist "cần pha hôm nay" (B4): một nguyên liệu PREPPED + tồn/ngưỡng tại chi nhánh. */
public class PrepChecklistRow {
    private final int ingredientId;
    private final String name;
    private final String unit;
    private final BigDecimal onHand;
    private final BigDecimal threshold;
    private final boolean hasRecipe;

    public PrepChecklistRow(int ingredientId, String name, String unit,
                            BigDecimal onHand, BigDecimal threshold, boolean hasRecipe) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.unit = unit;
        this.onHand = onHand == null ? BigDecimal.ZERO : onHand;
        this.threshold = threshold == null ? BigDecimal.ZERO : threshold;
        this.hasRecipe = hasRecipe;
    }

    public int getIngredientId() { return ingredientId; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public BigDecimal getOnHand() { return onHand; }
    public BigDecimal getThreshold() { return threshold; }
    public boolean isHasRecipe() { return hasRecipe; }

    /** Cần pha khi tồn ≤ ngưỡng tối thiểu. */
    public boolean isNeedPrep() { return onHand.compareTo(threshold) <= 0; }
}
