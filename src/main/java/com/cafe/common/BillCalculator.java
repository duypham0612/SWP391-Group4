package com.cafe.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ★ Tính tiền hoá đơn — logic THUẦN (không DB) để unit-test trước (Phase 5, rủi ro cao).
 *
 * Quy ước: giá dòng đơn (OrderItem.UnitPrice) là giá đã chốt tại thời điểm đặt. Bill:
 *   subtotal  = Σ amount các dòng trên bill
 *   discount  = giảm giá voucher (PERCENT theo % subtotal / FIXED số tiền), kẹp trong [0, subtotal]
 *   net       = subtotal - discount
 *   vat       = net × VAT_RATE
 *   total     = net + vat
 */
public final class BillCalculator {
    private BillCalculator() {}

    /** VAT F&B hiện hành (8%). Đổi ở một chỗ duy nhất. */
    public static final BigDecimal VAT_RATE = new BigDecimal("0.08");

    /** Giảm giá voucher trên subtotal. type: PERCENT | FIXED. Kẹp [0, subtotal]. minOrder chặn ở validate. */
    public static BigDecimal computeDiscount(String type, BigDecimal value, BigDecimal subtotal) {
        if (type == null || value == null || subtotal == null) return BigDecimal.ZERO;
        BigDecimal d;
        if ("PERCENT".equals(type)) {
            d = subtotal.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else { // FIXED
            d = value;
        }
        if (d.signum() < 0) d = BigDecimal.ZERO;
        if (d.compareTo(subtotal) > 0) d = subtotal;          // không giảm quá subtotal
        return d.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal computeVat(BigDecimal net) {
        if (net == null || net.signum() <= 0) return BigDecimal.ZERO;
        return net.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /** Tổng phải trả = (subtotal - discount) + vat. */
    public static BigDecimal computeTotal(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal net = subtotal.subtract(discount);
        if (net.signum() < 0) net = BigDecimal.ZERO;
        return net.add(computeVat(net)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * ★ Phân bổ KHÔNG LỆCH (largest-remainder): chia {@code amount} thành n phần theo {@code weights},
     * mỗi phần scale 2, TỔNG các phần == amount TUYỆT ĐỐI (không drift ±0.01 khi tách bill).
     * Dùng cho VAT/discount khi split: tính 1 lần trên tổng rồi rải phần dư cho phần có số lẻ lớn nhất.
     */
    public static java.util.List<BigDecimal> allocateByWeight(BigDecimal amount, java.util.List<BigDecimal> weights) {
        int n = weights == null ? 0 : weights.size();
        java.util.List<BigDecimal> out = new java.util.ArrayList<>(n);
        if (n == 0) return out;
        BigDecimal amt = (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (BigDecimal w : weights) totalWeight = totalWeight.add(w == null ? BigDecimal.ZERO : w);
        if (amt.signum() == 0 || totalWeight.signum() <= 0) {
            for (int i = 0; i < n; i++) out.add(BigDecimal.ZERO.setScale(2));
            return out;
        }
        long totalCents = amt.movePointRight(2).longValueExact();
        long[] cents = new long[n];
        BigDecimal[] frac = new BigDecimal[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            BigDecimal w = weights.get(i) == null ? BigDecimal.ZERO : weights.get(i);
            BigDecimal shareCents = amt.multiply(w).divide(totalWeight, 10, RoundingMode.HALF_UP).movePointRight(2);
            long floor = shareCents.setScale(0, RoundingMode.FLOOR).longValueExact();
            cents[i] = floor;
            frac[i] = shareCents.subtract(new BigDecimal(floor));
            allocated += floor;
        }
        long remainder = totalCents - allocated;   // số xu dư (< n)
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> {
            int c = frac[b].compareTo(frac[a]);
            return c != 0 ? c : Integer.compare(a, b);
        });
        for (int k = 0; k < remainder && k < n; k++) cents[idx[k]]++;
        for (int i = 0; i < n; i++) out.add(new BigDecimal(cents[i]).movePointLeft(2).setScale(2));
        return out;
    }
}
