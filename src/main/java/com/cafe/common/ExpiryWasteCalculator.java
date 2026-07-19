package com.cafe.common;

import com.cafe.model.PrepBatch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Tinh so luong hao hut goi y cho me pha san da het han. */
public final class ExpiryWasteCalculator {

    private ExpiryWasteCalculator() {
    }

    public static BigDecimal suggestedWasteQuantity(PrepBatch batch) {
        return suggestedWasteQuantity(batch, LocalDateTime.now(ZoneOffset.UTC));
    }

    public static BigDecimal suggestedWasteQuantity(PrepBatch batch, LocalDateTime nowUtc) {
        if (batch == null || nowUtc == null) return BigDecimal.ZERO;
        if (!batch.isActive() || batch.getExpiresAt() == null || !batch.getExpiresAt().isBefore(nowUtc)) {
            return BigDecimal.ZERO;
        }
        BigDecimal produced = nonNegative(batch.getQuantityProduced());
        BigDecimal onHand = nonNegative(batch.getBranchQuantityOnHand());
        return produced.compareTo(onHand) <= 0 ? produced : onHand;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() <= 0) return BigDecimal.ZERO;
        return value;
    }
}
