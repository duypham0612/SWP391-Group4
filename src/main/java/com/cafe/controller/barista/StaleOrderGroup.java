package com.cafe.controller.barista;

import com.cafe.common.BusinessDay;
import com.cafe.model.OrderItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đơn treo từ ngày kinh doanh trước, đã gộp về MỘT dòng cho mỗi đơn.
 *
 * <p>Barista đang đứng pha không thao tác được trên đơn treo (phải huỷ hoặc hoàn tiền — việc của
 * quản lý), nên render mỗi ly thành một card đầy đủ vừa tốn chỗ vừa che mất hàng chờ thật. Gộp
 * theo đơn: 30 ly của 8 đơn còn 8 dòng, đủ để barista trả lời khách "đơn #793 của anh/chị đang
 * chờ quản lý xử lý" mà không mất tầm nhìn board.
 */
public class StaleOrderGroup {

    private final int orderId;
    private final String location;
    private final String createdDisplay;
    private final int cups;
    private final int lines;
    private final String productSummary;
    /** Mốc thật để sắp xếp — KHÔNG sort theo createdDisplay vì "dd/MM" đảo thứ tự khi qua tháng. */
    private final LocalDateTime createdAtUtc;

    private StaleOrderGroup(int orderId, String location, String createdDisplay,
                            int cups, int lines, String productSummary, LocalDateTime createdAtUtc) {
        this.orderId = orderId;
        this.location = location;
        this.createdDisplay = createdDisplay;
        this.cups = cups;
        this.lines = lines;
        this.productSummary = productSummary;
        this.createdAtUtc = createdAtUtc;
    }

    public int getOrderId() { return orderId; }
    public String getLocation() { return location; }
    public String getCreatedDisplay() { return createdDisplay; }
    public int getCups() { return cups; }
    public int getLines() { return lines; }
    public String getProductSummary() { return productSummary; }

    /**
     * Gộp danh sách món treo theo đơn, đơn cũ nhất lên trước.
     * Thứ tự trong mỗi đơn giữ nguyên thứ tự DAO trả về (đã sắp theo thời gian tạo).
     */
    public static List<StaleOrderGroup> from(List<OrderItem> items) {
        Map<Integer, List<OrderItem>> byOrder = new LinkedHashMap<>();
        for (OrderItem item : items) {
            byOrder.computeIfAbsent(item.getOrderId(), key -> new ArrayList<>()).add(item);
        }

        List<StaleOrderGroup> groups = new ArrayList<>();
        for (Map.Entry<Integer, List<OrderItem>> entry : byOrder.entrySet()) {
            List<OrderItem> lines = entry.getValue();
            OrderItem head = lines.get(0);

            int cups = 0;
            StringBuilder summary = new StringBuilder();
            for (OrderItem line : lines) {
                cups += line.getQuantity();
                if (summary.length() > 0) summary.append(" · ");
                summary.append(line.getQuantity()).append("× ").append(line.getProductName());
            }

            String location = head.getTableNumber() == null || head.getTableNumber().isBlank()
                    ? head.getOrderTypeLabel()
                    : head.getTableNumber();

            groups.add(new StaleOrderGroup(entry.getKey(), location, stamp(head.getOrderCreatedAt()),
                    cups, lines.size(), summary.toString(), head.getOrderCreatedAt()));
        }

        // Đơn cũ nhất lên đầu; đơn thiếu mốc thời gian đẩy xuống cuối thay vì ném NPE.
        groups.sort(Comparator.comparing(g -> g.createdAtUtc, Comparator.nullsLast(Comparator.naturalOrder())));
        return groups;
    }

    /** orderCreatedAt lưu UTC — quy về giờ VN trước khi hiển thị cho nhân viên quầy. */
    private static String stamp(LocalDateTime createdAtUtc) {
        return BusinessDay.fmtStampVn(createdAtUtc);
    }
}
