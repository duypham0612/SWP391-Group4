package com.cafe.service.shared;

import com.cafe.model.BranchMenuItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchMenuServicePageTest {

    @Test
    void page_reports_filtered_range_and_navigation() {
        BranchMenuService.MenuAvailabilityPage page =
                new BranchMenuService.MenuAvailabilityPage(List.of(item(11), item(12)), 22, 2, 10);

        assertEquals(2, page.getPage());
        assertEquals(3, page.getTotalPages());
        assertEquals(11, page.getStartRow());
        assertEquals(20, page.getEndRow());
        assertTrue(page.isHasPrevious());
        assertTrue(page.isHasNext());
    }

    @Test
    void page_beyond_filtered_result_is_clamped_to_last_page() {
        BranchMenuService.MenuAvailabilityPage page =
                new BranchMenuService.MenuAvailabilityPage(List.of(item(21), item(22)), 22, 99, 10);

        assertEquals(3, page.getPage());
        assertEquals(21, page.getStartRow());
        assertEquals(22, page.getEndRow());
        assertFalse(page.isHasNext());
    }

    @Test
    void empty_result_keeps_stable_page_metadata() {
        BranchMenuService.MenuAvailabilityPage page =
                new BranchMenuService.MenuAvailabilityPage(List.of(), 0, 8, 10);

        assertEquals(1, page.getPage());
        assertEquals(1, page.getTotalPages());
        assertEquals(0, page.getStartRow());
        assertEquals(0, page.getEndRow());
        assertFalse(page.isHasPrevious());
        assertFalse(page.isHasNext());
    }

    @Test
    void visible_page_window_stays_compact() {
        BranchMenuService.MenuAvailabilityPage page =
                new BranchMenuService.MenuAvailabilityPage(List.of(), 100, 6, 10);

        assertEquals(List.of(4, 5, 6, 7, 8), page.getVisiblePages());
    }

    private static BranchMenuItem item(int id) {
        BranchMenuItem item = new BranchMenuItem();
        item.setProductId(id);
        return item;
    }
}
