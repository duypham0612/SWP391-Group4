package com.cafe.common;

import com.cafe.model.ProductRecipe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DeductionCalculatorTest {

    @Test
    void applies_size_ice_and_sugar_to_recipe() {
        Map<Integer, BigDecimal> required = DeductionCalculator.computeRequired(
                java.util.List.of(
                        recipe(1, "Cà phê", "18"),
                        recipe(2, "Sữa", "30"),
                        recipe(3, "Đường", "10"),
                        recipe(4, "Đá", "100")),
                2,
                "L",
                "Ít đá",
                "50%");

        assertDecimal("50.4", required.get(1));
        assertDecimal("84.0", required.get(2));
        assertDecimal("14.0", required.get(3));
        assertDecimal("100", required.get(4));
    }

    @Test
    void zero_sugar_and_no_ice_are_not_deducted() {
        Map<Integer, BigDecimal> required = DeductionCalculator.computeRequired(
                java.util.List.of(
                        recipe(3, "Đường", "10"),
                        recipe(4, "Đá", "100")),
                1,
                "M",
                "Không đá",
                "0%");

        assertFalse(required.containsKey(3));
        assertFalse(required.containsKey(4));
    }

    private static ProductRecipe recipe(int ingredientId, String ingredientName, String quantity) {
        ProductRecipe r = new ProductRecipe();
        r.setIngredientId(ingredientId);
        r.setIngredientName(ingredientName);
        r.setQuantity(new BigDecimal(quantity));
        return r;
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
