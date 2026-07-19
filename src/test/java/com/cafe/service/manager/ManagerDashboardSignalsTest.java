package com.cafe.service.manager;

import com.cafe.model.BranchInventory;
import com.cafe.model.MenuBlockRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagerDashboardSignalsTest {

    @Test
    void empty_menu_blocks_have_zero_counts() {
        ManagerDashboardService.Summary s = new ManagerDashboardService.Summary();
        s.openMenuBlocks = List.of();

        assertEquals(0, s.getOpenMenuBlockCount());
        assertEquals(0, s.getOverdueMenuBlockCount());
        assertFalse(s.isHasOpenMenuBlocks());
    }

    @Test
    void counts_open_and_overdue_menu_blocks() {
        ManagerDashboardService.Summary s = new ManagerDashboardService.Summary();
        s.openMenuBlocks = List.of(
                request(LocalDateTime.now().minusMinutes(10), null),
                request(LocalDateTime.now().plusHours(1), null),
                request(null, null));

        assertEquals(3, s.getOpenMenuBlockCount());
        assertEquals(1, s.getOverdueMenuBlockCount());
        assertTrue(s.isHasOpenMenuBlocks());
    }

    @Test
    void missing_eta_is_not_overdue() {
        ManagerDashboardService.Summary s = new ManagerDashboardService.Summary();
        s.openMenuBlocks = List.of(request(null, null));

        assertEquals(1, s.getOpenMenuBlockCount());
        assertEquals(0, s.getOverdueMenuBlockCount());
    }

    @Test
    void new_summary_is_safe_for_jsp_el() {
        ManagerDashboardService.Summary s = new ManagerDashboardService.Summary();

        assertNotNull(s.getTodayWaste());
        assertEquals(0, s.getTodayWaste().getTotalCost().compareTo(BigDecimal.ZERO));
        assertEquals(0, s.getOpenMenuBlockCount());
        assertEquals(0, s.getOversoldCount());
        assertFalse(s.isHasOversold());
    }

    @Test
    void oversold_summary_has_dedicated_signal() {
        ManagerDashboardService.Summary s = new ManagerDashboardService.Summary();
        s.oversoldCount = 2;

        assertEquals(2, s.getOversoldCount());
        assertTrue(s.isHasOversold());
    }

    @Test
    void branch_inventory_distinguishes_oversold_from_low_stock() {
        BranchInventory bi = new BranchInventory();
        bi.setQuantityOnHand(new BigDecimal("-0.001"));
        bi.setMinThreshold(BigDecimal.ZERO);

        assertTrue(bi.isOversold());
        assertTrue(bi.isLow());
    }

    private static MenuBlockRequest request(LocalDateTime backInEta, LocalDateTime closedAt) {
        MenuBlockRequest r = new MenuBlockRequest();
        r.setBackInEta(backInEta);
        r.setClosedAt(closedAt);
        return r;
    }
}
