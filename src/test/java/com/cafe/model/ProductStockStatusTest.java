package com.cafe.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductStockStatusTest {

    @Test
    void low_warns_but_does_not_mark_product_out() {
        ProductStockStatus status = new ProductStockStatus(3);
        status.include(ProductStockStatus.LOW, "Syrup Đào");

        assertTrue(status.isLow());
        assertFalse(status.isOut());
        assertEquals("Sắp hết Syrup Đào", status.getMessage());
    }

    @Test
    void out_has_priority_over_low_and_lists_the_depleted_ingredient() {
        ProductStockStatus status = new ProductStockStatus(3);
        status.include(ProductStockStatus.LOW, "Đá");
        status.include(ProductStockStatus.OUT, "Syrup Đào");

        assertTrue(status.isOut());
        assertEquals("Hết Syrup Đào", status.getMessage());
    }
}
