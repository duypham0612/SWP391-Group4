package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

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

    // ===== allocateByWeight — no-drift khi tách bill =====

    private static BigDecimal sum(List<BigDecimal> xs) {
        BigDecimal s = BigDecimal.ZERO;
        for (BigDecimal x : xs) s = s.add(x);
        return s;
    }

    @Test
    void allocate_sumEqualsAmount_exactly_oddCase() {
        // 10.00 chia theo 1:1:1 → 3.33 + 3.33 + 3.34 = 10.00 (không mất/ dư xu)
        List<BigDecimal> parts = BillCalculator.allocateByWeight(
                new BigDecimal("10.00"), Arrays.asList(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        eq("10.00", sum(parts));
        assertEquals(3, parts.size());
    }

    @Test
    void allocate_proportional() {
        // 100.00 theo 30:70 → 30.00 / 70.00
        List<BigDecimal> parts = BillCalculator.allocateByWeight(
                new BigDecimal("100.00"), Arrays.asList(new BigDecimal("30"), new BigDecimal("70")));
        eq("30.00", parts.get(0));
        eq("70.00", parts.get(1));
        eq("100.00", sum(parts));
    }

    @Test
    void allocate_zeroAmount_or_zeroWeights() {
        eq("0.00", sum(BillCalculator.allocateByWeight(BigDecimal.ZERO, Arrays.asList(BigDecimal.ONE, BigDecimal.TEN))));
        // tổng weight 0 → tất cả 0
        eq("0.00", sum(BillCalculator.allocateByWeight(new BigDecimal("5.00"), Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO))));
    }

    @Test
    void splitVat_noDrift_vsSingleBill() {
        // Tab: subtotal 33333 (bill A) + 66667 (bill B) = 100000, không voucher.
        // VAT cả tab (1 lần) = 8% × 100000 = 8000.00. Tổng VAT 2 bill phải = 8000.00 (không lệch).
        BigDecimal a = new BigDecimal("33333"), b = new BigDecimal("66667");
        BigDecimal sessionVat = BillCalculator.computeVat(a.add(b));
        eq("8000.00", sessionVat);
        List<BigDecimal> vatParts = BillCalculator.allocateByWeight(sessionVat, Arrays.asList(a, b));
        eq("8000.00", sum(vatParts));   // ★ no-drift: nếu tính VAT từng bill rồi cộng có thể ra 7999.99/8000.01
    }

    @Test
    void splitDiscount_proportional_noDrift() {
        // FIXED 10000 trên tab subtotal 100000, tách 2 bill 30000/70000 → 3000/7000, tổng = 10000.
        BigDecimal sessionSubtotal = new BigDecimal("100000");
        BigDecimal disc = BillCalculator.computeDiscount("FIXED", new BigDecimal("10000"), sessionSubtotal);
        eq("10000.00", disc);
        List<BigDecimal> parts = BillCalculator.allocateByWeight(disc, Arrays.asList(new BigDecimal("30000"), new BigDecimal("70000")));
        eq("3000.00", parts.get(0));
        eq("7000.00", parts.get(1));
        eq("10000.00", sum(parts));
    }
}
