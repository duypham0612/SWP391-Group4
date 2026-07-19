package com.cafe.model;

import com.cafe.common.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * B2 · Test SLA "chờ giao" của OrderItem — phân tier ok/warn/crit theo mốc PICKUP_* và hiển thị thời lượng.
 * Logic thuần, không đụng DB.
 */
class PickupSlaTest {

    private static OrderItem withServeWait(Integer seconds) {
        OrderItem it = new OrderItem();
        it.setServeWaitSeconds(seconds);
        return it;
    }

    @Test
    void tier_thresholds() {
        assertEquals("ok",   withServeWait(null).getServeTier());                        // chưa pha xong
        assertEquals("ok",   withServeWait(0).getServeTier());
        assertEquals("ok",   withServeWait(Constants.PICKUP_WARN_SECONDS - 1).getServeTier());
        assertEquals("warn", withServeWait(Constants.PICKUP_WARN_SECONDS).getServeTier());
        assertEquals("warn", withServeWait(Constants.PICKUP_CRIT_SECONDS - 1).getServeTier());
        assertEquals("crit", withServeWait(Constants.PICKUP_CRIT_SECONDS).getServeTier());
    }

    @Test
    void display_formats_minutes() {
        assertEquals("", withServeWait(null).getServeWaitDisplay());
        assertEquals("0 phút", withServeWait(30).getServeWaitDisplay());
        assertEquals("3 phút", withServeWait(180).getServeWaitDisplay());
        assertEquals("60 phút", withServeWait(3600).getServeWaitDisplay());
    }
}
