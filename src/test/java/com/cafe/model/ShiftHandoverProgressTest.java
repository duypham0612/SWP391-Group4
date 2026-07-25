package com.cafe.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tiến độ và tuổi bàn giao hiển thị trên thẻ — tính từ dữ liệu đã nạp, không đi DB thêm lần nào. */
class ShiftHandoverProgressTest {

    @Test
    void progressCountsOnlyDoneTasks() {
        ShiftHandover h = handoverWithTasks("NEW", "IN_PROGRESS", "DONE", "DONE");
        assertEquals(4, h.getTaskCount());
        assertEquals(2, h.getDoneTaskCount());
        assertEquals(2, h.getOpenTaskCount());
        assertEquals(50, h.getProgressPercent());
    }

    @Test
    void progressIsZeroWithoutTasksInsteadOfDividingByZero() {
        ShiftHandover h = new ShiftHandover();
        assertEquals(0, h.getTaskCount());
        assertEquals(0, h.getProgressPercent());
    }

    @Test
    void progressRoundsDownSoItNeverReadsHundredBeforeEveryTaskIsDone() {
        ShiftHandover h = handoverWithTasks("DONE", "DONE", "NEW");
        assertEquals(66, h.getProgressPercent());
    }

    @Test
    void awaitingReceiptUntilEveryRecipientAcknowledges() {
        ShiftHandover h = new ShiftHandover();
        h.setRecipients(List.of(recipient(true), recipient(false)));
        assertEquals(1, h.getAcknowledgedCount());
        assertEquals(2, h.getRecipientCount());
        assertTrue(h.isAwaitingReceipt());

        h.setRecipients(List.of(recipient(true), recipient(true)));
        assertFalse(h.isAwaitingReceipt());
    }

    @Test
    void handoverWithoutRecipientsIsNotReportedAsAwaitingReceipt() {
        assertFalse(new ShiftHandover().isAwaitingReceipt());
    }

    @Test
    void ageDisplayScalesFromMinutesToDays() {
        assertEquals("vừa xong", handoverCreatedMinutesAgo(0).getAgeDisplay());
        assertEquals("45 phút trước", handoverCreatedMinutesAgo(45).getAgeDisplay());
        assertEquals("3 giờ trước", handoverCreatedMinutesAgo(3 * 60 + 10).getAgeDisplay());
        assertEquals("2 ngày trước", handoverCreatedMinutesAgo(2 * 24 * 60 + 30).getAgeDisplay());
    }

    @Test
    void ageDisplayIsBlankWhenCreatedAtIsMissing() {
        assertEquals("", new ShiftHandover().getAgeDisplay());
    }

    private static ShiftHandover handoverCreatedMinutesAgo(long minutes) {
        ShiftHandover h = new ShiftHandover();
        h.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(minutes));
        return h;
    }

    private static ShiftHandover handoverWithTasks(String... statuses) {
        ShiftHandover h = new ShiftHandover();
        List<ShiftHandoverTask> tasks = new java.util.ArrayList<>();
        for (String status : statuses) { ShiftHandoverTask t = new ShiftHandoverTask(); t.setStatus(status); tasks.add(t); }
        h.setTasks(tasks);
        return h;
    }

    private static ShiftHandoverRecipient recipient(boolean acknowledged) {
        ShiftHandoverRecipient r = new ShiftHandoverRecipient();
        if (acknowledged) r.setAcknowledgedAt(LocalDateTime.now(ZoneOffset.UTC));
        return r;
    }
}
