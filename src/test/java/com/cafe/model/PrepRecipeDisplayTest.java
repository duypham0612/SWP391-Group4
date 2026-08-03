package com.cafe.model;

import org.junit.jupiter.api.Test;
import com.cafe.web.viewmodel.ViewFormatter;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrepRecipeDisplayTest {
    private final ViewFormatter view = new ViewFormatter();

    @Test
    void integer_display_removes_the_fractional_part() {
        Recipe recipe = new Recipe();
        recipe.setQuantity(new BigDecimal("500.750"));

        assertEquals("500", view.integer(recipe.getQuantity()));
    }

    @Test
    void integer_display_removes_decimal_zeroes() {
        Recipe recipe = new Recipe();
        recipe.setQuantity(new BigDecimal("20.000"));

        assertEquals("20", view.integer(recipe.getQuantity()));
    }

    @Test
    void yield_display_removes_only_unneeded_trailing_zeroes() {
        Ingredient recipe = new Ingredient();
        recipe.setPrepYieldQty(new BigDecimal("1000.000"));
        assertEquals("1000", view.plain(recipe.getPrepYieldQty()));

        recipe.setPrepYieldQty(new BigDecimal("250.500"));
        assertEquals("250.5", view.plain(recipe.getPrepYieldQty()));
    }
}
