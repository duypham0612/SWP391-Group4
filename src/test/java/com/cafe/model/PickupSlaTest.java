package com.cafe.model;

import com.cafe.common.Constants;
import com.cafe.web.viewmodel.ViewFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * B2 · Test SLA "chờ giao" của OrderItem — phân tier ok/warn/crit theo mốc PICKUP_* và hiển thị thời lượng.
 * Logic thuần, không đụng DB.
 */
class PickupSlaTest {
    private final ViewFormatter view = new ViewFormatter();

    private static OrderItem withServeWait(Integer seconds) {
        OrderItem it = new OrderItem();
        it.setServeWaitSeconds(seconds);
        return it;
    }

    @Test
    void tier_thresholds() {
        assertEquals("ok",   view.serveTier(null));
        assertEquals("ok",   view.serveTier(0));
        assertEquals("ok",   view.serveTier(Constants.PICKUP_WARN_SECONDS - 1));
        assertEquals("warn", view.serveTier(Constants.PICKUP_WARN_SECONDS));
        assertEquals("warn", view.serveTier(Constants.PICKUP_CRIT_SECONDS - 1));
        assertEquals("crit", view.serveTier(Constants.PICKUP_CRIT_SECONDS));
    }

    @Test
    void display_formats_minutes() {
        assertEquals("", view.durationMinutes(null));
        assertEquals("0 phút", view.durationMinutes(30));
        assertEquals("3 phút", view.durationMinutes(180));
        assertEquals("60 phút", view.durationMinutes(3600));
    }
}
