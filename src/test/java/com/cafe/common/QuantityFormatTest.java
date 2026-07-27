package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuantityFormatTest {

    @Test
    void groupsThousandsAndKeepsRealDecimalsInVietnameseFormat() {
        assertEquals("69.769", QuantityFormat.groupedVi(new BigDecimal("69769.000")));
        assertEquals("499.309", QuantityFormat.groupedVi(new BigDecimal("499309")));
        assertEquals("21,6", QuantityFormat.groupedVi(new BigDecimal("21.600")));
    }
}
