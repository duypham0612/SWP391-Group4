package com.cafe.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BranchAccessPolicyTest {

    @Test
    void stopped_branch_is_blocked_before_manager_assignment_check() {
        assertEquals(
                BranchAccessPolicy.BRANCH_STOPPED_MESSAGE,
                BranchAccessPolicy.blockedMessage(false, false));
    }

    @Test
    void active_branch_without_manager_is_blocked() {
        assertEquals(
                BranchAccessPolicy.BRANCH_UNMANAGED_MESSAGE,
                BranchAccessPolicy.blockedMessage(true, false));
    }

    @Test
    void active_branch_with_manager_is_allowed() {
        assertNull(BranchAccessPolicy.blockedMessage(true, true));
    }
}
