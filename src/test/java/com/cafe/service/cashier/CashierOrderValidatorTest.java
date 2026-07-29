package com.cafe.service.cashier;

import com.cafe.service.shared.OrderService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CashierOrderValidatorTest {

    @Test
    void accepts_twenty_units_of_one_product() {
        assertDoesNotThrow(() -> CashierOrderValidator.validate(List.of(line(1, 20))));
    }

    @Test
    void rejects_more_than_twenty_units_of_one_product() {
        assertThrows(IllegalArgumentException.class,
                () -> CashierOrderValidator.validate(List.of(line(1, 21))));
    }

    @Test
    void aggregates_duplicate_lines_of_the_same_product() {
        assertThrows(IllegalArgumentException.class,
                () -> CashierOrderValidator.validate(List.of(line(1, 12), line(1, 9))));
    }

    @Test
    void applies_the_limit_independently_per_product() {
        assertDoesNotThrow(() ->
                CashierOrderValidator.validate(List.of(line(1, 20), line(2, 20))));
    }

    private static OrderService.CartLine line(int productId, int quantity) {
        OrderService.CartLine line = new OrderService.CartLine();
        line.productId = productId;
        line.quantity = quantity;
        return line;
    }
}
