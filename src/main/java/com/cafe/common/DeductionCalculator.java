package com.cafe.common;

import com.cafe.model.ModifierIngredientImpact;
import com.cafe.model.ProductRecipe;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ★ Modifier-Aware Auto-Deduction — LÕI rủi ro cao (CLAUDE.md mục 5). Logic THUẦN, không DB → unit-test trước.
 *
 * required[ingredient] = Σ(recipe.qty) + Σ(modifier.qtyDelta), rồi × số lượng dòng đơn.
 *
 * Chống double-count RAW/PREPPED: KHÔNG phân nhánh ở đây. Cứ trừ đúng ingredient mà công thức
 * tham chiếu (Cold Brew PREPPED → trừ tồn Cold Brew, KHÔNG trừ cà phê hạt lần 2 — raw đã bị trừ ở PrepBatch).
 * Modifier QtyDelta có thể âm (đổi/bớt nguyên liệu); net ≤ 0 thì bỏ qua (không cộng tồn ngược).
 */
public final class DeductionCalculator {
    private DeductionCalculator() {}

    public static Map<Integer, BigDecimal> computeRequired(
            List<ProductRecipe> recipe,
            List<ModifierIngredientImpact> impacts,
            int orderItemQty) {

        Map<Integer, BigDecimal> perUnit = new LinkedHashMap<>();
        if (recipe != null) {
            for (ProductRecipe r : recipe) {
                perUnit.merge(r.getIngredientId(), r.getQuantity(), BigDecimal::add);
            }
        }
        if (impacts != null) {
            for (ModifierIngredientImpact m : impacts) {
                perUnit.merge(m.getIngredientId(), m.getQtyDelta(), BigDecimal::add);
            }
        }

        BigDecimal qty = BigDecimal.valueOf(orderItemQty);
        Map<Integer, BigDecimal> required = new LinkedHashMap<>();
        for (Map.Entry<Integer, BigDecimal> e : perUnit.entrySet()) {
            if (e.getValue().signum() <= 0) continue;          // bỏ ingredient bị bớt hết
            required.put(e.getKey(), e.getValue().multiply(qty));
        }
        return required;
    }
}
