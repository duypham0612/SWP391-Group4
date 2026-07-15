package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VietQrUtilTest {

    @Test
    void buildPayloadIncludesAmountContentAndValidCrc() {
        String payload = VietQrUtil.buildPayload(new BigDecimal("125000.49"), "Cafe bill 42");

        assertTrue(payload.startsWith("000201010212"));
        assertTrue(payload.contains("0010A000000727"));
        assertTrue(payload.contains("5303704"));
        assertTrue(payload.contains("5406125000"));
        assertTrue(payload.contains("5802VN"));
        assertTrue(payload.contains("CAFE BILL 42"));

        String body = payload.substring(0, payload.length() - 4);
        String crc = payload.substring(payload.length() - 4);
        assertEquals(crc16(body), crc);
    }

    private static String crc16(String input) {
        int crc = 0xFFFF;
        for (int i = 0; i < input.length(); i++) {
            crc ^= input.charAt(i) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format(Locale.ROOT, "%04X", crc);
    }
}
