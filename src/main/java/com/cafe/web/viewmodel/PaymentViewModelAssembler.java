package com.cafe.web.viewmodel;

import com.cafe.service.cashier.BillingService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Chuỗi trình bày kết quả thanh toán, tách khỏi Controller và domain. */
public final class PaymentViewModelAssembler {
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    public String successMessage(String method, BillingService.PaymentResult result) {
        if (!"CASH".equals(method)) return "Đã ghi nhận thanh toán thành công.";
        return "Đã thu tiền mặt " + money(result.paidAmount())
                + " đ. Tiền thối lại khách: " + money(result.cashChange()) + " đ.";
    }

    private String money(BigDecimal value) {
        return NumberFormat.getIntegerInstance(VI).format(value == null ? BigDecimal.ZERO : value);
    }
}
