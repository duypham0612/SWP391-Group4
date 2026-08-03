package com.cafe.service.cashier;

import com.cafe.model.OrderItem;
import com.cafe.model.PickupTicket;
import com.cafe.model.PickedUpGroup;
import com.cafe.service.shared.OrderHandoffService;
import com.cafe.service.shared.OrderQueryService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B2 · PickupService — bảng món sẵn lấy.
 * Uỷ thác tầng đơn: đọc món READY theo đơn/bàn ({@link OrderQueryService}) + giao/hoàn tác giao
 * ({@link OrderHandoffService}).
 */
public class PickupService {

    private final OrderQueryService orderQuery;
    private final OrderHandoffService orderHandoff;

    public PickupService() { this(new OrderQueryService(), new OrderHandoffService()); }
    public PickupService(OrderQueryService orderQuery, OrderHandoffService orderHandoff) {
        this.orderQuery = java.util.Objects.requireNonNull(orderQuery);
        this.orderHandoff = java.util.Objects.requireNonNull(orderHandoff);
    }

    /** Món cho phép hoàn tác giao nhầm hiển thị trong ~10 phút gần nhất. */
    private static final int UNDO_WINDOW_MINUTES = 10;

    /** Các ticket sẵn lấy của chi nhánh (gom theo đơn, kèm toàn bộ món để đối chiếu đủ/đúng). */
    public List<PickupTicket> getReadyTickets(int branchId) throws SQLException {
        return orderQuery.getPickupTickets(branchId);
    }

    /** Món vừa giao gần đây (để hoàn tác nếu bấm nhầm). */
    public List<OrderItem> getRecentlyServed(int branchId) throws SQLException {
        return orderQuery.getRecentlyServed(branchId, UNDO_WINDOW_MINUTES);
    }

    public List<OrderItem> getPickedUpItems(int branchId) throws SQLException {
        return orderQuery.getPickedUpItems(branchId);
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
        return orderHandoff.markItemPickedUp(orderItemId, userId, branchId);
    }

    /** Giao tất cả món READY của một đơn trong MỘT transaction. Trả số món đã giao. */
    public int pickUpAllReady(int orderId, Integer userId, int branchId) throws SQLException {
        return orderHandoff.pickUpAllReady(orderId, userId, branchId);
    }

    public boolean serveItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderHandoff.markItemServed(orderItemId, userId, branchId);
    }

    public int serveAllPickedUp(List<Integer> orderIds, String tableNumber,
                                Integer userId, int branchId) throws SQLException {
        return orderHandoff.serveAllPickedUp(orderIds, tableNumber, userId, branchId);
    }

    /** Hoàn tác giao nhầm: SERVED → READY (không đụng ledger). Trả true nếu hoàn tác được. */
    public boolean unserveItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderHandoff.unserveItem(orderItemId, userId, branchId);
    }
}
