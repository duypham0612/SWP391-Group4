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
        assertEquals("late", item.getSlaTier());
    }

    /** Chưa tới hạn thì nói còn bao lâu — một con số duy nhất để quyết định. */
    @Test
    void within_sla_shows_remaining_time() {
        OrderItem item = new OrderItem();
        item.setOrderCreatedAt(LocalDateTime.now());
        item.setWaitedSeconds(5 * 60);
        assertEquals("Còn 7 phút", item.getSlaLabel());
        assertEquals("Đã chờ 5/12 phút", item.getWaitProgressLabel());
    }

    /**
     * Chỉ ba bậc màu, và đỏ phải HIẾM thì mới còn tác dụng. Đơn qua đêm nay nằm ở khu
     * "Đơn treo" riêng nên không được đẩy card lên một bậc đỏ nặng hơn nữa.
     */
    @Test
    void overnight_order_does_not_get_its_own_red_tier() {
        OrderItem item = new OrderItem();
        item.setOrderCreatedAt(LocalDateTime.now().minusDays(1));
        item.setWaitedSeconds(27 * 60 * 60);
        assertEquals("Trễ từ hôm qua", item.getSlaLabel());
        assertEquals("late", item.getSlaTier());
    }

    /** Sắp tới hạn = hổ phách, chưa phải đỏ. */
    @Test
    void approaching_sla_is_amber_not_red() {
        OrderItem item = new OrderItem();
        item.setOrderCreatedAt(LocalDateTime.now());
        item.setWaitedSeconds(Constants.KDS_WARN_SECONDS);
        assertEquals("warn", item.getSlaTier());
    }

    /**
     * Phút làm tròn xuống nên phút đầu tiên hai bên vạch hạn đều ra "0 phút".
     * "Còn 0 phút" / "Trễ 0 phút" là chữ vô nghĩa — phải nói thành lời.
     */
    @Test
    void the_minute_around_the_deadline_never_reads_as_zero() {
        OrderItem justOver = new OrderItem();
        justOver.setOrderCreatedAt(LocalDateTime.now());
        justOver.setWaitedSeconds(Constants.KDS_SLA_SECONDS + 30);
        assertEquals("Vừa quá hạn", justOver.getSlaLabel());

        OrderItem exactly = new OrderItem();
        exactly.setOrderCreatedAt(LocalDateTime.now());
        exactly.setWaitedSeconds(Constants.KDS_SLA_SECONDS);
        assertEquals("Vừa quá hạn", exactly.getSlaLabel());

        OrderItem almost = new OrderItem();
        almost.setOrderCreatedAt(LocalDateTime.now());
        almost.setWaitedSeconds(Constants.KDS_SLA_SECONDS - 30);
        assertEquals("Sắp hết giờ", almost.getSlaLabel());
    }

    /** Dưới 2 tiếng dùng phút để nhẩm nhanh; quá đó đổi sang giờ vì "1770 phút" không ai đọc được. */
    @Test
    void long_durations_switch_from_minutes_to_hours() {
        assertEquals("45 phút", OrderItem.formatMinutesLabel(45 * 60));
        assertEquals("119 phút", OrderItem.formatMinutesLabel(119 * 60));
        assertEquals("2 tiếng", OrderItem.formatMinutesLabel(120 * 60));
        assertEquals("29 tiếng 30 phút", OrderItem.formatMinutesLabel(1770 * 60));
    }

    /**
     * Cùng thời gian chờ, hai món khác mốc pha chuẩn phải cho màu khác nhau:
     * Espresso (1') pha lâu là trễ, còn Đá xay (15') vẫn đang trong hạn.
     * Đây là lý do bỏ ngưỡng 12 phút cứng.
     */
    @Test
    void tier_follows_each_products_own_prep_time() {
        OrderItem fast = new OrderItem();
        fast.setOrderCreatedAt(LocalDateTime.now());
        fast.setPrepSeconds(60);          // món pha nhanh: chuẩn 1 phút
        fast.setWaitedSeconds(90);
        assertEquals("late", fast.getSlaTier());

        OrderItem slow = new OrderItem();
        slow.setOrderCreatedAt(LocalDateTime.now());
        slow.setPrepSeconds(15 * 60);     // món pha lâu: chuẩn 15 phút
        slow.setWaitedSeconds(90);
        assertEquals("ok", slow.getSlaTier());
        assertEquals("Đã chờ 1/15 phút", slow.getWaitProgressLabel());
    }

    /** Ngưỡng hổ phách = 2/3 chặng của mốc pha chuẩn từng món, không phải hằng số. */
    @Test
    void warn_threshold_scales_with_prep_time() {
        OrderItem item = new OrderItem();
        item.setOrderCreatedAt(LocalDateTime.now());
        item.setPrepSeconds(15 * 60);     // 2/3 của 15' = 10'
        item.setWaitedSeconds(10 * 60);
        assertEquals("warn", item.getSlaTier());
        item.setWaitedSeconds(10 * 60 - 1);
        assertEquals("ok", item.getSlaTier());
    }

    /** Món chưa khai mốc (0 hoặc bất thường) lùi về mặc định 12 phút, không vỡ. */
    @Test
    void missing_prep_time_falls_back_to_default() {
        OrderItem item = new OrderItem();
        item.setOrderCreatedAt(LocalDateTime.now());
        item.setPrepSeconds(0);
        item.setWaitedSeconds(Constants.KDS_SLA_SECONDS);
        assertEquals("late", item.getSlaTier());
        assertEquals("Đã chờ 12/12 phút", item.getWaitProgressLabel());
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
