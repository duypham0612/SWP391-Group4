package com.cafe.web.form;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

final class FormValues {
    private FormValues() { }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }

    static int optionalInt(String value, String label) {
        if (value == null || value.isBlank()) return 0;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException e) { throw new FormBindingException(label + " không hợp lệ."); }
    }

    static Integer optionalInteger(String value, String label) {
        if (value == null || value.isBlank()) return null;
        return optionalInt(value, label);
    }

    static BigDecimal decimal(String value, String label) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(value.trim().replace(",", "")); }
        catch (NumberFormatException e) { throw new FormBindingException(label + " phải là một số hợp lệ."); }
    }

    static LocalDateTime optionalDateTime(String value, String label) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDateTime.parse(value.trim()); }
        catch (DateTimeParseException e) { throw new FormBindingException(label + " không đúng định dạng."); }
    }

    static LocalTime optionalTime(String value, String label) {
        if (value == null || value.isBlank()) return null;
        try { return LocalTime.parse(value.trim()); }
        catch (DateTimeParseException e) { throw new FormBindingException(label + " không đúng định dạng."); }
    }
}
