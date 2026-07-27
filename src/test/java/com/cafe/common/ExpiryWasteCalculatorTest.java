package com.cafe.common;

import com.cafe.model.PrepBatch;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpiryWasteCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 19, 12, 0);

    private static PrepBatch expired(String produced, String onHand) {
        PrepBatch batch = new PrepBatch();
        batch.setStatus("ACTIVE");
        batch.setExpiresAt(NOW.minusMinutes(1));
        batch.setQuantityProduced(new BigDecimal(produced));
        batch.setBranchQuantityOnHand(new BigDecimal(onHand));
        return batch;
    }

    private static PrepBatch expired(int preppedIngredientId, String produced, String onHand) {
        PrepBatch batch = expired(produced, onHand);
        batch.setPreppedIngredientId(preppedIngredientId);
        return batch;
    }

    private static void assertQty(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void capsAtProducedQuantityWhenStockIsHigher() {
        assertQty("10", ExpiryWasteCalculator.suggestedWasteQuantity(expired("10", "15"), NOW));
    }

    @Test
    void capsAtOnHandWhenStockIsLower() {
        assertQty("6.5", ExpiryWasteCalculator.suggestedWasteQuantity(expired("10", "6.5"), NOW));
    }

    @Test
    void returnsZeroWhenOnHandIsZero() {
        assertQty("0", ExpiryWasteCalculator.suggestedWasteQuantity(expired("10", "0"), NOW));
    }

    @Test
    void clampsNegativeStockToZero() {
        assertQty("0", ExpiryWasteCalculator.suggestedWasteQuantity(expired("10", "-2"), NOW));
    }

    @Test
    void returnsZeroWhenExpiryIsMissing() {
        PrepBatch batch = expired("10", "8");
        batch.setExpiresAt(null);

        assertQty("0", ExpiryWasteCalculator.suggestedWasteQuantity(batch, NOW));
    }

    @Test
    void allocationSplitsSharedStockSoTotalNeverExceedsOnHand() {
        PrepBatch first = expired(7, "5", "6");
        PrepBatch second = expired(7, "5", "6");

        ExpiryWasteCalculator.allocateSuggestedWaste(List.of(first, second), NOW);

        assertQty("5", first.getSuggestedWasteQuantity());   // mẻ hết hạn sớm nhất được ưu tiên
        assertQty("1", second.getSuggestedWasteQuantity());  // chỉ còn 1 trong tồn cho mẻ sau
    }

    @Test
    void allocationKeepsIngredientsIndependent() {
        PrepBatch coldBrew = expired(7, "5", "5");
        PrepBatch syrup = expired(9, "3", "3");

        ExpiryWasteCalculator.allocateSuggestedWaste(List.of(coldBrew, syrup), NOW);

        assertQty("5", coldBrew.getSuggestedWasteQuantity());
        assertQty("3", syrup.getSuggestedWasteQuantity());
    }

    @Test
    void allocationLeavesNothingForLaterBatchesWhenStockIsUsedUp() {
        PrepBatch first = expired(7, "8", "8");
        PrepBatch second = expired(7, "4", "8");

        ExpiryWasteCalculator.allocateSuggestedWaste(List.of(first, second), NOW);

        assertQty("8", first.getSuggestedWasteQuantity());
        assertQty("0", second.getSuggestedWasteQuantity());
        assertEquals(false, second.isHasSuggestedWaste());
    }

    @Test
    void allocationSkipsBatchesThatAreNotExpired() {
        PrepBatch stillGood = expired(7, "5", "6");
        stillGood.setExpiresAt(NOW.plusHours(3));
        PrepBatch reallyExpired = expired(7, "5", "6");

        ExpiryWasteCalculator.allocateSuggestedWaste(List.of(stillGood, reallyExpired), NOW);

        assertQty("0", stillGood.getSuggestedWasteQuantity());
        assertQty("5", reallyExpired.getSuggestedWasteQuantity());   // mẻ còn hạn không ăn mất phần tồn
    }
}
