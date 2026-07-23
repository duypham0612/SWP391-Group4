package com.cafe.service.manager;

import com.cafe.common.BusinessDay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Regression: các lệnh clockIn/clockOut phải lấy ngày từ nguồn giờ Việt Nam dùng chung. */
class AttendanceServiceClockDateTest {

    @Test
    void clock_commands_use_vietnam_business_date() {
        assertEquals(BusinessDay.todayVn(), AttendanceService.currentVnDate());
    }
}
