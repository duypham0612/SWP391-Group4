package com.cafe.common;

import com.cafe.model.PrepRecipe;
import com.cafe.model.PrepRecipeIngredient;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Công thức tiêu hao RAW khi tạo/đảo mẻ PREPPED. */
public final class PrepConsumptionCalculator {
    private PrepConsumptionCalculator() {}

    public static BigDecimal consumedRaw(BigDecimal qtyProduced, PrepRecipe recipe,
                                         PrepRecipeIngredient ingredient) {
        return qtyProduced.divide(recipe.getYieldQty(), 6, RoundingMode.HALF_UP)
                .multiply(ingredient.getQuantity());
    }
}
