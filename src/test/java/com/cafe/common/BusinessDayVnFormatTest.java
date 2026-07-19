package com.cafe.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BusinessDayVnFormatTest {

    @Test
    void formats_utc_as_vietnam_time() {
        assertEquals("14:00", BusinessDay.fmtTimeVn(LocalDateTime.parse("2026-07-19T07:00:00")));
        assertEquals("01:30 20/07", BusinessDay.fmtDateTimeVn(LocalDateTime.parse("2026-07-19T18:30:00")));
        assertEquals("01/01 01:00", BusinessDay.fmtStampVn(LocalDateTime.parse("2026-12-31T18:00:00")));
    }

    @Test
    void keeps_existing_null_display_contracts() {
        assertEquals("", BusinessDay.fmtTimeVn(null));
        assertEquals("-", BusinessDay.fmtDateTimeVn(null));
        assertEquals("—", BusinessDay.fmtStampVn(null));
        assertNull(BusinessDay.toVn(null));
    }
}
