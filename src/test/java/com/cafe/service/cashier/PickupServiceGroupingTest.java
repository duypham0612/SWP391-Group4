package com.cafe.service.cashier;

import com.cafe.model.OrderItem;
import com.cafe.model.PickedUpGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickupServiceGroupingTest {

    @Test
    void groups_multiple_orders_for_the_same_table_and_enables_serve_all() {
        List<PickedUpGroup> groups = PickupService.groupPickedUpItems(List.of(
                item(10, 1, "Bàn 01", "D12", 1),
                item(11, 2, "Bàn 01", "D13", 2),
                item(12, 3, "Bàn 02", "D14", 1)));

        assertEquals(2, groups.size());
        PickedUpGroup tableOne = groups.get(0);
        assertEquals(List.of(10, 11), tableOne.getOrderIds());
        assertEquals(3, tableOne.getCupCount());
        assertTrue(tableOne.isCanServeAll());
    }

    @Test
    void keeps_takeaway_orders_separate_and_does_not_show_table_serve_all() {
        List<PickedUpGroup> groups = PickupService.groupPickedUpItems(List.of(
                item(20, 1, null, "T20", 1),
                item(21, 2, null, "T21", 1)));

        assertEquals(2, groups.size());
        assertFalse(groups.get(0).isCanServeAll());
        assertFalse(groups.get(1).isCanServeAll());
    }

    private static OrderItem item(int orderId, int itemId, String table, String code, int quantity) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setOrderItemId(itemId);
        item.setTableNumber(table);
        item.setPickupCode(code);
        item.setQuantity(quantity);
        item.setStatus("PICKED_UP");
        return item;
    }
}
