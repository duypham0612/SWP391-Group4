package com.cafe.service.cashier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingPaymentMethodTest {

    @Test
    void accepts_only_supported_payment_methods() {
        assertTrue(BillingService.isSupportedPaymentMethod("CASH"));
        assertTrue(BillingService.isSupportedPaymentMethod("TRANSFER"));
        assertTrue(BillingService.isSupportedPaymentMethod("QR_BANK"));

        assertFalse(BillingService.isSupportedPaymentMethod(null));
        assertFalse(BillingService.isSupportedPaymentMethod(""));
        assertFalse(BillingService.isSupportedPaymentMethod("cash"));
        assertFalse(BillingService.isSupportedPaymentMethod("OTHER"));
    }
}
