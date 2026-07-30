package com.cafe.service.cashier;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashPaymentCalculatorTest {

    @Test
    void exact_thousand_does_not_create_adjustment() {
        CashPaymentCalculator.CashQuote quote =
                CashPaymentCalculator.quote(new BigDecimal("97000.00"));

        moneyEquals("97000", quote.paidAmount());
        moneyEquals("0", quote.roundingAdjustment());
    }

    @Test
    void rounds_down_to_nearest_thousand() {
        CashPaymentCalculator.CashQuote quote =
                CashPaymentCalculator.quote(new BigDecimal("97200.00"));

        moneyEquals("97000", quote.paidAmount());
        moneyEquals("-200", quote.roundingAdjustment());
    }

    @Test
    void rounds_up_to_nearest_thousand() {
        CashPaymentCalculator.CashQuote quote =
                CashPaymentCalculator.quote(new BigDecimal("39960.00"));

        moneyEquals("40000", quote.paidAmount());
        moneyEquals("40", quote.roundingAdjustment());
    }

    @Test
    void midpoint_uses_half_up_policy() {
        CashPaymentCalculator.CashQuote quote =
                CashPaymentCalculator.quote(new BigDecimal("97500.00"));

        moneyEquals("98000", quote.paidAmount());
        moneyEquals("500", quote.roundingAdjustment());
    }

    @Test
    void computes_change_from_tendered_cash_and_rounded_payable() {
        CashPaymentCalculator.CashSettlement settlement =
                CashPaymentCalculator.settle(
                        new BigDecimal("39960.00"),
                        new BigDecimal("50000"));

        moneyEquals("40000", settlement.paidAmount());
        moneyEquals("50000", settlement.cashTendered());
        moneyEquals("10000", settlement.cashChange());
    }

    @Test
    void rejects_insufficient_tendered_cash() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> CashPaymentCalculator.settle(
                        new BigDecimal("39960.00"),
                        new BigDecimal("39000")));

        assertTrue(error.getMessage().contains("còn thiếu 1000"));
    }

    @Test
    void rejects_fractional_vnd_tender_and_invalid_bill_totals() {
        assertThrows(IllegalArgumentException.class,
                () -> CashPaymentCalculator.settle(
                        new BigDecimal("39960.00"),
                        new BigDecimal("50000.50")));
        assertThrows(IllegalArgumentException.class,
                () -> CashPaymentCalculator.quote(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> CashPaymentCalculator.quote(new BigDecimal("400")));
    }

    private void moneyEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }
}
