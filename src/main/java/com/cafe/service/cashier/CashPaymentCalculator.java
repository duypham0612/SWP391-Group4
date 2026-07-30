package com.cafe.service.cashier;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Quy tắc thu tiền mặt VND: làm tròn tổng bill tới 1.000 đồng gần nhất. */
public final class CashPaymentCalculator {

    public static final BigDecimal ROUNDING_UNIT = new BigDecimal("1000");
    private static final BigDecimal MAX_MONEY = new BigDecimal("999999999999.99");

    private CashPaymentCalculator() {
    }

    public static CashQuote quote(BigDecimal billTotal) {
        requireValidBillTotal(billTotal);
        BigDecimal payable = billTotal
                .divide(ROUNDING_UNIT, 0, RoundingMode.HALF_UP)
                .multiply(ROUNDING_UNIT)
                .setScale(2, RoundingMode.UNNECESSARY);
        if (payable.signum() <= 0) {
            throw new IllegalArgumentException("Tổng tiền quá nhỏ để thanh toán tiền mặt.");
        }
        return new CashQuote(
                billTotal,
                payable.subtract(billTotal),
                payable);
    }

    public static CashSettlement settle(BigDecimal billTotal, BigDecimal cashTendered) {
        CashQuote quote = quote(billTotal);
        requireValidTender(cashTendered);
        if (cashTendered.compareTo(quote.paidAmount()) < 0) {
            BigDecimal missing = quote.paidAmount().subtract(cashTendered);
            throw new IllegalArgumentException(
                    "Tiền khách đưa chưa đủ, còn thiếu " + moneyText(missing) + " đ.");
        }
        return new CashSettlement(
                quote.billTotal(),
                quote.roundingAdjustment(),
                quote.paidAmount(),
                cashTendered,
                cashTendered.subtract(quote.paidAmount()));
    }

    private static void requireValidBillTotal(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.compareTo(MAX_MONEY) > 0) {
            throw new IllegalArgumentException("Tổng tiền hóa đơn không hợp lệ.");
        }
    }

    private static void requireValidTender(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Phải nhập số tiền khách đưa.");
        }
        if (value.signum() < 0 || value.compareTo(MAX_MONEY) > 0
                || value.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Tiền khách đưa phải là số nguyên VND hợp lệ.");
        }
    }

    private static String moneyText(BigDecimal value) {
        return value.setScale(0, RoundingMode.UNNECESSARY).toPlainString();
    }

    public record CashQuote(
            BigDecimal billTotal,
            BigDecimal roundingAdjustment,
            BigDecimal paidAmount) {
    }

    public record CashSettlement(
            BigDecimal billTotal,
            BigDecimal roundingAdjustment,
            BigDecimal paidAmount,
            BigDecimal cashTendered,
            BigDecimal cashChange) {
    }
}
