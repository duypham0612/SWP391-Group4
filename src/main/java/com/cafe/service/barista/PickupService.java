package com.cafe.service.barista;

import com.cafe.model.Order;
import com.cafe.model.OrderItem;
import com.cafe.model.PickupTicket;
import com.cafe.service.shared.OrderService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B2 · PickupService — bảng món sẵn lấy.
 * Gom món READY theo đơn/bàn, nạp toàn bộ đơn để barista đối chiếu đủ/đúng trước khi giao.
 */
public class PickupService {

    private final OrderService orderService = new OrderService();

    /** Các ticket sẵn lấy của chi nhánh (gom theo đơn, giữ thứ tự xuất hiện của món READY). */
    public List<PickupTicket> getReadyTickets(int branchId) throws SQLException {
        List<OrderItem> ready = orderService.getReadyItems(branchId);
        Map<Integer, List<OrderItem>> byOrder = new LinkedHashMap<>();
        for (OrderItem it : ready) {
            byOrder.computeIfAbsent(it.getOrderId(), k -> new ArrayList<>()).add(it);
        }
        List<PickupTicket> tickets = new ArrayList<>();
        for (Map.Entry<Integer, List<OrderItem>> e : byOrder.entrySet()) {
            Order full = orderService.getOrder(e.getKey());   // toàn bộ đơn: mọi món + trạng thái + modifier
            tickets.add(new PickupTicket(e.getKey(), e.getValue(), full));
        }
        return tickets;
    }

    public void serveItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        orderService.markItemServed(orderItemId, userId, branchId);
    }

    /** Giao tất cả món READY của một đơn (mỗi món một lần markServed). */
    public void serveAllReady(int orderId, Integer userId, int branchId) throws SQLException {
        Order o = orderService.getOrder(orderId);
        if (o == null || o.getItems() == null) return;
        for (OrderItem it : o.getItems()) {
            if ("READY".equals(it.getStatus())) orderService.markItemServed(it.getOrderItemId(), userId, branchId);
        }
    }
}
