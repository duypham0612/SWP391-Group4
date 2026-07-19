package com.cafe.service.barista;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test hiển thị KPI bàn giao/dashboard — logic thuần, không đụng DB. */
class HandoverKpiTest {

    @Test
    void avg_lead_display_is_dash_when_no_lead_time() {
        HandoverService.HandoverKpi kpi = new HandoverService.HandoverKpi(-1, 0);

        assertFalse(kpi.isHasLead());
        assertEquals("—", kpi.getAvgLeadDisplay());
    }

    @Test
    void avg_lead_display_formats_zero_seconds() {
        HandoverService.HandoverKpi kpi = new HandoverService.HandoverKpi(0, 3);

        assertTrue(kpi.isHasLead());
        assertEquals("0 giây", kpi.getAvgLeadDisplay());
    }

    @Test
    void avg_lead_display_formats_under_one_minute() {
        HandoverService.HandoverKpi kpi = new HandoverService.HandoverKpi(45, 5);

        assertEquals("45 giây", kpi.getAvgLeadDisplay());
    }

    @Test
    void avg_lead_display_formats_minutes_and_seconds() {
        HandoverService.HandoverKpi kpi = new HandoverService.HandoverKpi(125, 8);

        assertEquals("2 phút 5 giây", kpi.getAvgLeadDisplay());
    }
}
