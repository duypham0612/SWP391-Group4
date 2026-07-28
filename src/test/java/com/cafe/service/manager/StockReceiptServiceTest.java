package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StockReceiptServiceTest {

    @Test
    void quantityAndUnitCostMustBePositive() {
        assertThrows(BusinessException.class,
                () -> StockReceiptService.validateLine(BigDecimal.ZERO, BigDecimal.ONE));
        assertThrows(BusinessException.class,
                () -> StockReceiptService.validateLine(BigDecimal.ONE, BigDecimal.ZERO));
        assertDoesNotThrow(
                () -> StockReceiptService.validateLine(new BigDecimal("0.001"), new BigDecimal("0.05")));
    }
}
