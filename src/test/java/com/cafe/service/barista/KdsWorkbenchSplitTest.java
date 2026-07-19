package com.cafe.service.barista;

import com.cafe.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phân giỏ board quầy pha chế theo trạng thái — logic thuần, không đụng DB.
 * Trọng tâm: món BLOCKED phải TÁCH khỏi hàng chờ (không ai nhận pha lại được)
 * nhưng vẫn nằm trên board (bỏ ra thì khách chờ mãi mà không ai thấy món).
 */
class KdsWorkbenchSplitTest {

    private static OrderItem item(int orderItemId, String status) {
        OrderItem it = new OrderItem();
        it.setOrderItemId(orderItemId);
        it.setOrderId(100);
        it.setStatus(status);
        it.setProductName("P" + orderItemId);
        return it;
    }

    private static List<Integer> ids(List<OrderItem> items) {
        return items.stream().map(OrderItem::getOrderItemId).toList();
    }

    @Test
    void blocked_item_leaves_waiting_but_stays_on_board() {
        Map<String, List<OrderItem>> board = KdsService.splitWorkbench(List.of(
                item(1, "WAITING"),
                item(2, "MAKING"),
                item(3, "READY"),
                item(4, "BLOCKED")));

        assertEquals(List.of(1), ids(board.get("waiting")), "món bị chặn không được nằm trong hàng chờ");
        assertEquals(List.of(2), ids(board.get("inProgress")));
        assertEquals(List.of(3), ids(board.get("ready")));
        assertEquals(List.of(4), ids(board.get("blocked")), "món bị chặn phải hiện ở khu Cần xử lý");
    }

    /** Bốn giỏ luôn tồn tại để JSP không phải kiểm null. */
    @Test
    void board_always_has_four_buckets_even_when_empty() {
        Map<String, List<OrderItem>> board = KdsService.splitWorkbench(List.of());
        assertEquals(List.of("waiting", "inProgress", "ready", "blocked"), List.copyOf(board.keySet()));
        board.values().forEach(list -> assertTrue(list.isEmpty()));
    }

    /** Trạng thái ngoài phạm vi quầy pha chế (đã giao, đã huỷ) không được lọt vào giỏ nào. */
    @Test
    void statuses_outside_workbench_are_ignored() {
        Map<String, List<OrderItem>> board = KdsService.splitWorkbench(List.of(
                item(1, "SERVED"),
                item(2, "CANCELLED"),
                item(3, "PICKED_UP"),
                item(4, "WAITING")));

        assertEquals(List.of(4), ids(board.get("waiting")));
        assertTrue(board.get("inProgress").isEmpty());
        assertTrue(board.get("ready").isEmpty());
        assertTrue(board.get("blocked").isEmpty());
    }
}
