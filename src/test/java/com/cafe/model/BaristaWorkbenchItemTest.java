package com.cafe.model;

import com.cafe.common.Constants;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaristaWorkbenchItemTest {
    /** Nhãn trễ phải nêu con số cụ thể và dùng chữ barista nói được — không dùng thuật ngữ "SLA". */
    @Test
    void late_label_is_explicit_and_free_of_jargon() {
        OrderItem item = new OrderItem();
        item.setOrderCreatedAt(LocalDateTime.now());
        item.setWaitedSeconds(Constants.KDS_SLA_SECONDS + 3 * 60);
        assertEquals("Trễ 3 phút", item.getSlaLabel());
        assertEquals("crit", item.getSlaTier());
    }

    /** Đơn qua đêm không được hiện thành chuỗi hàng chục tiếng — nói thẳng là trễ từ hôm qua. */
    @Test
    void overnight_order_is_marked_for_manual_handling() {
        OrderItem item = new OrderItem();
        item.setOrderCreatedAt(LocalDateTime.now().minusDays(1));
        item.setWaitedSeconds(27 * 60 * 60);
        assertEquals("Trễ từ hôm qua", item.getSlaLabel());
        assertEquals("severe", item.getSlaTier());
    }

    /** Dưới 2 tiếng dùng phút để nhẩm nhanh; quá đó đổi sang giờ vì "1770 phút" không ai đọc được. */
    @Test
    void long_durations_switch_from_minutes_to_hours() {
        assertEquals("45 phút", OrderItem.formatMinutesLabel(45 * 60));
        assertEquals("119 phút", OrderItem.formatMinutesLabel(119 * 60));
        assertEquals("2 tiếng", OrderItem.formatMinutesLabel(120 * 60));
        assertEquals("29 tiếng 30 phút", OrderItem.formatMinutesLabel(1770 * 60));
    }

    @Test
    void quantity_order_type_station_and_remake_are_unambiguous() {
        OrderItem item = new OrderItem();
        item.setQuantity(3);
        item.setOrderType("TAKEAWAY");
        item.setCategoryName("Trà trái cây");
        item.setProductName("Trà đào");
        item.setRemakeCount(1);
        assertEquals(3, item.getCupCount());
        assertEquals("Mang đi", item.getOrderTypeLabel());
        assertEquals("TEA", item.getStation());
        assertTrue(item.isPriority());
    }
}
