package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.model.StockReceipt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void supplierIsRequiredBeforeCreatingOrConfirmingAReceipt() {
        StockReceipt receipt = new StockReceipt();

        BusinessException missing = assertThrows(BusinessException.class,
                () -> StockReceiptService.validateSupplier(receipt));
        assertEquals("Vui lòng chọn nhà cung cấp trước khi tạo hoặc xác nhận phiếu nhập kho.",
                missing.getMessage());

        receipt.setSupplierId(1);
        assertDoesNotThrow(() -> StockReceiptService.validateSupplier(receipt));
    }
}
