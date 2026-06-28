package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** ★ Test TRƯỚC cho tính tiền hoá đơn (voucher + VAT) — Phase 5. */
class BillCalculatorTest {

    private static void eq(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void percentDiscount() {
        // 10% của 100000 = 10000
        eq("10000.00", BillCalculator.computeDiscount("PERCENT", new BigDecimal("10"), new BigDecimal("100000")));
    }

    @Test
    void fixedDiscount_clampedToSubtotal() {
        // FIXED 50000 trên subtotal 30000 → kẹp về 30000
        eq("30000.00", BillCalculator.computeDiscount("FIXED", new BigDecimal("50000"), new BigDecimal("30000")));
    }

    @Test
    void vat_is_8_percent_of_net() {
        // net 90000 → VAT 8% = 7200
        eq("7200.00", BillCalculator.computeVat(new BigDecimal("90000")));
    }

    @Test
    void total_subtotal_minus_discount_plus_vat() {
        // subtotal 100000, discount 10000 → net 90000 + vat 7200 = 97200
        eq("97200.00", BillCalculator.computeTotal(new BigDecimal("100000"), new BigDecimal("10000")));
    }

    @Test
    void total_without_discount() {
        // 37000 + 8% = 39960
        eq("39960.00", BillCalculator.computeTotal(new BigDecimal("37000"), BigDecimal.ZERO));
    }

    @Test
    void noNegativeVatOrTotal() {
        eq("0.00", BillCalculator.computeVat(new BigDecimal("-5")));
        // discount > subtotal kẹp về subtotal → net 0 → total 0
        eq("0.00", BillCalculator.computeTotal(new BigDecimal("20000"),
                BillCalculator.computeDiscount("FIXED", new BigDecimal("99999"), new BigDecimal("20000"))));
    }
}
