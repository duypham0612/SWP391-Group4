package com.cafe.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ★ Tính tiền hoá đơn — logic THUẦN (không DB) để unit-test trước (Phase 5, rủi ro cao).
 *
 * Quy ước: giá dòng đơn (OrderItem.UnitPrice) ĐÃ gồm modifier. Bill:
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
}
