package com.cafe.service.cashier;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CashierCashReconciliationTest {

    @Test
    void expected_cash_is_opening_float_plus_paid_cash_bills() {
        assertEquals(new BigDecimal("3500000"),
                CashierCashReconciliation.expectedClosingCash(
                        new BigDecimal("500000"),
                        new BigDecimal("3000000")));
    }

    @Test
    void matching_cash_can_close_the_shift() {
        assertDoesNotThrow(() -> CashierCashReconciliation.requireMatchingClosingCash(
                new BigDecimal("3500000"),
                new BigDecimal("500000"),
                new BigDecimal("3000000")));
    }

    @Test
    void shortage_or_overage_cannot_close_the_shift() {
        assertThrows(IllegalArgumentException.class,
                () -> CashierCashReconciliation.requireMatchingClosingCash(
                        new BigDecimal("3499000"),
                        new BigDecimal("500000"),
                        new BigDecimal("3000000")));
        assertThrows(IllegalArgumentException.class,
                () -> CashierCashReconciliation.requireMatchingClosingCash(
                        new BigDecimal("3501000"),
                        new BigDecimal("500000"),
                        new BigDecimal("3000000")));
    }

    @Test
    void negative_or_over_precision_money_is_invalid() {
        assertThrows(IllegalArgumentException.class,
                () -> CashierCashReconciliation.requireValidMoney(
                        new BigDecimal("-1"), "Quỹ đầu ca"));
        assertThrows(IllegalArgumentException.class,
                () -> CashierCashReconciliation.requireValidMoney(
                        new BigDecimal("1.001"), "Quỹ đầu ca"));
    }
}
