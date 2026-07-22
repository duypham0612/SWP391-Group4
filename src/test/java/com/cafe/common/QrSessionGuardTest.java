package com.cafe.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrSessionGuardTest {

    @Test
    void acceptsOnlyTheSessionBoundToThisBrowser() {
        assertTrue(QrSessionGuard.matches(42, 42));
        assertFalse(QrSessionGuard.matches(42, 43));
        assertFalse(QrSessionGuard.matches(null, 42));
        assertFalse(QrSessionGuard.matches("42", 42));
        assertFalse(QrSessionGuard.matches(42, null));
    }
}
