package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizedNumberTest {
    @Test
    void parsesVietnameseGroupedInteger() {
        assertEquals(new BigDecimal("23200"), LocalizedNumber.parse("23.200"));
        assertEquals(new BigDecimal("68000"), LocalizedNumber.parse("68.000"));
    }

    @Test
    void parsesVietnameseDecimalAndStandardDecimal() {
        assertEquals(new BigDecimal("1234.56"), LocalizedNumber.parse("1.234,56"));
        assertEquals(new BigDecimal("21.6"), LocalizedNumber.parse("21,6"));
        assertEquals(new BigDecimal("21.6"), LocalizedNumber.parse("21.6"));
    }
}
