package com.cafe.controller.barista;

import com.cafe.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WasteServletValidationTest {

    @Test
    void ingredient_reason_must_match_selected_waste_type() {
        assertThrows(BusinessException.class, () ->
                WasteServlet.requireIngredientCause("EXPIRED", "Đổ khi pha", "", 1));
    }

    @Test
    void other_requires_a_real_explanation() {
        assertThrows(BusinessException.class, () ->
                WasteServlet.requireIngredientCause("OTHER", "Khác", "   ", 2));
    }

    @Test
    void inventory_reconciliation_is_not_accepted_as_waste() {
        assertThrows(BusinessException.class, () ->
                WasteServlet.requireIngredientCause("OTHER", "Kiểm kê lệch", "", 1));
    }

    @Test
    void qc_sample_reason_uses_normalized_code() {
        assertEquals("QC_SAMPLE", WasteServlet.requireIngredientCause("OTHER", "Mẫu thử/QC", "", 1));
    }

    /** Không còn đường ghi nào bỏ trống lý do: bỏ trống là chặn, không im lặng suy ra mã nguyên nhân. */
    @Test
    void reason_can_never_be_left_empty() {
        assertThrows(BusinessException.class, () ->
                WasteServlet.requireIngredientCause("SPILL", "", "", 1));
    }

    /** Làm lại món do KDS ghi; gửi thẳng loại REMAKE vào form hao hụt phải bị từ chối. */
    @Test
    void remake_is_not_an_ingredient_waste_type() {
        assertThrows(BusinessException.class, () ->
                WasteServlet.requireIngredientCause("REMAKE", "Đổ khi pha", "", 1));
    }
}
