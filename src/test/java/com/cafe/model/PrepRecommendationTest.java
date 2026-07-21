package com.cafe.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrepRecommendationTest {
    @Test
    void recommendationAddsSafetyThresholdAndOpenDemandThenSubtractsStock() {
        assertEquals(0, new BigDecimal("7.5").compareTo(
                PrepRecommendation.calculate(new BigDecimal("4.5"), new BigDecimal("2"), new BigDecimal("10"))));
    }

    @Test
    void recommendationNeverGoesNegativeWhenStockCoversDemandAndThreshold() {
        assertEquals(0, BigDecimal.ZERO.compareTo(
                PrepRecommendation.calculate(new BigDecimal("20"), new BigDecimal("2"), new BigDecimal("3"))));
    }
}
