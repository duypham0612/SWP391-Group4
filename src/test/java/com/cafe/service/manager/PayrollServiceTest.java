package com.cafe.service.manager;

import com.cafe.model.PayrollRow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollServiceTest {

    @Test
    void salaryKeepsAggregatedPerShiftValueInsteadOfRecomputingFromRoundedHours() {
        PayrollRow row = new PayrollRow();
        row.setTotalHours(3.3);
        row.setHourlyRate(new BigDecimal("30909.09"));
        row.setSalary(new BigDecimal("102000.00"));

        assertEquals(new BigDecimal("102000"), row.getSalary());
    }

    @Test
    void nullAggregatesBecomeZeroForLegacyRowsWithoutAnyRate() {
        PayrollRow row = new PayrollRow();
        row.setHourlyRate(null);
        row.setSalary(null);

        assertEquals(BigDecimal.ZERO, row.getHourlyRate());
        assertEquals(BigDecimal.ZERO.setScale(0), row.getSalary());
    }
}
