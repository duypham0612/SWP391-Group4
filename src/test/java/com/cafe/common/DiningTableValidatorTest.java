package com.cafe.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiningTableValidatorTest {

    @Test
    void normalizesTableNumber() {
        assertEquals("Bàn 01", DiningTableValidator.normalizeTableNumber("  Bàn   01  "));
    }

    @Test
    void rejectsBlankOrOversizedTableNumber() {
        assertThrows(IllegalArgumentException.class, () -> DiningTableValidator.normalizeTableNumber("  "));
        assertThrows(IllegalArgumentException.class, () -> DiningTableValidator.normalizeTableNumber("x".repeat(21)));
    }

    @Test
    void capacityMustStayInOperationalRange() {
        assertEquals(4, DiningTableValidator.requireCapacity(4));
        assertThrows(IllegalArgumentException.class, () -> DiningTableValidator.requireCapacity(0));
        assertThrows(IllegalArgumentException.class, () -> DiningTableValidator.requireCapacity(31));
    }

    @Test
    void mergeRequiresTwoDifferentTables() {
        assertThrows(IllegalArgumentException.class, () -> DiningTableValidator.requireDifferentTables(5, 5));
    }
}
