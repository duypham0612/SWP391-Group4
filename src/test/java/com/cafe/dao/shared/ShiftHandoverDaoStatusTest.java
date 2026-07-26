package com.cafe.dao.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Luật suy trạng thái bàn giao là nghiệp vụ thuần, không cần kết nối cơ sở dữ liệu để kiểm thử. */
class ShiftHandoverDaoStatusTest {

    @Test
    void allTasksDoneCompletesAfterAtLeastOneRecipientAcknowledges() {
        assertEquals("COMPLETED", ShiftHandoverDao.overallStatus(1, 5, 5));
    }

    @Test
    void completedTasksStillWaitWhenNobodyAcknowledged() {
        assertEquals("WAITING_RECEIPT", ShiftHandoverDao.overallStatus(0, 5, 5));
    }

    @Test
    void acknowledgedHandoverRemainsInProgressWhileTasksAreOpen() {
        assertEquals("IN_PROGRESS", ShiftHandoverDao.overallStatus(1, 5, 4));
    }

    @Test
    void handoverWithoutTasksDoesNotComplete() {
        assertEquals("IN_PROGRESS", ShiftHandoverDao.overallStatus(1, 0, 0));
    }
}
