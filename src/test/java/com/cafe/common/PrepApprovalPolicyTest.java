package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrepApprovalPolicyTest {

    @Test
    void doesNotRequireApprovalAtExactlyOneAndHalfTimesTarget() {
        assertFalse(PrepApprovalPolicy.requiresApproval(new BigDecimal("15"), new BigDecimal("10")));
    }

    @Test
    void requiresApprovalJustAboveOneAndHalfTimesTarget() {
        assertTrue(PrepApprovalPolicy.requiresApproval(new BigDecimal("15.001"), new BigDecimal("10")));
    }

    @Test
    void doesNotRequireApprovalBelowTarget() {
        assertFalse(PrepApprovalPolicy.requiresApproval(new BigDecimal("5"), new BigDecimal("10")));
    }

    @Test
    void doesNotRequireApprovalWhenTargetIsNull() {
        assertFalse(PrepApprovalPolicy.requiresApproval(new BigDecimal("1000"), null));
    }

    @Test
    void doesNotRequireApprovalWhenTargetIsZeroOrNegative() {
        assertFalse(PrepApprovalPolicy.requiresApproval(new BigDecimal("1000"), BigDecimal.ZERO));
    }
}
