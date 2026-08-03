package com.cafe.common;

import com.cafe.model.Recipe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrepConsumptionCalculatorTest {

    private static BigDecimal prepYield(String yieldQty) { return new BigDecimal(yieldQty); }

    private static Recipe ingredient(int rawIngredientId, String quantity) {
        Recipe line = new Recipe();
        line.setIngredientId(rawIngredientId);
        line.setQuantity(new BigDecimal(quantity));
        return line;
    }

    private static void assertQty(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void dividesByYieldAndRoundsHalfUpToSixPlacesBeforeMultiplyingRawQuantity() {
        BigDecimal consumed = PrepConsumptionCalculator.consumedRaw(
                new BigDecimal("10"), prepYield("3"), ingredient(1, "2"));

        assertQty("6.666666", consumed);
    }

    @Test
    void handlesProducedQuantitySmallerThanYield() {
        BigDecimal consumed = PrepConsumptionCalculator.consumedRaw(
                new BigDecimal("0.5"), prepYield("4"), ingredient(1, "12"));

        assertQty("1.500000", consumed);
    }

    @Test
    void appliesSameFormulaForEachRawLineInAPreppedRecipe() {
        BigDecimal recipeYield = prepYield("20");
        BigDecimal coffee = PrepConsumptionCalculator.consumedRaw(
                new BigDecimal("30"), recipeYield, ingredient(1, "8"));
        BigDecimal sugar = PrepConsumptionCalculator.consumedRaw(
                new BigDecimal("30"), recipeYield, ingredient(2, "3"));

        assertQty("12.000000", coffee);
        assertQty("4.500000", sugar);
    }
}
