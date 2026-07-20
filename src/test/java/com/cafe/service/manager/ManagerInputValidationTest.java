package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.model.Payroll;
import com.cafe.model.Supplier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagerInputValidationTest {

    @Test
    void receiptLineRejectsNonPositiveQuantityAndNegativeCost() {
        assertThrows(BusinessException.class,
                () -> StockReceiptService.validateLine(1, BigDecimal.ZERO, BigDecimal.ZERO, "kg"));
        assertThrows(BusinessException.class,
                () -> StockReceiptService.validateLine(1, BigDecimal.ONE, new BigDecimal("-1"), "kg"));
        assertDoesNotThrow(() -> StockReceiptService.validateLine(
                1, new BigDecimal("2.5"), new BigDecimal("10000"), "kg"));
    }

    @Test
    void payrollRejectsNegativeOrImpossibleMonthlyHours() {
        Payroll negative = payroll("-1", "25000");
        Payroll impossible = payroll("745", "25000");
        assertThrows(BusinessException.class, () -> PayrollService.validateHourlyRates(List.of(negative)));
        assertThrows(BusinessException.class, () -> PayrollService.validateHourlyRates(List.of(impossible)));
    }

    @Test
    void supplierRejectsMalformedPhoneAndTrimsValidInput() {
        Supplier bad = supplier("Nhà cung cấp", "abc", "Địa chỉ");
        assertThrows(BusinessException.class, () -> SupplierService.validate(bad));

        Supplier valid = supplier("  Nhà cung cấp A  ", " 0901234567 ", "  Hà Nội  ");
        assertDoesNotThrow(() -> SupplierService.validate(valid));
        org.junit.jupiter.api.Assertions.assertEquals("Nhà cung cấp A", valid.getName());
        org.junit.jupiter.api.Assertions.assertEquals("0901234567", valid.getPhone());
    }

    private Payroll payroll(String hours, String rate) {
        Payroll p = new Payroll();
        p.setWorkedHours(new BigDecimal(hours));
        p.setHourlyRate(new BigDecimal(rate));
        return p;
    }

    private Supplier supplier(String name, String phone, String address) {
        Supplier s = new Supplier();
        s.setName(name); s.setPhone(phone); s.setAddress(address); s.setActive(true);
        return s;
    }
}
