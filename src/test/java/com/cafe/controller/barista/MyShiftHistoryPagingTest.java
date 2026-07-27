package com.cafe.controller.barista;

import com.cafe.service.manager.AttendanceService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Phân trang lịch sử ca làm của barista (màn "Ca làm của tôi"). */
class MyShiftHistoryPagingTest {

    // ----- Số dòng/trang: chỉ nhận mức có trên giao diện -----

    @Test
    void page_size_only_accepts_offered_values() {
        assertEquals(5, MyShiftServlet.normalizePageSize(5));
        assertEquals(10, MyShiftServlet.normalizePageSize(10));
        assertEquals(20, MyShiftServlet.normalizePageSize(20));
        assertEquals(50, MyShiftServlet.normalizePageSize(50));
    }

    @Test
    void page_size_falls_back_when_value_is_not_offered() {
        assertEquals(10, MyShiftServlet.normalizePageSize(7));
        assertEquals(10, MyShiftServlet.normalizePageSize(0));
        assertEquals(10, MyShiftServlet.normalizePageSize(-1));
        assertEquals(10, MyShiftServlet.normalizePageSize(Integer.MAX_VALUE));
    }

    // ----- Toán phân trang -----

    @Test
    void row_range_reflects_the_page_being_shown() {
        AttendanceService.MonthlyAttendancePage page =
                new AttendanceService.MonthlyAttendancePage(List.of(), 26, 2, 10);
        assertEquals(3, page.getTotalPages());
        assertEquals(11, page.getStartRow());
        assertEquals(20, page.getEndRow());
        assertTrue(page.isHasPrevious());
        assertTrue(page.isHasNext());
    }

    @Test
    void last_page_stops_at_the_real_total() {
        AttendanceService.MonthlyAttendancePage page =
                new AttendanceService.MonthlyAttendancePage(List.of(), 26, 3, 10);
        assertEquals(21, page.getStartRow());
        assertEquals(26, page.getEndRow());
        assertFalse(page.isHasNext());
    }

    @Test
    void month_without_matching_shifts_reports_zero_rows_and_a_single_page() {
        AttendanceService.MonthlyAttendancePage page =
                new AttendanceService.MonthlyAttendancePage(List.of(), 0, 1, 10);
        assertEquals(1, page.getTotalPages());
        assertEquals(0, page.getStartRow());
        assertEquals(0, page.getEndRow());
        assertFalse(page.isHasPrevious());
        assertFalse(page.isHasNext());
    }

    @Test
    void pager_shows_at_most_five_numbers_around_the_current_page() {
        AttendanceService.MonthlyAttendancePage page =
                new AttendanceService.MonthlyAttendancePage(List.of(), 100, 10, 5);
        assertEquals(List.of(8, 9, 10, 11, 12), page.getVisiblePages());
    }

    @Test
    void pager_window_stays_inside_the_first_and_last_page() {
        assertEquals(List.of(1, 2, 3, 4, 5),
                new AttendanceService.MonthlyAttendancePage(List.of(), 100, 1, 5).getVisiblePages());
        assertEquals(List.of(16, 17, 18, 19, 20),
                new AttendanceService.MonthlyAttendancePage(List.of(), 100, 20, 5).getVisiblePages());
        assertEquals(List.of(1, 2),
                new AttendanceService.MonthlyAttendancePage(List.of(), 7, 1, 5).getVisiblePages());
    }
}
