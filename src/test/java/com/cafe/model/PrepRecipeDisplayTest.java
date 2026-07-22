package com.cafe.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrepRecipeDisplayTest {

    @Test
    void yield_display_removes_only_unneeded_trailing_zeroes() {
        PrepRecipe recipe = new PrepRecipe();
        recipe.setYieldQty(new BigDecimal("1000.000"));
        assertEquals("1000", recipe.getYieldDisplay());

        recipe.setYieldQty(new BigDecimal("250.500"));
        assertEquals("250.5", recipe.getYieldDisplay());
    }
}
