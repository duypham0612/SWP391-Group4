package com.cafe.service.manager;

import com.cafe.model.MonthlyAttendanceRow;
import com.cafe.model.MonthlyWorkSummary;
import com.cafe.web.viewmodel.ViewFormatter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttendanceMonthlySummaryTest {

    @Test
    void approvedHoursAndWorkedShifts_areSummed() {
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(
                row("APPROVED", 4.0),
                row("APPROVED", 4.0),
                row("APPROVED", 8.0)));

        assertEquals(16.0, s.getApprovedHours());
        assertEquals(3, s.getShiftsWorked());
    }

    @Test
    void pendingAndRejected_areSeparatedFromApproved() {
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(
                row("APPROVED", 16.0),
                row("PENDING", 5.0),
                row("REJECTED", 6.0)));

        assertEquals(16.0, s.getApprovedHours());
        assertEquals(5.0, s.getPendingHours());
        assertEquals(6.0, s.getRejectedHours());
    }

    @Test
    void absentRows_doNotIncreaseWorkedShifts() {
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(absent(null)));

        assertEquals(1, s.getAbsentCount());
        assertEquals(0, s.getShiftsWorked());
    }

    @Test
    void openRows_doNotIncreaseWorkedShifts() {
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(open()));

        assertEquals(1, s.getOpenCount());
        assertEquals(0, s.getShiftsWorked());
    }

    @Test
    void rowWithStatusButNoCheckIn_isAbsent() {
        MonthlyAttendanceRow r = absent("PENDING");
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(r));

        ViewFormatter view = new ViewFormatter();
        assertEquals("Vắng", view.attendanceState(r));
        assertEquals("badge-served", view.attendanceBadge(r));
        assertEquals(1, s.getAbsentCount());
        assertEquals(0, s.getPendingHours());
    }

    @Test
    void avgHoursPerShift_handlesNoWorkedShifts() {
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(absent(null)));

        assertEquals(0d, s.getAvgHoursPerShift());
    }

    @Test
    void totalsRoundOnceAfterSumming() {
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(
                row("APPROVED", 2.05),
                row("APPROVED", 2.05),
                row("APPROVED", 2.05)));

        assertEquals(6.2, s.getApprovedHours());
    }

    private static MonthlyAttendanceRow row(String status, double hours) {
        MonthlyAttendanceRow r = new MonthlyAttendanceRow();
        r.setCheckInAt(LocalDateTime.parse("2026-07-19T01:00:00"));
        r.setCheckOutAt(LocalDateTime.parse("2026-07-19T05:00:00"));
        r.setStatus(status);
        r.setWorkHours(hours);
        return r;
    }

    private static MonthlyAttendanceRow absent(String status) {
        MonthlyAttendanceRow r = new MonthlyAttendanceRow();
        r.setStatus(status);
        return r;
    }

    private static MonthlyAttendanceRow open() {
        MonthlyAttendanceRow r = new MonthlyAttendanceRow();
        r.setCheckInAt(LocalDateTime.parse("2026-07-19T01:00:00"));
        r.setStatus("PENDING");
        return r;
    }
}
