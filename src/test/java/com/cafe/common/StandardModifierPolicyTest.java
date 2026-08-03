package com.cafe.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardModifierPolicyTest {

    @Test
    void fixedPriceDeltas_matchBusinessRule() {
        assertEquals("0.00", StandardModifierPolicy.priceDelta("Size", "Size S").toPlainString());
        assertEquals("6000.00", StandardModifierPolicy.priceDelta("Size", "Size M").toPlainString());
        assertEquals("10000.00", StandardModifierPolicy.priceDelta("Size", "Size L").toPlainString());
        assertEquals("0.00", StandardModifierPolicy.priceDelta("Đường", "Nhiều đường").toPlainString());
        assertEquals("0.00", StandardModifierPolicy.priceDelta("Đá", "Nhiều đá").toPlainString());
    }

    @Test
    void defaults_areSizeS_andNormalSugarIce() {
        assertTrue(StandardModifierPolicy.isDefault("Size", "Size S"));
        assertTrue(StandardModifierPolicy.isDefault("Đường", "Bình thường"));
        assertTrue(StandardModifierPolicy.isDefault("Đá", "Bình thường"));
        assertFalse(StandardModifierPolicy.isDefault("Size", "Size M"));
        assertFalse(StandardModifierPolicy.isDefault("Đường", "Ít đường"));
    }
}
