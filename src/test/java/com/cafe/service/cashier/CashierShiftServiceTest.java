package com.cafe.service.cashier;

import com.cafe.dao.payment.CashierShiftDao;
import com.cafe.model.CashierShift;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashierShiftServiceTest {

    @Test
    void noOpenShiftAllowsNewShift() {
        assertNull(CashierShiftService.selectOwnOpenShift(List.of(), 10));
    }

    @Test
    void ownOpenShiftMakesRepeatedStartIdempotent() {
        CashierShift own = shift(98, 10, "Lê Thu Ngân");

        assertSame(own, CashierShiftService.selectOwnOpenShift(List.of(own), 10));
    }

    @Test
    void anotherCashiersOpenShiftBlocksStarting() {
        CashierShift existing = shift(97, 20, "nguyenquanganh");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> CashierShiftService.selectOwnOpenShift(List.of(existing), 10));

        assertTrue(error.getMessage().contains("#97"));
        assertTrue(error.getMessage().contains("nguyenquanganh"));
    }

    @Test
    void legacyDuplicateOpenRowsStillBlockEvenWhenOneBelongsToCurrentCashier() {
        CashierShift own = shift(98, 10, "Lê Thu Ngân");
        CashierShift other = shift(97, 20, "nguyenquanganh");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> CashierShiftService.selectOwnOpenShift(List.of(own, other), 10));

        assertTrue(error.getMessage().contains("nhiều ca thu ngân"));
        assertTrue(error.getMessage().contains("Quản lý"));
    }

    @Test
    void legacyDuplicateOpenRowsOfSameCashierAreNotSilentlyReused() {
        CashierShift first = shift(97, 10, "Lê Thu Ngân");
        CashierShift second = shift(98, 10, "Lê Thu Ngân");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> CashierShiftService.selectOwnOpenShift(List.of(first, second), 10));

        assertTrue(error.getMessage().contains("nhiều ca thu ngân"));
    }

    @Test
    void noPendingOrderDoesNotRequireHandoverConfirmation() {
        assertDoesNotThrow(() -> CashierShiftService.requireHandoverConfirmed(
                new CashierShiftDao.PendingHandover(0, 0), false));
    }

    @Test
    void pendingOrdersRequireExplicitHandoverConfirmation() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> CashierShiftService.requireHandoverConfirmed(
                        new CashierShiftDao.PendingHandover(2, 3), false));

        assertTrue(error.getMessage().contains("5 đơn"));
        assertDoesNotThrow(() -> CashierShiftService.requireHandoverConfirmed(
                new CashierShiftDao.PendingHandover(2, 3), true));
    }

    private CashierShift shift(int shiftId, int cashierId, String cashierName) {
        CashierShift shift = new CashierShift();
        shift.setCashierShiftId(shiftId);
        shift.setCashierId(cashierId);
        shift.setCashierName(cashierName);
        return shift;
    }
}
