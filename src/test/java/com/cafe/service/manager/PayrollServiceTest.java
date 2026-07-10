package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.model.Payroll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PayrollServiceTest {

    @Test
    void hourlyRateBelowMinimum_isRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PayrollService.validateHourlyRates(List.of(payroll(3, "24999"))));

        assertEquals("Lương cơ bản phải lớn hơn hoặc bằng 25.000₫/giờ.", ex.getMessage());
    }

    @Test
    void nullHourlyRate_isRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PayrollService.validateHourlyRates(List.of(payroll(5, null))));

        assertEquals("Lương cơ bản phải lớn hơn hoặc bằng 25.000₫/giờ.", ex.getMessage());
    }

    @Test
    void zeroHourlyRate_isRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> PayrollService.validateHourlyRates(List.of(payroll(7, "0"))));

        assertEquals("Lương cơ bản phải lớn hơn hoặc bằng 25.000₫/giờ.", ex.getMessage());
    }

    @Test
    void hourlyRateAtMinimum_isAccepted() {
        assertDoesNotThrow(() -> PayrollService.validateHourlyRates(List.of(payroll(11, "25000"))));
    }

    private Payroll payroll(int userId, String hourlyRate) {
        Payroll p = new Payroll();
        p.setUserId(userId);
        p.setHourlyRate(hourlyRate == null ? null : new BigDecimal(hourlyRate));
        return p;
    }
}
