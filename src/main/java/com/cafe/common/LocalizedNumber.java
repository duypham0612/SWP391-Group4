package com.cafe.common;

import java.math.BigDecimal;

/** Parse số nhập theo cách viết Việt Nam, đồng thời vẫn nhận dạng chuẩn của BigDecimal. */
public final class LocalizedNumber {
    private LocalizedNumber() {}

    /**
     * Ví dụ: 23.200 -> 23200; 21,6 -> 21.6; 1.234,56 -> 1234.56;
     * 21.6 -> 21.6 (dạng chuẩn từ client).
     */
    public static BigDecimal parse(String value) {
        if (value == null) throw new NumberFormatException("null");
        String raw = value.trim().replace(" ", "");
        if (raw.isEmpty()) throw new NumberFormatException("empty");

        if (raw.indexOf(',') >= 0) {
            raw = raw.replace(".", "").replace(',', '.');
        } else if (raw.matches("[+-]?\\d{1,3}(\\.\\d{3})+")) {
            raw = raw.replace(".", "");
        }
        return new BigDecimal(raw);
    }
}
