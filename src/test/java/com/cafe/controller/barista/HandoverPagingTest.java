package com.cafe.controller.barista;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** URL quay lại sau POST phải giữ nguyên bộ lọc + trang, và neo về đúng thẻ vừa thao tác. */
class HandoverPagingTest {

    @Test
    void pageSizeFallsBackToDefaultForValuesNotOnTheUi() {
        assertEquals(5, ShiftHandoverServlet.normalizePageSize(1));
        assertEquals(5, ShiftHandoverServlet.normalizePageSize(7));
        assertEquals(5, ShiftHandoverServlet.normalizePageSize(1000));
        assertEquals(10, ShiftHandoverServlet.normalizePageSize(10));
        assertEquals(20, ShiftHandoverServlet.normalizePageSize(20));
        assertEquals(50, ShiftHandoverServlet.normalizePageSize(50));
    }

    @Test
    void emptyFiltersAreOmittedFromTheUrl() {
        assertEquals("/barista/handover?pageSize=5&page=1",
                ShiftHandoverServlet.buildSelfUrl("", "", "", 5, 1, null));
    }

    @Test
    void filtersAndPageSurviveTheRedirect() {
        assertEquals("/barista/handover?q=may+xay&scope=MINE&state=IN_PROGRESS&pageSize=20&page=3",
                ShiftHandoverServlet.buildSelfUrl("may xay", "MINE", "IN_PROGRESS", 20, 3, null));
    }

    @Test
    void queryIsUrlEncoded() {
        assertEquals("/barista/handover?q=c%C3%A0+ph%C3%AA%26sua&pageSize=5&page=1",
                ShiftHandoverServlet.buildSelfUrl("cà phê&sua", "", "", 5, 1, null));
    }

    @Test
    void focusIdBecomesAnchorAfterTheQueryString() {
        assertEquals("/barista/handover?scope=SENT&pageSize=10&page=2#h42",
                ShiftHandoverServlet.buildSelfUrl("", "SENT", "", 10, 2, 42));
    }

    @Test
    void nonPositiveFocusIdAddsNoAnchor() {
        assertEquals("/barista/handover?pageSize=5&page=1",
                ShiftHandoverServlet.buildSelfUrl("", "", "", 5, 1, 0));
    }
}
