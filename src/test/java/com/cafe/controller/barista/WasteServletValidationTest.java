package com.cafe.controller.barista;

import com.cafe.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WasteServletValidationTest {

    @Test
    void ingredient_reason_must_match_selected_waste_type() {
        assertThrows(BusinessException.class, () ->
                WasteServlet.requireIngredientCause("EXPIRED", "Đổ khi pha", "", 1, true));
    }

    @Test
    void other_requires_a_real_explanation() {
        assertThrows(BusinessException.class, () ->
                WasteServlet.requireIngredientCause("OTHER", "Khác", "   ", 2, true));
    }

    @Test
    void inventory_reconciliation_is_not_accepted_as_waste() {
        assertThrows(BusinessException.class, () ->
                WasteServlet.requireIngredientCause("OTHER", "Kiểm kê lệch", "", 1, true));
    }

    @Test
    void qc_sample_and_remake_reason_use_normalized_codes() {
        assertEquals("QC_SAMPLE", WasteServlet.requireIngredientCause("OTHER", "Mẫu thử/QC", "", 1, true));
        assertEquals("WRONG_RECIPE", WasteServlet.requireRemakeCause("wrong_recipe"));
        assertThrows(BusinessException.class, () -> WasteServlet.requireRemakeCause("OTHER"));
    }
}
