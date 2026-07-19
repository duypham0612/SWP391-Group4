package com.cafe.service.barista;

import com.cafe.model.OrderItem;
import com.cafe.model.PickupTicket;
import com.cafe.service.shared.OrderService;

import java.sql.SQLException;
import java.util.List;

/**
 * B2 · PickupService — bảng món sẵn lấy.
 * Uỷ thác OrderService: gom món READY theo đơn/bàn (1 connection) + giao/hoàn tác giao.
 */
public class PickupService {

    private final OrderService orderService = new OrderService();

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

    /** Hoàn tác giao nhầm: SERVED → READY (không đụng ledger). Trả true nếu hoàn tác được. */
    public boolean unserveItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        return orderService.unserveItem(orderItemId, userId, branchId);
    }
}
