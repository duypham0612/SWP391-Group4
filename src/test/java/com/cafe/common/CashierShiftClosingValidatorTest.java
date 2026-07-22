package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CashierShiftClosingValidatorTest {

    @Test
    void expectedClosingCashIsOpeningCashPlusPaidRevenue() {
        assertEquals(new BigDecimal("1750000"), CashierShiftClosingValidator.expectedClosingCash(
                new BigDecimal("500000"), new BigDecimal("1250000")));
    }

    @Test
    void acceptsOnlyTheExactClosingCash() {
        assertDoesNotThrow(() -> CashierShiftClosingValidator.requireExact(
                new BigDecimal("1750000.00"), new BigDecimal("500000"), new BigDecimal("1250000")));
        assertThrows(IllegalStateException.class, () -> CashierShiftClosingValidator.requireExact(
                new BigDecimal("1749000"), new BigDecimal("500000"), new BigDecimal("1250000")));
    }

    @Test
    void treatsMissingRevenueAsZero() {
        assertEquals(new BigDecimal("500000"), CashierShiftClosingValidator.expectedClosingCash(
                new BigDecimal("500000"), null));
    }
}
