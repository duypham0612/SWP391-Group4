package com.cafe.model;

import java.math.BigDecimal;

/** Dòng checklist "cần pha hôm nay" (B4): một nguyên liệu PREPPED + tồn/ngưỡng tại chi nhánh. */
public class PrepChecklistRow {
    private final int ingredientId;
    private final String name;
    private final String unit;
    private final BigDecimal onHand;
    private final BigDecimal threshold;
    private final BigDecimal targetQty;
    private final boolean hasRecipe;
    private final Integer shelfLifeMinutes;

    public PrepChecklistRow(int ingredientId, String name, String unit,
                            BigDecimal onHand, BigDecimal threshold, BigDecimal targetQty,
                            boolean hasRecipe, Integer shelfLifeMinutes) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.unit = unit;
        this.onHand = onHand == null ? BigDecimal.ZERO : onHand;
        this.threshold = threshold == null ? BigDecimal.ZERO : threshold;
        this.targetQty = targetQty;
        this.hasRecipe = hasRecipe;
        this.shelfLifeMinutes = shelfLifeMinutes;
    }

    public int getIngredientId() { return ingredientId; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public BigDecimal getOnHand() { return onHand; }
    public BigDecimal getThreshold() { return threshold; }
    public BigDecimal getTargetQty() { return targetQty; }

    /** Cho JSP — bỏ .000 thừa. */
    public String getOnHandDisplay() { return com.cafe.common.QuantityFormat.plain(onHand); }
    public String getThresholdDisplay() { return com.cafe.common.QuantityFormat.plain(threshold); }
    public String getTargetQtyDisplay() { return com.cafe.common.QuantityFormat.plain(targetQty); }
    public boolean isHasRecipe() { return hasRecipe; }
    public boolean isHasTarget() { return targetQty != null; }
    public boolean isHasShelfLife() { return shelfLifeMinutes != null && shelfLifeMinutes > 0; }
    public Integer getShelfLifeMinutes() { return shelfLifeMinutes; }

    /** Tồn âm phải kiểm kê, không dùng việc pha để che chênh lệch kho. */
    public boolean isOversold() { return onHand.signum() < 0; }

    /** Cần pha khi chạm ngưỡng và đã có mục tiêu lớn hơn tồn. */
    public boolean isNeedPrep() {
        return !isOversold() && targetQty != null
                && onHand.compareTo(threshold) <= 0 && targetQty.compareTo(onHand) > 0;
    }

    public boolean isReadyToPrep() { return isNeedPrep() && hasRecipe && isHasShelfLife(); }

    public BigDecimal getSuggestedQty() {
        return isNeedPrep() ? targetQty.subtract(onHand) : BigDecimal.ZERO;
    }

    public String getSuggestedQtyDisplay() {
        return com.cafe.common.QuantityFormat.plain(getSuggestedQty());
    }

    public String getBlockedReason() {
        if (isOversold()) return "Tồn đang âm — cần Manager kiểm kê.";
        if (!isHasTarget()) return "Manager chưa đặt mức tồn mục tiêu.";
        if (!hasRecipe) return "Admin chưa khai báo công thức.";
        if (!isHasShelfLife()) return "Admin chưa đặt hạn bảo quản.";
        return "";
    }
}
