package com.cafe.service.shared;

import com.cafe.common.BusinessDay;
import com.cafe.model.Voucher;
import com.cafe.web.viewmodel.ViewFormatter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VoucherUtcBoundaryTest {

    @Test
    void voucher_uses_vietnam_input_but_validates_half_open_utc_range() {
        LocalDateTime startVn = LocalDateTime.of(2026, 7, 20, 0, 0);
        LocalDateTime endVn = LocalDateTime.of(2026, 7, 20, 7, 0);
        Voucher voucher = new Voucher();
        voucher.setActive(true);
        voucher.setScope("CHAIN");
        voucher.setMinOrderAmount(BigDecimal.ZERO);
        voucher.setStartAtUtc(BusinessDay.toUtc(startVn));
        voucher.setEndAtUtc(BusinessDay.toUtc(endVn));

        ViewFormatter view = new ViewFormatter();
        assertEquals("2026-07-20T00:00", view.voucherInput(voucher.getStartAtUtc()));
        assertEquals("2026-07-20T07:00", view.voucherInput(voucher.getEndAtUtc()));
        assertEquals("Voucher chưa tới ngày áp dụng.", VoucherService.validateVoucherRecordAt(
                voucher, 1, BigDecimal.ZERO, BusinessDay.toUtc(startVn.minusMinutes(1))));
        assertNull(VoucherService.validateVoucherRecordAt(
                voucher, 1, BigDecimal.ZERO, BusinessDay.toUtc(startVn)));
        assertNull(VoucherService.validateVoucherRecordAt(
                voucher, 1, BigDecimal.ZERO,
                BusinessDay.toUtc(LocalDateTime.of(2026, 7, 20, 6, 59))));
        assertEquals("Voucher đã hết hạn.", VoucherService.validateVoucherRecordAt(
                voucher, 1, BigDecimal.ZERO, BusinessDay.toUtc(endVn)));
    }
}
