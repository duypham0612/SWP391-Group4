package com.cafe.common;

import com.cafe.model.ModifierIngredientImpact;
import com.cafe.model.ProductRecipe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/** ★ Test TRƯỚC cho Modifier-Aware Auto-Deduction (logic rủi ro — CLAUDE.md mục 5). */
class DeductionCalculatorTest {

    private static ProductRecipe pr(int ing, String qty) {
        ProductRecipe r = new ProductRecipe();
        r.setIngredientId(ing); r.setQuantity(new BigDecimal(qty)); return r;
    }
    private static ModifierIngredientImpact mi(int ing, String delta) {
        ModifierIngredientImpact m = new ModifierIngredientImpact();
        m.setIngredientId(ing); m.setQtyDelta(new BigDecimal(delta)); return m;
    }
    private static void assertQty(Map<Integer, BigDecimal> req, int ing, String expected) {
        assertEquals(0, req.get(ing).compareTo(new BigDecimal(expected)),
                "ingredient " + ing + " expected " + expected + " but was " + req.get(ing));
    }

    /** ★ Test BẮT BUỘC: pha 1 ly Cold Brew (PREPPED) → chỉ trừ Cold Brew(6)+Đá(4), KHÔNG trừ cà phê hạt(1). */
    @Test
    void coldBrew_deducts_prepped_only_not_raw_coffee_twice() {
        // Cold Brew = 180ml Cold Brew(ing 6, PREPPED) + 100g đá(ing 4)
        Map<Integer, BigDecimal> req = DeductionCalculator.computeRequired(
                List.of(pr(6, "180"), pr(4, "100")), List.of(), 1);

        assertQty(req, 6, "180");
        assertQty(req, 4, "100");
        assertNull(req.get(1), "KHÔNG được trừ cà phê hạt (ing 1) — raw đã trừ ở PrepBatch");
        assertEquals(2, req.size());
    }

    /** Modifier "Thêm shot" (+18g cà phê) cho 1 ly Cà phê sữa → cà phê 18+18=36g, sữa 30ml. */
    @Test
    void milkCoffee_withExtraShot_addsImpact() {
        Map<Integer, BigDecimal> req = DeductionCalculator.computeRequired(
                List.of(pr(1, "18"), pr(2, "30")), List.of(mi(1, "18")), 1);
        assertQty(req, 1, "36");
        assertQty(req, 2, "30");
    }

    /** Số lượng > 1 nhân toàn bộ định mức (gồm modifier). */
    @Test
    void quantity_multiplies_everything() {
        Map<Integer, BigDecimal> req = DeductionCalculator.computeRequired(
                List.of(pr(1, "18"), pr(2, "30")), List.of(mi(1, "18")), 3);
        assertQty(req, 1, "108");  // (18+18)*3
        assertQty(req, 2, "90");   // 30*3
    }

    /** Modifier âm làm net ≤ 0 thì bỏ qua, không cộng tồn ngược. */
    @Test
    void negative_net_ingredient_is_dropped() {
        Map<Integer, BigDecimal> req = DeductionCalculator.computeRequired(
                List.of(pr(2, "30")), List.of(mi(2, "-30")), 1);
        assertFalse(req.containsKey(2), "net 0 → không trừ");
    }
}
