package com.cafe.service.barista;

import com.cafe.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gom nhóm theo đơn trên hàng chờ quầy pha chế — logic thuần, không đụng DB.
 * Trọng tâm: số đếm phải nói về CẢ ĐƠN (không phải phần còn sót sau bộ lọc), và tiêu đề nhóm
 * chỉ được dựng khi khối thật sự có từ 2 dòng liền nhau ở đúng danh sách đang hiện.
 */
class KdsOrderGroupTest {

    private static OrderItem item(int orderItemId, int orderId, String status) {
        OrderItem it = new OrderItem();
        it.setOrderItemId(orderItemId);
        it.setOrderId(orderId);
        it.setStatus(status);
        it.setProductName("Cà phê sữa " + orderItemId);
        it.setOrderType("DINE_IN");
        it.setTableNumber("Bàn 02");
        return it;
    }

    private static OrderItem making(int orderItemId, int orderId, Integer baristaId) {
        OrderItem it = item(orderItemId, orderId, "MAKING");
        it.setBaristaId(baristaId);
        return it;
    }

    @Test
    void lines_of_one_order_share_the_same_group_info_and_are_numbered_in_order() {
        List<OrderItem> queue = new ArrayList<>(List.of(
                item(1, 500, "WAITING"), item(2, 500, "WAITING"), item(3, 500, "READY")));

        KdsService.annotateOrderLines(queue, 7);

        assertSame(queue.get(0).getGroupInfo(), queue.get(2).getGroupInfo());
        assertEquals(List.of(1, 2, 3),
                queue.stream().map(OrderItem::getOrderLineNo).toList());
        assertEquals(3, queue.get(0).getGroupInfo().getLineCount());
        assertEquals(1, queue.get(0).getGroupInfo().getDoneCount());
        assertEquals(2, queue.get(0).getGroupInfo().getWaitingCount());
        assertEquals(2, queue.get(0).getGroupInfo().getPendingCount());
        assertTrue(queue.get(0).isGrouped());
    }

    /** "Xong cả đơn" chỉ được đếm món của CHÍNH người đang đăng nhập — món người khác pha không tính. */
    @Test
    void mine_making_count_ignores_items_held_by_other_baristas() {
        List<OrderItem> queue = new ArrayList<>(List.of(
                making(1, 500, 7), making(2, 500, 8), making(3, 500, 7)));

        KdsService.annotateOrderLines(queue, 7);

        assertEquals(2, queue.get(0).getGroupInfo().getMineMakingCount());
    }

    /** Chưa đăng nhập (currentUserId null) thì không món nào là "của tôi" — không được NPE. */
    @Test
    void unknown_current_user_yields_no_mine_items() {
        List<OrderItem> queue = new ArrayList<>(List.of(making(1, 500, 7), making(2, 500, 8)));

        KdsService.annotateOrderLines(queue, null);

        assertEquals(0, queue.get(0).getGroupInfo().getMineMakingCount());
    }

    /** Đơn một món không phải là "nhóm": bàn và mã gọi món đã nằm sẵn trên chính dòng đó. */
    @Test
    void single_line_order_is_not_grouped() {
        List<OrderItem> queue = new ArrayList<>(List.of(item(1, 500, "WAITING")));

        KdsService.annotateOrderLines(queue, 7);

        assertFalse(queue.get(0).isGrouped());
        assertFalse(queue.get(0).getGroupInfo().isGrouped());
    }

    @Test
    void empty_queue_is_left_untouched() {
        List<OrderItem> queue = new ArrayList<>();

        KdsService.annotateOrderLines(queue, 7);
        KdsService.markGroupStarts(queue);

        assertTrue(queue.isEmpty());
    }

    /** Đích đến: đơn tại bàn hiện số bàn, đơn mang đi hiện loại đơn. */
    @Test
    void destination_falls_back_to_order_type_when_there_is_no_table() {
        OrderItem takeaway = item(1, 500, "WAITING");
        takeaway.setTableNumber(null);
        takeaway.setOrderType("TAKEAWAY");
        List<OrderItem> queue = new ArrayList<>(List.of(takeaway, item(2, 501, "WAITING")));

        KdsService.annotateOrderLines(queue, 7);

        assertEquals("Mang đi", queue.get(0).getGroupInfo().getDestinationLabel());
        assertEquals("Bàn 02", queue.get(1).getGroupInfo().getDestinationLabel());
    }

    @Test
    void group_start_marks_the_first_row_of_each_block() {
        List<OrderItem> queue = new ArrayList<>(List.of(
                item(1, 500, "WAITING"), item(2, 500, "WAITING"),
                item(3, 501, "WAITING"), item(4, 501, "WAITING"), item(5, 501, "WAITING")));
        KdsService.annotateOrderLines(queue, 7);

        KdsService.markGroupStarts(queue);

        assertEquals(List.of(true, false, true, false, false),
                queue.stream().map(OrderItem::isGroupStart).toList());
        assertTrue(queue.stream().allMatch(OrderItem::isGroupMember));
    }

    /**
     * Ly đã pha xong bị dồn xuống đáy nằm tách khỏi phần còn lại của đơn — dòng lẻ đó KHÔNG được
     * đội tiêu đề riêng, vì đọc lên nó giống hệt một đơn mới.
     */
    @Test
    void a_lone_row_of_a_multi_line_order_gets_no_header() {
        OrderItem ready = item(3, 500, "READY");
        List<OrderItem> queue = new ArrayList<>(List.of(
                item(1, 500, "WAITING"), item(2, 500, "WAITING"), item(4, 501, "WAITING"), ready));
        KdsService.annotateOrderLines(queue, 7);

        KdsService.markGroupStarts(queue);

        assertTrue(queue.get(0).isGroupStart());
        assertFalse(queue.get(2).isGroupStart());   // đơn 501 chỉ có một dòng
        assertFalse(ready.isGroupStart());          // dòng lẻ của đơn 500
        assertFalse(ready.isGroupMember());
        // Nhãn "3/3" vẫn đúng: dòng lẻ vẫn biết mình là ly thứ mấy của đơn.
        assertEquals(3, ready.getOrderLineNo());
        assertEquals(3, ready.getGroupInfo().getLineCount());
    }

    /** Bộ lọc cắt bớt dòng nhưng nhãn phải vẫn nói về CẢ đơn, không phải phần còn sót. */
    @Test
    void counts_stay_on_the_whole_order_after_filtering() {
        List<OrderItem> queue = new ArrayList<>(List.of(
                making(1, 500, 7), item(2, 500, "WAITING"), item(3, 500, "WAITING")));
        KdsService.annotateOrderLines(queue, 7);

        List<OrderItem> visible = KdsService.filterWorkbench(queue, "mine", "all", "all", 7);
        KdsService.markGroupStarts(visible);

        assertEquals(1, visible.size());
        assertEquals(3, visible.get(0).getGroupInfo().getLineCount());   // vẫn là 3 món của đơn
        assertFalse(visible.get(0).isGroupStart());                      // còn một dòng → không tiêu đề
    }

    /** Dòng chưa qua annotate (khu dùng chung template) phải trả null gọn, không ném lỗi. */
    @Test
    void row_without_annotation_reports_no_group() {
        OrderItem raw = item(1, 500, "WAITING");

        assertNull(raw.getGroupInfo());
        assertFalse(raw.isGrouped());
    }
}
