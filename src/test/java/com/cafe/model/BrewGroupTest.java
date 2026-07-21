package com.cafe.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrewGroupTest {

    private static OrderItem item(int itemId, int orderId, Integer tableSessionId,
                                  String orderType, String status, int quantity, LocalDateTime createdAt) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(itemId);
        item.setOrderId(orderId);
        item.setTableSessionId(tableSessionId);
        item.setOrderType(orderType);
        item.setStatus(status);
        item.setQuantity(quantity);
        item.setOrderCreatedAt(createdAt);
        return item;
    }

    @Test
    void dine_in_orders_in_one_table_session_are_one_group_and_cups_are_summed() {
        LocalDateTime t0 = LocalDateTime.of(2026, 7, 20, 8, 0);
        List<BrewGroup> groups = BrewGroup.from(List.of(
                item(1, 10, 55, "DINE_IN", "WAITING", 2, t0),
                item(2, 11, 55, "DINE_IN", "MAKING", 1, t0.plusMinutes(2))));

        assertEquals(1, groups.size());
        assertEquals("T55", groups.get(0).getKey());
        assertEquals(3, groups.get(0).getCups());
        assertEquals(2, groups.get(0).getWaitingCups());
        assertEquals(1, groups.get(0).getMakingCups());
    }

    @Test
    void takeaway_orders_are_grouped_by_order_and_groups_keep_fifo_order() {
        LocalDateTime t0 = LocalDateTime.of(2026, 7, 20, 8, 0);
        List<BrewGroup> groups = BrewGroup.from(List.of(
                item(1, 20, null, "TAKEAWAY", "WAITING", 1, t0),
                item(2, 30, null, "TAKEAWAY", "READY", 1, t0.plusMinutes(1)),
                item(3, 20, null, "TAKEAWAY", "MAKING", 1, t0.plusMinutes(2))));

        assertEquals(List.of("O20", "O30"), groups.stream().map(BrewGroup::getKey).toList());
        assertEquals(List.of(1, 3), groups.get(0).getItems().stream().map(OrderItem::getOrderItemId).toList());
    }
}
