package com.cafe.service.shared;

import com.cafe.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BranchMenuServicePriceTest {

    @Test
    void localPriceMustBePositiveWholeVietnameseDongOrNull() {
        assertDoesNotThrow(() -> BranchMenuService.validateLocalPrice(null));
        assertDoesNotThrow(() -> BranchMenuService.validateLocalPrice(new BigDecimal("23200")));
        assertThrows(BusinessException.class,
                () -> BranchMenuService.validateLocalPrice(BigDecimal.ZERO));
        assertThrows(BusinessException.class,
                () -> BranchMenuService.validateLocalPrice(new BigDecimal("23.5")));
    }
}
