package com.cafe.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Business rule for reconciling the cashier till before a shift can be closed. */
public final class CashierShiftClosingValidator {

    private CashierShiftClosingValidator() { }

    public static BigDecimal expectedClosingCash(BigDecimal openingCash, BigDecimal shiftRevenue) {
        return money(openingCash).add(money(shiftRevenue));
    }

    public static void requireExact(BigDecimal closingCash, BigDecimal openingCash, BigDecimal shiftRevenue) {
        BigDecimal expected = expectedClosingCash(openingCash, shiftRevenue);
        if (closingCash == null || closingCash.compareTo(expected) != 0) {
            NumberFormat currency = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
            currency.setMaximumFractionDigits(0);
            throw new IllegalStateException(
                    "Quỹ cuối ca phải bằng quỹ đầu ca + doanh thu ca: " + currency.format(expected) + " ₫.");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
