package com.cafe.model;

import java.math.BigDecimal;
import java.util.List;

/** Workbench recommendation for one PREPPED ingredient. */
public class PrepRecommendation {
    private final int ingredientId;
    private final String name;
    private final String unit;
    private final BigDecimal onHand;
    private final BigDecimal minThreshold;
    private final BigDecimal openOrderDemand;
    private final BigDecimal recommendedQuantity;
    private final boolean hasRecipe;
    private final List<String> rawShortfalls;

    public PrepRecommendation(int ingredientId, String name, String unit, BigDecimal onHand,
                              BigDecimal minThreshold, BigDecimal openOrderDemand, boolean hasRecipe,
                              List<String> rawShortfalls) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.unit = unit;
        this.onHand = zero(onHand);
        this.minThreshold = zero(minThreshold);
        this.openOrderDemand = zero(openOrderDemand);
        this.recommendedQuantity = calculate(this.onHand, this.minThreshold, this.openOrderDemand);
        this.hasRecipe = hasRecipe;
        this.rawShortfalls = rawShortfalls == null ? List.of() : List.copyOf(rawShortfalls);
    }

    public static BigDecimal calculate(BigDecimal onHand, BigDecimal threshold, BigDecimal demand) {
        BigDecimal value = zero(threshold).add(zero(demand)).subtract(zero(onHand));
        return value.signum() > 0 ? value : BigDecimal.ZERO;
    }

    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    public int getIngredientId() { return ingredientId; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public BigDecimal getOnHand() { return onHand; }
    public BigDecimal getMinThreshold() { return minThreshold; }
    public BigDecimal getOpenOrderDemand() { return openOrderDemand; }
    public BigDecimal getRecommendedQuantity() { return recommendedQuantity; }
    public boolean isHasRecipe() { return hasRecipe; }
    public List<String> getRawShortfalls() { return rawShortfalls; }
    public boolean isNeedPrep() { return recommendedQuantity.signum() > 0; }
    public boolean isCanPrep() { return hasRecipe && rawShortfalls.isEmpty(); }
}
