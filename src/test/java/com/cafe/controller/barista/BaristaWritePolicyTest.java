package com.cafe.controller.barista;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaristaWritePolicyTest {

    @Test
    void accepts_only_known_actions_for_each_barista_surface() {
        assertTrue(BaristaWritePolicy.isKdsAction("markReady"));
        assertTrue(BaristaWritePolicy.isPrepAction("createBatch"));
        assertTrue(BaristaWritePolicy.isWasteAction("void"));
        assertTrue(BaristaWritePolicy.isEightySixAction("report86"));
        assertTrue(BaristaWritePolicy.isHandoverAction("createAndClockOut"));
        assertTrue(BaristaWritePolicy.isShiftAction("clockIn"));
    }

    @Test
    void rejects_null_unknown_and_cross_surface_actions() {
        assertFalse(BaristaWritePolicy.isKdsAction(null));
        assertFalse(BaristaWritePolicy.isKdsAction("deleteEverything"));
        assertFalse(BaristaWritePolicy.isPrepAction("markReady"));
        assertFalse(BaristaWritePolicy.isWasteAction("report86"));
        assertFalse(BaristaWritePolicy.isEightySixAction("void"));
        assertFalse(BaristaWritePolicy.isHandoverAction("clockOut"));
        assertFalse(BaristaWritePolicy.isShiftAction("create"));
    }
}
