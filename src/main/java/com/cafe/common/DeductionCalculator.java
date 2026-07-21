package com.cafe.common;

import com.cafe.model.ProductRecipe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auto-deduction by product recipe. Pure logic, no DB.
 *
 * required[ingredient] = Σ(recipe.qty đã chỉnh theo size/đá/đường) × số lượng dòng đơn.
 *
 * Chống double-count RAW/PREPPED: KHÔNG phân nhánh ở đây. Cứ trừ đúng ingredient mà công thức
 * tham chiếu (Cold Brew PREPPED → trừ tồn Cold Brew, KHÔNG trừ cà phê hạt lần 2 — raw đã bị trừ ở PrepBatch).
 */
public final class DeductionCalculator {
    private DeductionCalculator() {}

    public static Map<Integer, BigDecimal> computeRequired(
            List<ProductRecipe> recipe,
            int orderItemQty) {
        return computeRequired(recipe, orderItemQty, "M", "Bình thường", "100%");
    }

    public static Map<Integer, BigDecimal> computeRequired(
            List<ProductRecipe> recipe,
            int orderItemQty,
            String size,
            String iceLevel,
            String sugarLevel) {

        Map<Integer, BigDecimal> perUnit = new LinkedHashMap<>();
        BigDecimal sizeFactor = sizeFactor(size);
        BigDecimal sugarFactor = sugarFactor(sugarLevel);
        BigDecimal iceQty = iceQuantity(iceLevel);
        if (recipe != null) {
            for (ProductRecipe r : recipe) {
                BigDecimal quantity = r.getQuantity().multiply(sizeFactor);
                String name = r.getIngredientName() == null ? "" : r.getIngredientName().trim().toLowerCase(java.util.Locale.ROOT);
                if ("đá".equals(name) || "da".equals(name)) {
                    quantity = iceQty;
                } else if ("đường".equals(name) || "duong".equals(name)) {
                    quantity = quantity.multiply(sugarFactor);
                }
                perUnit.merge(r.getIngredientId(), quantity, BigDecimal::add);
            }
        }

        BigDecimal qty = BigDecimal.valueOf(orderItemQty);
        Map<Integer, BigDecimal> required = new LinkedHashMap<>();
        for (Map.Entry<Integer, BigDecimal> e : perUnit.entrySet()) {
            if (e.getValue().signum() <= 0) continue;          // bỏ ingredient bị bớt hết
            required.put(e.getKey(), e.getValue().multiply(qty).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros());
        }
        return required;
    }

    private static BigDecimal sizeFactor(String size) {
        if ("S".equalsIgnoreCase(size)) return BigDecimal.ONE;
        if ("L".equalsIgnoreCase(size)) return new BigDecimal("1.4");
        return new BigDecimal("1.2");
    }

    private static BigDecimal iceQuantity(String iceLevel) {
        if ("Không đá".equals(iceLevel)) return BigDecimal.ZERO;
        if ("Ít đá".equals(iceLevel)) return new BigDecimal("50");
        if ("Nhiều đá".equals(iceLevel)) return new BigDecimal("150");
        return new BigDecimal("100");
    }

    private static BigDecimal sugarFactor(String sugarLevel) {
        if (sugarLevel == null || sugarLevel.isBlank()) return BigDecimal.ONE;
        String value = sugarLevel.trim().replace("%", "");
        try {
            return new BigDecimal(value).divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ONE;
        }
    }
}
