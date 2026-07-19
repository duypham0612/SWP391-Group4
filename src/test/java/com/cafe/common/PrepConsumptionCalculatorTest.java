package com.cafe.common;

import com.cafe.model.PrepRecipe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrepConsumptionCalculatorTest {

    private static PrepRecipe recipe(int rawIngredientId, String quantity, String yieldQty) {
        PrepRecipe r = new PrepRecipe();
        r.setRawIngredientId(rawIngredientId);
        r.setQuantity(new BigDecimal(quantity));
        r.setYieldQty(new BigDecimal(yieldQty));
        return r;
    }

    private static void assertQty(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void dividesByYieldAndRoundsHalfUpToSixPlacesBeforeMultiplyingRawQuantity() {
        BigDecimal consumed = PrepConsumptionCalculator.consumedRaw(
                new BigDecimal("10"), recipe(1, "2", "3"));

        assertQty("6.666666", consumed);
    }

    @Test
    void handlesProducedQuantitySmallerThanYield() {
        BigDecimal consumed = PrepConsumptionCalculator.consumedRaw(
                new BigDecimal("0.5"), recipe(1, "12", "4"));

        assertQty("1.500000", consumed);
    }

    @Test
    void appliesSameFormulaForEachRawLineInAPreppedRecipe() {
        BigDecimal coffee = PrepConsumptionCalculator.consumedRaw(
                new BigDecimal("30"), recipe(1, "8", "20"));
        BigDecimal sugar = PrepConsumptionCalculator.consumedRaw(
                new BigDecimal("30"), recipe(2, "3", "20"));

        assertQty("12.000000", coffee);
        assertQty("4.500000", sugar);
    }
}
