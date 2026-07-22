package com.cafe.common;

/** Prevents guessed QR tracking links from accessing another browser's table session. */
public final class QrSessionGuard {

    private QrSessionGuard() { }

    public static boolean matches(Object boundSessionId, Integer requestedSessionId) {
        return boundSessionId instanceof Integer && requestedSessionId != null
                && ((Integer) boundSessionId).intValue() == requestedSessionId.intValue();
    }
}
