package com.cafe.service.barista;

import com.cafe.model.ShiftAssignment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Luật chọn ca nhận phải đúng cả với ca nối đuôi, nhiều người và ca qua đêm. */
class HandoverReceiverTimeTest {
    @Test
    void normalShiftEndsOnItsWorkDate() {
        ShiftAssignment shift = shift(LocalDate.of(2026, 7, 22), "12:00", "17:00");
        assertEquals("2026-07-22T17:00", HandoverService.scheduledEnd(shift).toString());
    }

    @Test
    void overnightShiftEndsOnFollowingDate() {
        ShiftAssignment shift = shift(LocalDate.of(2026, 7, 22), "22:00", "06:00");
        assertEquals("2026-07-23T06:00", HandoverService.scheduledEnd(shift).toString());
    }

    @Test
    void adjacentShiftIsChosenAsReceiver() {
        LocalDate date = LocalDate.of(2026, 7, 22);
        List<ShiftAssignment> picked = HandoverService.pickNextShift(List.of(
                shift(date, "12:00", "17:00", "BARISTA", 12),
                shift(date, "17:00", "22:00", "BARISTA", 17)),
                date.atTime(12, 0));

        assertEquals(List.of(12), userIds(picked));
    }

    @Test
    void overnightShiftReceivesFromEveningShift() {
        LocalDate date = LocalDate.of(2026, 7, 22);
        List<ShiftAssignment> picked = HandoverService.pickNextShift(List.of(
                shift(date, "22:00", "06:00", "BARISTA", 22)),
                date.atTime(22, 0));

        assertEquals(List.of(22), userIds(picked));
    }

    @Test
    void allBaristasOfSameNextShiftAreRecipients() {
        LocalDate date = LocalDate.of(2026, 7, 22);
        List<ShiftAssignment> picked = HandoverService.pickNextShift(List.of(
                shift(date, "12:00", "17:00", "BARISTA", 12),
                shift(date, "12:00", "17:00", "BARISTA", 13)),
                date.atTime(12, 0));

        assertEquals(List.of(12, 13), userIds(picked));
    }

    @Test
    void nonBaristaAssignmentsAreIgnored() {
        LocalDate date = LocalDate.of(2026, 7, 22);
        List<ShiftAssignment> picked = HandoverService.pickNextShift(List.of(
                shift(date, "12:00", "17:00", "CASHIER", 12),
                shift(date, "17:00", "22:00", "BARISTA", 17)),
                date.atTime(12, 0));

        assertEquals(List.of(17), userIds(picked));
    }

    @Test
    void sourceShiftItselfIsNeverItsOwnReceiver() {
        LocalDate date = LocalDate.of(2026, 7, 22);
        ShiftAssignment source = shift(date, "07:00", "12:00", "BARISTA", 7);

        assertTrue(HandoverService.pickNextShift(
                List.of(source), HandoverService.scheduledEnd(source)).isEmpty());
    }

    @Test
    void earlierShiftOnLaterDayLosesToSameDayShift() {
        LocalDate date = LocalDate.of(2026, 7, 22);
        List<ShiftAssignment> picked = HandoverService.pickNextShift(List.of(
                shift(date.plusDays(1), "07:00", "12:00", "BARISTA", 107),
                shift(date, "17:00", "22:00", "BARISTA", 17)),
                date.atTime(12, 0));

        assertEquals(List.of(17), userIds(picked));
    }

    private ShiftAssignment shift(LocalDate date, String start, String end) {
        return shift(date, start, end, null, 0);
    }

    private ShiftAssignment shift(LocalDate date, String start, String end, String role, int userId) {
        ShiftAssignment shift = new ShiftAssignment();
        shift.setWorkDate(date); shift.setStartTime(LocalTime.parse(start)); shift.setEndTime(LocalTime.parse(end));
        shift.setRoleCode(role); shift.setUserId(userId);
        return shift;
    }

    private List<Integer> userIds(List<ShiftAssignment> shifts) {
        return shifts.stream().map(ShiftAssignment::getUserId).toList();
    }
}
