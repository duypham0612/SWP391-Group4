package com.cafe.controller.barista;

import com.cafe.service.shared.InventoryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phân trang + giữ bộ lọc của nhật ký hao hụt (quầy pha chế và đối soát của quản lý). */
class WasteLogPagingTest {

    // ----- Số dòng/trang: chỉ nhận mức có trên giao diện -----

    @Test
    void barista_page_size_only_accepts_offered_values() {
        assertEquals(5, WasteServlet.normalizePageSize(5));
        assertEquals(10, WasteServlet.normalizePageSize(10));
        assertEquals(20, WasteServlet.normalizePageSize(20));
        assertEquals(50, WasteServlet.normalizePageSize(50));
    }

    @Test
    void barista_page_size_falls_back_when_value_is_not_offered() {
        assertEquals(5, WasteServlet.normalizePageSize(7));
        assertEquals(5, WasteServlet.normalizePageSize(0));
        assertEquals(5, WasteServlet.normalizePageSize(-1));
        assertEquals(5, WasteServlet.normalizePageSize(Integer.MAX_VALUE));
    }

    // ----- URL redirect sau POST: giữ nguyên chỗ đang đứng -----

    @Test
    void barista_redirect_keeps_filter_and_page() {
        assertEquals("/app/barista/waste?q=cafe&logType=SPILL&status=ACTIVE&pageSize=20&page=3",
                WasteServlet.buildSelfUrl("/app", "cafe", "SPILL", "ACTIVE", 20, 3));
    }

    @Test
    void barista_redirect_omits_empty_filters_but_always_keeps_paging() {
        assertEquals("/app/barista/waste?pageSize=5&page=1",
                WasteServlet.buildSelfUrl("/app", "", "", "", 5, 1));
    }

    @Test
    void barista_redirect_encodes_vietnamese_and_reserved_characters() {
        String url = WasteServlet.buildSelfUrl("/app", "sữa & đá", "", "", 5, 1);
        assertTrue(url.contains("q=s%E1%BB%AFa+%26+%C4%91%C3%A1"), url);
        // Ký tự & trong từ khoá đã thành %26 nên URL vẫn chỉ có đúng 3 tham số: q, pageSize, page.
        assertEquals(3, url.split("&", -1).length, url);
    }

    @Test
    void barista_redirect_cannot_be_pushed_to_another_host() {
        String url = WasteServlet.buildSelfUrl("/app", "//evil.example/x", "", "", 5, 1);
        assertTrue(url.startsWith("/app/barista/waste?"), url);
        assertFalse(url.contains("//evil.example/x"), url);
    }

    @Test
    void barista_redirect_keeps_crlf_out_of_the_location_header() {
        String url = WasteServlet.buildSelfUrl("/app", "a\r\nSet-Cookie: x=1", "", "", 5, 1);
        assertFalse(url.contains("\r"), url);
        assertFalse(url.contains("\n"), url);
    }

    // ----- Toán phân trang -----

    @Test
    void row_range_reflects_the_page_being_shown() {
        InventoryService.WasteLogPage page = new InventoryService.WasteLogPage(List.of(), 38, 2, 10);
        assertEquals(4, page.getTotalPages());
        assertEquals(11, page.getStartRow());
        assertEquals(20, page.getEndRow());
        assertTrue(page.isHasPrevious());
        assertTrue(page.isHasNext());
    }

    @Test
    void last_page_stops_at_the_real_total() {
        InventoryService.WasteLogPage page = new InventoryService.WasteLogPage(List.of(), 38, 4, 10);
        assertEquals(31, page.getStartRow());
        assertEquals(38, page.getEndRow());
        assertFalse(page.isHasNext());
    }

    @Test
    void empty_log_reports_zero_rows_and_a_single_page() {
        InventoryService.WasteLogPage page = new InventoryService.WasteLogPage(List.of(), 0, 1, 5);
        assertEquals(1, page.getTotalPages());
        assertEquals(0, page.getStartRow());
        assertEquals(0, page.getEndRow());
        assertFalse(page.isHasPrevious());
        assertFalse(page.isHasNext());
    }

    @Test
    void pager_shows_at_most_five_numbers_around_the_current_page() {
        InventoryService.WasteLogPage page = new InventoryService.WasteLogPage(List.of(), 200, 10, 5);
        assertEquals(List.of(8, 9, 10, 11, 12), page.getVisiblePages());
    }

    @Test
    void pager_window_stays_inside_the_first_and_last_page() {
        assertEquals(List.of(1, 2, 3, 4, 5),
                new InventoryService.WasteLogPage(List.of(), 200, 1, 5).getVisiblePages());
        assertEquals(List.of(36, 37, 38, 39, 40),
                new InventoryService.WasteLogPage(List.of(), 200, 40, 5).getVisiblePages());
        assertEquals(List.of(1, 2),
                new InventoryService.WasteLogPage(List.of(), 7, 1, 5).getVisiblePages());
    }
}
