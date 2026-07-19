package com.cafe.common;

import com.cafe.model.StockAdjustment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecountValidatorTest {

    @Test
    void null_arrays_mean_no_recount_lines() {
        assertTrue(RecountValidator.parse(null, null).isEmpty());
    }

    @Test
    void blank_actual_qty_is_skipped() {
        List<StockAdjustment> lines = RecountValidator.parse(
                new String[]{"10", "11"},
                new String[]{"   ", "2.5"});

        assertEquals(1, lines.size());
        assertEquals(11, lines.get(0).getIngredientId());
        assertEquals(0, lines.get(0).getActualQty().compareTo(new BigDecimal("2.5")));
    }

    @Test
    void zero_actual_qty_creates_adjustment_line() {
        List<StockAdjustment> lines = RecountValidator.parse(
                new String[]{"10"},
                new String[]{"0"});

        assertEquals(1, lines.size());
        assertEquals(10, lines.get(0).getIngredientId());
        assertEquals(BigDecimal.ZERO, lines.get(0).getActualQty());
    }

    @Test
    void negative_actual_qty_is_rejected() {
        BusinessException e = assertThrows(BusinessException.class, () -> RecountValidator.parse(
                new String[]{"10"},
                new String[]{"-0.1"}));
        assertTrue(e.getMessage().contains("không được âm"));
    }

    @Test
    void invalid_actual_qty_is_rejected() {
        BusinessException e = assertThrows(BusinessException.class, () -> RecountValidator.parse(
                new String[]{"10"},
                new String[]{"abc"}));
        assertTrue(e.getMessage().contains("không hợp lệ"));
    }

    @Test
    void mismatched_arrays_are_rejected() {
        BusinessException e = assertThrows(BusinessException.class, () -> RecountValidator.parse(
                new String[]{"10", "11"},
                new String[]{"1"}));
        assertTrue(e.getMessage().contains("không khớp"));
    }

    @Test
    void invalid_ingredient_id_with_value_is_rejected() {
        BusinessException e = assertThrows(BusinessException.class, () -> RecountValidator.parse(
                new String[]{"x"},
                new String[]{"1"}));
        assertTrue(e.getMessage().contains("Nguyên liệu"));
    }

    @Test
    void duplicate_ingredient_ids_are_rejected() {
        BusinessException e = assertThrows(BusinessException.class, () -> RecountValidator.parse(
                new String[]{"10", "10"},
                new String[]{"1", "2"}));
        assertTrue(e.getMessage().contains("trùng"));
    }
}
