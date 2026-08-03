package com.cafe.common;

import com.cafe.model.Recipe;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Công thức tiêu hao RAW khi tạo/đảo mẻ PREPPED. */
public final class PrepConsumptionCalculator {
    private PrepConsumptionCalculator() {}

    public static BigDecimal consumedRaw(BigDecimal qtyProduced, BigDecimal prepYieldQty,
                                         Recipe ingredient) {
        return qtyProduced.divide(prepYieldQty, 6, RoundingMode.HALF_UP)
                .multiply(ingredient.getQuantity());
    }
}
