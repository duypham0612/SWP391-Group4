package com.cafe.service.shared;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderQuantityValidatorTest {

    @Test
    void accepts_exactly_twenty_units_from_qr_or_pos() {
        assertDoesNotThrow(() -> OrderQuantityValidator.validate(List.of(line(3, 20))));
    }

    @Test
    void rejects_twenty_one_units_across_duplicate_cart_lines() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> OrderQuantityValidator.validate(List.of(line(3, 10), line(3, 11))));
        assertEquals("Mỗi loại món chỉ được đặt tối đa 20 trong một đơn.", error.getMessage());
    }

    private static OrderService.CartLine line(int productId, int quantity) {
        OrderService.CartLine line = new OrderService.CartLine();
        line.productId = productId;
        line.quantity = quantity;
        return line;
    }
}
