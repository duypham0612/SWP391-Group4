package com.cafe.service.barista;

import com.cafe.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lọc + cắt trang hàng chờ quầy pha chế — logic thuần, không đụng DB.
 * Trọng tâm: phân trang phải đếm trên tập ĐÃ lọc, số trang ngoài phạm vi phải kéo về biên,
 * và món BỊ CHẶN không được bộ lọc giấu đi.
 */
class KdsQueuePageTest {

    private static OrderItem item(int orderItemId, String status) {
        OrderItem it = new OrderItem();
        it.setOrderItemId(orderItemId);
        it.setOrderId(100 + orderItemId);
        it.setStatus(status);
        it.setProductName("Cà phê sữa " + orderItemId);   // getStation() → COFFEE
        it.setOrderType("DINE_IN");
        return it;
    }

    private static List<OrderItem> waiting(int count) {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) items.add(item(i, "WAITING"));
        return items;
    }

    private static List<Integer> ids(List<OrderItem> items) {
        return items.stream().map(OrderItem::getOrderItemId).toList();
    }

    /** Dòng thuộc MỘT đơn cụ thể — dựng khối để kiểm phân trang không cắt ngang đơn. */
    private static OrderItem lineOf(int orderItemId, int orderId) {
        OrderItem it = item(orderItemId, "WAITING");
        it.setOrderId(orderId);
        return it;
    }

    @Test
    void page_slices_the_queue_and_reports_its_range() {
        KdsService.QueuePage page = KdsService.paginate(waiting(25), 2, 12);

        assertEquals(List.of(13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24), ids(page.getItems()));
        assertEquals(25, page.getTotal());
        assertEquals(3, page.getTotalPages());
        assertEquals(13, page.getStartRow());
        assertEquals(24, page.getEndRow());
        assertTrue(page.isHasPrevious());
        assertTrue(page.isHasNext());
    }

    /** Trang cuối chỉ còn phần dư — không được vượt quá cuối danh sách. */
    @Test
    void last_page_holds_only_the_remainder() {
        KdsService.QueuePage page = KdsService.paginate(waiting(25), 3, 12);

        assertEquals(List.of(25), ids(page.getItems()));
        assertEquals(25, page.getStartRow());
        assertEquals(25, page.getEndRow());
        assertFalse(page.isHasNext());
    }

    /** Hàng chờ ngắn đi sau một thao tác → trang đang xem có thể không còn; phải kéo về trang cuối. */
    @Test
    void page_beyond_the_end_falls_back_to_the_last_page() {
        KdsService.QueuePage page = KdsService.paginate(waiting(5), 9, 12);

        assertEquals(1, page.getPage());
        assertEquals(5, page.getItems().size());
    }

    @Test
    void empty_queue_still_reports_one_page() {
        KdsService.QueuePage page = KdsService.paginate(List.of(), 1, 12);

        assertTrue(page.getItems().isEmpty());
        assertEquals(1, page.getTotalPages());
        assertEquals(0, page.getStartRow());
        assertEquals(0, page.getEndRow());
        assertFalse(page.isHasPrevious());
        assertFalse(page.isHasNext());
    }

    /** "Món của tôi" = tôi đang pha, hoặc chính tôi vừa pha xong. */
    @Test
    void mine_filter_keeps_only_items_held_by_the_current_barista() {
        OrderItem mineMaking = item(1, "MAKING");
        mineMaking.setBaristaId(7);
        OrderItem otherMaking = item(2, "MAKING");
        otherMaking.setBaristaId(8);
        OrderItem mineReady = item(3, "READY");
        mineReady.setPreparedBy(7);
        OrderItem stillWaiting = item(4, "WAITING");

        List<OrderItem> out = KdsService.filterWorkbench(
                List.of(mineMaking, otherMaking, mineReady, stillWaiting), "mine", "all", "all", 7);

        assertEquals(List.of(1, 3), ids(out));
    }

    @Test
    void unassigned_filter_keeps_only_items_nobody_has_started() {
        OrderItem making = item(2, "MAKING");
        making.setBaristaId(7);

        List<OrderItem> out = KdsService.filterWorkbench(
                List.of(item(1, "WAITING"), making, item(3, "READY")), "unassigned", "all", "all", 7);

        assertEquals(List.of(1), ids(out));
    }

    /** Món bị chặn là cảnh báo an toàn — mọi bộ lọc đều phải để lọt qua. */
    @Test
    void blocked_items_survive_every_filter() {
        OrderItem blocked = item(9, "BLOCKED");
        blocked.setOrderType("TAKEAWAY");

        List<OrderItem> out = KdsService.filterWorkbench(
                List.of(item(1, "WAITING"), blocked), "mine", "TEA", "DELIVERY", 7);

        assertEquals(List.of(9), ids(out));
    }

    @Test
    void station_and_order_type_filters_narrow_the_queue() {
        OrderItem tea = item(1, "WAITING");
        tea.setProductName("Trà đào");                 // getStation() → TEA
        OrderItem takeaway = item(2, "WAITING");
        takeaway.setOrderType("TAKEAWAY");

        assertEquals(List.of(1), ids(KdsService.filterWorkbench(
                List.of(tea, takeaway), "all", "TEA", "all", 7)));
        assertEquals(List.of(2), ids(KdsService.filterWorkbench(
                List.of(tea, takeaway), "all", "all", "TAKEAWAY", 7)));
    }

    /** Giá trị lạ (client sửa tay) coi như không lọc, không được ném lỗi hay trả rỗng. */
    @Test
    void unknown_filter_values_do_not_narrow_the_queue() {
        List<OrderItem> items = waiting(3);

        assertEquals(3, KdsService.filterWorkbench(items, null, null, null, 7).size());
        assertEquals(3, KdsService.filterWorkbench(items, "all", "all", "all", null).size());
    }

    /**
     * Đơn nhiều món KHÔNG được cắt ngang hai trang: pha hết trang 1 mà đơn còn ly ở trang 2 là
     * cách chắc chắn nhất để giao thiếu. Trang nhận trọn khối chừng nào chưa đạt cỡ trang.
     */
    @Test
    void a_multi_line_order_is_never_split_across_pages() {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) items.add(lineOf(i, 200 + i));   // 10 đơn một dòng
        for (int i = 11; i <= 14; i++) items.add(lineOf(i, 999));      // một đơn 4 dòng

        KdsService.QueuePage page = KdsService.paginate(items, 1, 12);

        assertEquals(14, page.getItems().size());   // kéo trọn khối 4 dòng thay vì cắt ở dòng 12
        assertEquals(1, page.getTotalPages());
        assertEquals(1, page.getStartRow());
        assertEquals(14, page.getEndRow());
        assertFalse(page.isHasNext());
    }

    /** Trang đã đủ cỡ thì khối kế tiếp mở trang mới — và phạm vi "đang xem" phải theo vị trí thật. */
    @Test
    void the_next_block_starts_a_new_page_and_rows_keep_counting() {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) items.add(lineOf(i, 200 + i));
        for (int i = 11; i <= 14; i++) items.add(lineOf(i, 999));      // trang 1 dôi thành 14 dòng
        for (int i = 15; i <= 17; i++) items.add(lineOf(i, 300 + i));

        KdsService.QueuePage second = KdsService.paginate(items, 2, 12);

        assertEquals(List.of(15, 16, 17), ids(second.getItems()));
        assertEquals(2, second.getTotalPages());
        assertEquals(15, second.getStartRow());
        assertEquals(17, second.getEndRow());
        assertEquals(17, second.getTotal());
        assertTrue(second.isHasPrevious());
        assertFalse(second.isHasNext());
    }

    /** Đơn lớn hơn cả trang vẫn nằm trọn một trang — nếu không thì không trang nào chứa nổi nó. */
    @Test
    void a_block_larger_than_one_page_still_stays_whole() {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 15; i++) items.add(lineOf(i, 999));
        items.add(lineOf(16, 777));

        KdsService.QueuePage first = KdsService.paginate(items, 1, 12);
        KdsService.QueuePage second = KdsService.paginate(items, 2, 12);

        assertEquals(15, first.getItems().size());
        assertEquals(2, first.getTotalPages());
        assertEquals(List.of(16), ids(second.getItems()));
        assertEquals(16, second.getStartRow());
        assertEquals(16, second.getEndRow());
    }

    /** Cắt trang phải chạy TRÊN tập đã lọc, nếu không số trang sẽ đếm cả món đang bị ẩn. */
    @Test
    void pagination_counts_the_filtered_queue_only() {
        List<OrderItem> items = new ArrayList<>(waiting(20));
        OrderItem tea = item(21, "WAITING");
        tea.setProductName("Trà sen vàng");
        items.add(tea);

        KdsService.QueuePage page = KdsService.paginate(
                KdsService.filterWorkbench(items, "all", "TEA", "all", 7), 1, 12);

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getTotalPages());
        assertEquals(List.of(21), ids(page.getItems()));
    }
}
