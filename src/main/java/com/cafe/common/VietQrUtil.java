package com.cafe.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

/** Builds EMVCo merchant-presented VietQR payloads for offline QR rendering. */
public final class VietQrUtil {

    private static final String PAYLOAD_FORMAT = "01";
    private static final String POINT_OF_INIT_METHOD_DYNAMIC = "12";
    private static final String VIETQR_GUID = "A000000727";
    private static final String VIETQR_SERVICE = "QRIBFTTA";
    private static final String CURRENCY_VND = "704";
    private static final String COUNTRY_VN = "VN";

    private VietQrUtil() { }

    public static String buildPayload(BigDecimal amount, String content) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
        String amountText = safeAmount.setScale(0, RoundingMode.HALF_UP).toPlainString();

        String consumerAccount = tag("00", Constants.VIETQR_BANK_BIN)
                + tag("01", Constants.VIETQR_ACCOUNT_NO);
        String merchantAccount = tag("00", VIETQR_GUID)
                + tag("01", consumerAccount)
                + tag("02", VIETQR_SERVICE);

        String payload = tag("00", PAYLOAD_FORMAT)
                + tag("01", POINT_OF_INIT_METHOD_DYNAMIC)
                + tag("38", merchantAccount)
                + tag("53", CURRENCY_VND)
                + tag("54", amountText)
                + tag("58", COUNTRY_VN)
                + tag("59", sanitize(Constants.VIETQR_ACCOUNT_NAME, 25))
                + tag("60", "VN")
                + tag("62", tag("08", sanitize(content, 50)));

        String crcInput = payload + "6304";
        return crcInput + crc16(crcInput);
    }

    private static String tag(String id, String value) {
        String v = value == null ? "" : value;
        return id + String.format(Locale.ROOT, "%02d", v.length()) + v;
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null) return "";
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ._\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
        return ascii.length() <= maxLength ? ascii : ascii.substring(0, maxLength);
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
