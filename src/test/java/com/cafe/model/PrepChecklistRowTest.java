package com.cafe.model;

import org.junit.jupiter.api.Test;
import com.cafe.web.viewmodel.ViewFormatter;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PrepChecklistRowTest {
    private final ViewFormatter view = new ViewFormatter();

    @Test
    void suggests_target_minus_stock_only_when_stock_hits_threshold() {
        PrepChecklistRow row = new PrepChecklistRow(1, "Cold Brew", "ml",
                new BigDecimal("200"), new BigDecimal("300"), new BigDecimal("1000"),
                true, 1440);

        assertTrue(row.isNeedPrep());
        assertTrue(row.isReadyToPrep());
        assertEquals(new BigDecimal("800"), row.getSuggestedQty());
    }

    @Test
    void does_not_create_task_above_threshold_or_when_target_missing() {
        PrepChecklistRow above = new PrepChecklistRow(1, "Cold Brew", "ml",
                new BigDecimal("301"), new BigDecimal("300"), new BigDecimal("1000"),
                true, 1440);
        PrepChecklistRow noTarget = new PrepChecklistRow(1, "Cold Brew", "ml",
                new BigDecimal("200"), new BigDecimal("300"), null,
                true, 1440);

        assertFalse(above.isNeedPrep());
        assertFalse(noTarget.isNeedPrep());
        assertEquals("Manager chưa đặt mức tồn mục tiêu.", view.checklistBlockedReason(noTarget));
    }

    @Test
    void oversold_stock_is_blocked_until_manager_reconciles() {
        PrepChecklistRow row = new PrepChecklistRow(1, "Cold Brew", "ml",
                new BigDecimal("-10"), new BigDecimal("300"), new BigDecimal("1000"),
                true, 1440);

        assertTrue(row.isOversold());
        assertFalse(row.isNeedPrep());
        assertFalse(row.isReadyToPrep());
        assertEquals("Tồn đang âm — cần Manager kiểm kê.", view.checklistBlockedReason(row));
    }

    @Test
    void missing_recipe_or_shelf_life_is_not_ready() {
        PrepChecklistRow noRecipe = new PrepChecklistRow(1, "Cold Brew", "ml",
                new BigDecimal("200"), new BigDecimal("300"), new BigDecimal("1000"),
                false, 1440);
        PrepChecklistRow noShelf = new PrepChecklistRow(1, "Cold Brew", "ml",
                new BigDecimal("200"), new BigDecimal("300"), new BigDecimal("1000"),
                true, null);

        assertFalse(noRecipe.isReadyToPrep());
        assertEquals("Admin chưa khai báo công thức.", view.checklistBlockedReason(noRecipe));
        assertFalse(noShelf.isReadyToPrep());
        assertEquals("Admin chưa đặt hạn bảo quản.", view.checklistBlockedReason(noShelf));
    }
}
