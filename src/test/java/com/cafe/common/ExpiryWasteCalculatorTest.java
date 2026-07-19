package com.cafe.common;

import com.cafe.model.PrepBatch;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
