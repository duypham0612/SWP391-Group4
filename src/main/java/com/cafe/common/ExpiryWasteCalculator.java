package com.cafe.common;

import com.cafe.model.PrepBatch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tinh so luong hao hut goi y cho me pha san da het han. */
public final class ExpiryWasteCalculator {

    private ExpiryWasteCalculator() {
    }

    /**
     * Phan bo luong hao hut goi y cho ca danh sach me qua han theo FIFO (het han som nhat truoc).
     * Ton kho ghi nhan theo nguyen lieu chu khong theo tung me: neu moi me deu lay min(san luong, ton)
     * thi tong goi y cua nhieu me cung nguyen lieu se vuot ton thuc va lam am kho khi barista lam theo.
     * Danh sach phai duoc sap xep san theo ExpiresAt tang dan (PrepBatchDao.findExpiredActive).
     */
    public static void allocateSuggestedWaste(List<PrepBatch> expiredBatches, LocalDateTime nowUtc) {
        if (expiredBatches == null || expiredBatches.isEmpty()) return;
        Map<Integer, BigDecimal> remainingByIngredient = new HashMap<>();
        for (PrepBatch batch : expiredBatches) {
            if (batch == null) continue;
            BigDecimal remaining = remainingByIngredient.containsKey(batch.getPreppedIngredientId())
                    ? remainingByIngredient.get(batch.getPreppedIngredientId())
                    : nonNegative(batch.getBranchQuantityOnHand());
            BigDecimal suggested = suggestedWasteQuantity(batch, nowUtc);
            if (suggested.compareTo(remaining) > 0) suggested = remaining;
            batch.setSuggestedWasteQuantity(suggested);
            remainingByIngredient.put(batch.getPreppedIngredientId(), remaining.subtract(suggested));
        }
    }

    public static void allocateSuggestedWaste(List<PrepBatch> expiredBatches) {
        allocateSuggestedWaste(expiredBatches, LocalDateTime.now(ZoneOffset.UTC));
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
