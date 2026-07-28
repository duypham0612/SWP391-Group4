package com.cafe.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Luật hiện nút nhận thay phải khớp điều kiện ghi nguyên tử trong ShiftHandoverDao. */
class ShiftHandoverClaimableTest {

    @Test
    void orphanHandoverIsClaimableByAnotherBaristaImmediately() {
        ShiftHandover handover = handover(7, "WAITING_RECEIPT", 0);

        handover.applyViewer(9);

        assertTrue(handover.isClaimable());
    }

    @Test
    void recentHandoverWithAssignedRecipientIsNotClaimable() {
        ShiftHandover handover = handover(7, "WAITING_RECEIPT", 1);
        handover.setRecipients(List.of(recipient(8, false)));

        handover.applyViewer(9);

        assertFalse(handover.isClaimable());
    }

    @Test
    void staleHandoverWithNoAcknowledgementIsClaimable() {
        ShiftHandover handover = handover(7, "WAITING_RECEIPT", 5);
        handover.setRecipients(List.of(recipient(8, false)));

        handover.applyViewer(9);

        assertTrue(handover.isClaimable());
    }

    @Test
    void staleHandoverAlreadyAcknowledgedBySomeoneIsNotClaimable() {
        ShiftHandover handover = handover(7, "WAITING_RECEIPT", 5);
        handover.setRecipients(List.of(recipient(8, true), recipient(10, false)));

        handover.applyViewer(9);

        assertFalse(handover.isClaimable());
    }

    @Test
    void assignedRecipientMustUseAcknowledgeInsteadOfClaim() {
        ShiftHandover handover = handover(7, "WAITING_RECEIPT", 5);
        handover.setRecipients(List.of(recipient(9, false)));
        handover.setCurrentUserRecipient(true);

        handover.applyViewer(9);

        assertFalse(handover.isClaimable());
    }

    @Test
    void senderCannotClaimTheirOwnHandover() {
        ShiftHandover handover = handover(7, "WAITING_RECEIPT", 5);

        handover.applyViewer(7);

        assertFalse(handover.isClaimable());
    }

    @Test
    void nonWaitingHandoversAreNeverClaimable() {
        ShiftHandover legacy = handover(7, "LEGACY", 5);
        ShiftHandover inProgress = handover(7, "IN_PROGRESS", 5);

        legacy.applyViewer(9);
        inProgress.applyViewer(9);

        assertFalse(legacy.isClaimable());
        assertFalse(inProgress.isClaimable());
    }

    private static ShiftHandover handover(int createdBy, String status, int hoursAgo) {
        ShiftHandover handover = new ShiftHandover();
        handover.setCreatedBy(createdBy);
        handover.setOverallStatus(status);
        handover.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(hoursAgo));
        return handover;
    }

    private static ShiftHandoverRecipient recipient(int userId, boolean acknowledged) {
        ShiftHandoverRecipient recipient = new ShiftHandoverRecipient();
        recipient.setRecipientUserId(userId);
        if (acknowledged) recipient.setAcknowledgedAt(LocalDateTime.now(ZoneOffset.UTC));
        return recipient;
    }
}
