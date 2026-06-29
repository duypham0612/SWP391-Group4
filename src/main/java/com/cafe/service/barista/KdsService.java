package com.cafe.service.barista;
import com.cafe.service.shared.OrderService;

import com.cafe.model.OrderItem;

import java.sql.SQLException;
import java.util.List;

/** B1/B2 · KdsService — màn bếp (Barista). Uỷ thác OrderService; auto-deduct nằm ở markReady. */
public class KdsService {

    private final OrderService orderService = new OrderService();

    public List<OrderItem> getQueue(int branchId) throws SQLException { return orderService.getKdsQueue(branchId); }
    public List<OrderItem> getReadyItems(int branchId) throws SQLException { return orderService.getReadyItems(branchId); }

    public void startItem(int orderItemId, Integer userId) throws SQLException { orderService.startItem(orderItemId, userId); }
    public void bump(int orderItemId) throws SQLException { orderService.bumpItem(orderItemId); }
    public void markReady(int orderItemId, Integer userId) throws SQLException { orderService.markItemReady(orderItemId, userId); }
    public void markServed(int orderItemId, Integer userId) throws SQLException { orderService.markItemServed(orderItemId, userId); }
}
