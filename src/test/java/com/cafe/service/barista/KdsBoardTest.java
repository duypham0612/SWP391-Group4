package com.cafe.service.barista;

import com.cafe.model.KdsTicket;
import com.cafe.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B1 · Test gom board KDS 2 cột (KdsService.getQueueBoard) — logic thuần, override getQueue để không đụng DB.
 * Kiểm: orderPendingCount = tổng WAITING+MAKING của cả đơn.
 */
class KdsBoardTest {

    private static OrderItem item(int orderItemId, int orderId, String status) {
        OrderItem it = new OrderItem();
        it.setOrderItemId(orderItemId);
        it.setOrderId(orderId);
        it.setStatus(status);
        it.setProductName("P" + orderItemId);
        return it;
    }

    /** KdsService với hàng chờ giả (không mở DB). */
    private static KdsService boardOf(List<OrderItem> queue) {
        return new KdsService() {
            @Override
            public List<OrderItem> getQueue(int branchId) { return queue; }
        };
    }

    private static KdsTicket find(List<KdsTicket> tickets, int orderId) {
        return tickets.stream().filter(t -> t.getOrderId() == orderId).findFirst().orElseThrow();
    }

    /** Đơn có món ở CẢ hai cột → mỗi ticket cột chỉ giữ món của nó nhưng orderPendingCount = tổng cả đơn. */
    @Test
    void order_pending_count_spans_both_columns() throws Exception {
        KdsService svc = boardOf(List.of(
                item(1, 10, "WAITING"),
                item(2, 10, "MAKING"),
                item(3, 20, "WAITING")));
        Map<String, List<KdsTicket>> board = svc.getQueueBoard(1);

        KdsTicket w10 = find(board.get("waiting"), 10);
        KdsTicket m10 = find(board.get("making"), 10);
        KdsTicket w20 = find(board.get("waiting"), 20);

        assertEquals(1, w10.getItemCount());               // cột chờ pha chỉ có 1 món của đơn 10
        assertEquals(2, w10.getOrderPendingCount());        // nhưng cả đơn còn 2 món chưa xong
        assertTrue(w10.isSpansBothColumns());
        assertEquals(2, m10.getOrderPendingCount());
        assertTrue(m10.isSpansBothColumns());

        assertEquals(1, w20.getOrderPendingCount());        // đơn 20 chỉ 1 cột
        assertFalse(w20.isSpansBothColumns());
    }

    /** Đơn nằm gọn 1 cột → orderPendingCount = số món cột đó, không span. */
    @Test
    void single_column_order_pending_equals_item_count() throws Exception {
        KdsService svc = boardOf(List.of(
                item(1, 30, "WAITING"),
                item(2, 30, "WAITING")));
        Map<String, List<KdsTicket>> board = svc.getQueueBoard(1);

        assertTrue(board.get("making").isEmpty());
        KdsTicket w = board.get("waiting").get(0);
        assertEquals(2, w.getItemCount());
        assertEquals(2, w.getOrderPendingCount());
        assertFalse(w.isSpansBothColumns());
    }
}
