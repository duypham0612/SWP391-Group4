package com.cafe.controller.barista;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EightySixServletFilterTest {

    @Test
    void only_supported_states_are_accepted() {
        assertEquals("available", EightySixServlet.normalizeState("AVAILABLE"));
        assertEquals("out", EightySixServlet.normalizeState("out"));
        assertEquals("", EightySixServlet.normalizeState("disabled"));
        assertEquals("", EightySixServlet.normalizeState(null));
    }

    @Test
    void page_size_is_limited_to_ui_options() {
        assertEquals(10, EightySixServlet.normalizePageSize(-1));
        assertEquals(10, EightySixServlet.normalizePageSize(9999));
        assertEquals(20, EightySixServlet.normalizePageSize(20));
        assertEquals(50, EightySixServlet.normalizePageSize(50));
    }
}
