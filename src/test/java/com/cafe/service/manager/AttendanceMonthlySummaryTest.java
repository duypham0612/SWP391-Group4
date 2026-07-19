package com.cafe.service.manager;

import com.cafe.model.MonthlyAttendanceRow;
import com.cafe.model.MonthlyWorkSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertEquals("Vắng", r.getStateLabel());
        assertEquals("badge-served", r.getStateBadge());
        assertEquals(1, s.getAbsentCount());
        assertEquals(0, s.getPendingHours());
    }

    @Test
    void unlockedPayroll_hasNoPay() {
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(row("APPROVED", 8.0)));

        assertFalse(s.isPayrollLocked());
        assertNull(s.getLockedPay());
    }

    @Test
    void lockedPay_usesLockedHoursNotApprovedHours() {
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(row("APPROVED", 16.0)));
        s.setLockedHours(new BigDecimal("160"));
        s.setHourlyRate(new BigDecimal("30000"));

        assertTrue(s.isPayrollLocked());
        assertEquals(new BigDecimal("4800000"), s.getLockedPay());
        assertTrue(s.isHoursMismatch());
    }

    @Test
    void matchingLockedHours_areNotMismatch() {
        MonthlyWorkSummary s = AttendanceService.summarize(List.of(row("APPROVED", 16.0)));
        s.setLockedHours(new BigDecimal("16.0"));
        s.setHourlyRate(new BigDecimal("30000"));

        assertFalse(s.isHoursMismatch());
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
