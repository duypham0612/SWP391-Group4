package com.cafe.controller.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phân trang + giữ khoảng ngày và bộ lọc trên màn đối soát của quản lý. */
class WasteReportPagingTest {

    @Test
    void page_size_defaults_to_ten_and_allows_up_to_a_hundred() {
        assertEquals(10, WasteReportServlet.normalizePageSize(10));
        assertEquals(20, WasteReportServlet.normalizePageSize(20));
        assertEquals(50, WasteReportServlet.normalizePageSize(50));
        assertEquals(100, WasteReportServlet.normalizePageSize(100));
    }

    @Test
    void page_size_falls_back_when_value_is_not_offered() {
        assertEquals(10, WasteReportServlet.normalizePageSize(5));
        assertEquals(10, WasteReportServlet.normalizePageSize(0));
        assertEquals(10, WasteReportServlet.normalizePageSize(-3));
        assertEquals(10, WasteReportServlet.normalizePageSize(999999));
    }

    @Test
    void redirect_keeps_date_range_alongside_filters_and_page() {
        assertEquals("/app/manager/reconciliation?from=2026-01-01&to=2026-03-31"
                        + "&wasteType=REMAKE&pageSize=50&page=2",
                WasteReportServlet.buildSelfUrl("/app", "2026-01-01", "2026-03-31", "",
                        "REMAKE", "", 50, 2));
    }

    @Test
    void redirect_omits_empty_filters_but_always_keeps_paging() {
        assertEquals("/app/manager/reconciliation?pageSize=10&page=1",
                WasteReportServlet.buildSelfUrl("/app", "", "", "", "", "", 10, 1));
    }

    @Test
    void redirect_encodes_vietnamese_and_reserved_characters() {
        String url = WasteReportServlet.buildSelfUrl("/app", "", "", "sữa & đá", "", "", 10, 1);
        assertTrue(url.contains("q=s%E1%BB%AFa+%26+%C4%91%C3%A1"), url);
        assertEquals(3, url.split("&", -1).length, url);
    }

    @Test
    void redirect_cannot_be_pushed_to_another_host() {
        String url = WasteReportServlet.buildSelfUrl("/app", "//evil.example/x", "", "", "", "", 10, 1);
        assertTrue(url.startsWith("/app/manager/reconciliation?"), url);
        assertFalse(url.contains("//evil.example/x"), url);
    }

    @Test
    void redirect_keeps_crlf_out_of_the_location_header() {
        String url = WasteReportServlet.buildSelfUrl("/app", "", "", "a\r\nSet-Cookie: x=1", "", "", 10, 1);
        assertFalse(url.contains("\r"), url);
        assertFalse(url.contains("\n"), url);
    }
}
