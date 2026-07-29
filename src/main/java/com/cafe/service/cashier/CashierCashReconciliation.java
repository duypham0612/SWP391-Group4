package com.cafe.service.cashier;

import java.math.BigDecimal;

/** Quy tắc đối chiếu tiền mặt khi mở và kết ca. */
public final class CashierCashReconciliation {

    private static final BigDecimal MAX_MONEY = new BigDecimal("999999999999.99");

    private CashierCashReconciliation() {
    }

    public static BigDecimal expectedClosingCash(BigDecimal openingCash, BigDecimal cashRevenue) {
        return zeroIfNull(openingCash).add(zeroIfNull(cashRevenue));
    }

    public static void requireMatchingClosingCash(
            BigDecimal actualClosingCash,
            BigDecimal openingCash,
            BigDecimal cashRevenue) {
        requireValidMoney(actualClosingCash, "Tổng tiền mặt trong két");
        BigDecimal expected = expectedClosingCash(openingCash, cashRevenue);
        if (actualClosingCash.compareTo(expected) != 0) {
            throw new IllegalArgumentException(
                    "Tổng tiền mặt không khớp. Vui lòng kiểm đếm và nhập lại đúng tổng tiền trong két.");
        }
    }

    public static void requireValidMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " không được âm.");
        }
        if (value.compareTo(MAX_MONEY) > 0 || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(fieldName + " không hợp lệ.");
        }
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
