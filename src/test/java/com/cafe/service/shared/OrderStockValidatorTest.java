package com.cafe.service.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStockValidatorTest {

    @Test
    void detectsOrderQuantityAboveAvailableStock() {
        List<OrderStockValidator.Shortfall> shortfalls = OrderStockValidator.findShortfalls(
                Map.of(7, new BigDecimal("5")), Map.of(7, new BigDecimal("1")));

        assertEquals(1, shortfalls.size());
        assertEquals(7, shortfalls.get(0).ingredientId());
        assertEquals(new BigDecimal("5"), shortfalls.get(0).required());
        assertEquals(new BigDecimal("1"), shortfalls.get(0).onHand());
    }

    @Test
    void acceptsOrderWithinAvailableStockAndIgnoresNonPositiveRequirement() {
        List<OrderStockValidator.Shortfall> shortfalls = OrderStockValidator.findShortfalls(
                Map.of(7, new BigDecimal("1"), 8, BigDecimal.ZERO),
                Map.of(7, new BigDecimal("1"), 8, BigDecimal.ZERO));

        assertTrue(shortfalls.isEmpty());
    }
}
