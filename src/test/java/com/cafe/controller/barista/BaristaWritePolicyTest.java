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

    /**
     * Chấm công là bước có ngữ cảnh (thấy ca được xếp + bàn giao đang chờ) nên chỉ màn
     * "Ca làm của tôi" nhận. Màn vận hành chỉ trỏ sang đó, và chốt nằm ở server chứ không
     * phải chỉ ẩn nút — POST tự soạn tới các màn này phải bị từ chối.
     */
    @Test
    void operational_surfaces_reject_clock_actions() {
        for (String action : new String[]{"clockIn", "clockOut"}) {
            assertFalse(BaristaWritePolicy.isKdsAction(action), "KDS phải từ chối " + action);
            assertFalse(BaristaWritePolicy.isPrepAction(action), "Prep phải từ chối " + action);
            assertFalse(BaristaWritePolicy.isWasteAction(action), "Waste phải từ chối " + action);
            assertFalse(BaristaWritePolicy.isEightySixAction(action), "86 phải từ chối " + action);
            assertFalse(BaristaWritePolicy.isHandoverAction(action), "Handover phải từ chối " + action);

            assertTrue(BaristaWritePolicy.isShiftAction(action), "Ca làm của tôi phải nhận " + action);
        }
    }

    @Test
    void prep_surface_no_longer_accepts_direct_edit_or_cancel() {
        assertFalse(BaristaWritePolicy.isPrepAction("updateBatch"));
        assertFalse(BaristaWritePolicy.isPrepAction("cancelBatch"));
        assertTrue(BaristaWritePolicy.isPrepAction("createBatch"));
        assertTrue(BaristaWritePolicy.isPrepAction("writeOffExpired"));
    }
}
