package com.cafe.service.cashier;

import com.cafe.model.OrderItem;
import com.cafe.model.PickupTicket;
import com.cafe.model.PickedUpGroup;
import com.cafe.service.shared.OrderService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B2 · PickupService — bảng món sẵn lấy.
 * Uỷ thác OrderService: gom món READY theo đơn/bàn (1 connection) + giao/hoàn tác giao.
 */
public class PickupService {

    private final OrderService orderService;

    public PickupService() { this(new OrderService()); }
    public PickupService(OrderService orderService) {
        this.orderService = java.util.Objects.requireNonNull(orderService);
    }

    /** Món cho phép hoàn tác giao nhầm hiển thị trong ~10 phút gần nhất. */
    private static final int UNDO_WINDOW_MINUTES = 10;

    /** Các ticket sẵn lấy của chi nhánh (gom theo đơn, kèm toàn bộ món để đối chiếu đủ/đúng). */
    public List<PickupTicket> getReadyTickets(int branchId) throws SQLException {
        return orderService.getPickupTickets(branchId);
    }

    /** Món vừa giao gần đây (để hoàn tác nếu bấm nhầm). */
    public List<OrderItem> getRecentlyServed(int branchId) throws SQLException {
        return orderService.getRecentlyServed(branchId, UNDO_WINDOW_MINUTES);
    }

    public List<OrderItem> getPickedUpItems(int branchId) throws SQLException {
        return orderService.getPickedUpItems(branchId);
    }

    /** Gom theo bàn; đơn mang đi không có bàn thì chỉ gom trong chính đơn đó. */
    public List<PickedUpGroup> getPickedUpGroups(int branchId) throws SQLException {
        return groupPickedUpItems(getPickedUpItems(branchId));
    }

    static List<PickedUpGroup> groupPickedUpItems(List<OrderItem> items) {
        Map<String, PickedUpGroup> groups = new LinkedHashMap<>();
        if (items == null) return new ArrayList<>();
        for (OrderItem item : items) {
            String table = item.getTableNumber();
            String key = table == null || table.isBlank()
                    ? "ORDER:" + item.getOrderId()
                    : "TABLE:" + table.trim();
            groups.computeIfAbsent(key, ignored -> new PickedUpGroup(table)).add(item);
        }
        return new ArrayList<>(groups.values());
    }

    public boolean pickUpItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.markItemPickedUp(orderItemId, userId, branchId);
    }

    /** Giao tất cả món READY của một đơn trong MỘT transaction. Trả số món đã giao. */
    public int pickUpAllReady(int orderId, Integer userId, int branchId) throws SQLException {
        return orderService.pickUpAllReady(orderId, userId, branchId);
    }

    public boolean serveItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.markItemServed(orderItemId, userId, branchId);
    }

    public int serveAllPickedUp(List<Integer> orderIds, String tableNumber,
                                Integer userId, int branchId) throws SQLException {
        return orderService.serveAllPickedUp(orderIds, tableNumber, userId, branchId);
    }

    /** Hoàn tác giao nhầm: SERVED → READY (không đụng ledger). Trả true nếu hoàn tác được. */
    public boolean unserveItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.unserveItem(orderItemId, userId, branchId);
    }
}
