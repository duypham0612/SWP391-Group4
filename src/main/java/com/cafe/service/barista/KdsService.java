package com.cafe.service.barista;
import com.cafe.service.shared.BranchMenuService;
import com.cafe.service.shared.OrderService;

import com.cafe.model.KdsTicket;
import com.cafe.model.OrderItem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** B1/B2 · KdsService — màn bếp (Barista). Uỷ thác OrderService; auto-deduct nằm ở markReady. */
public class KdsService {

    private final OrderService orderService = new OrderService();
    private final BranchMenuService branchMenuService = new BranchMenuService();

    public List<OrderItem> getQueue(int branchId) throws SQLException { return orderService.getKdsQueue(branchId); }

    public List<KdsTicket> getQueueTickets(int branchId) throws SQLException {
        Map<Integer, KdsTicket> byOrder = new LinkedHashMap<>();
        for (OrderItem item : getQueue(branchId)) {
            KdsTicket ticket = byOrder.get(item.getOrderId());
            if (ticket == null) {
                byOrder.put(item.getOrderId(), new KdsTicket(item));
            } else {
                ticket.addItem(item);
            }
        }
        return new ArrayList<>(byOrder.values());
    }

    /**
     * Board 2 cột: gom món WAITING và MAKING theo đơn thành 2 danh sách ticket riêng.
     * Mỗi cột giữ thứ tự FIFO (getQueue đã sắp theo giờ vào đơn). Đơn có cả 2 loại món sẽ
     * xuất hiện ở cả hai cột (mỗi cột chỉ chứa món đúng trạng thái của nó).
     * Trả Map với khoá "waiting" và "making".
     */
    public Map<String, List<KdsTicket>> getQueueBoard(int branchId) throws SQLException {
        Map<Integer, KdsTicket> waiting = new LinkedHashMap<>();
        Map<Integer, KdsTicket> making = new LinkedHashMap<>();
        for (OrderItem item : getQueue(branchId)) {
            Map<Integer, KdsTicket> target = "MAKING".equals(item.getStatus()) ? making : waiting;
            KdsTicket ticket = target.get(item.getOrderId());
            if (ticket == null) target.put(item.getOrderId(), new KdsTicket(item));
            else ticket.addItem(item);
        }
        Map<String, List<KdsTicket>> board = new LinkedHashMap<>();
        board.put("waiting", new ArrayList<>(waiting.values()));
        board.put("making", new ArrayList<>(making.values()));
        return board;
    }

    public List<OrderItem> getReadyItems(int branchId) throws SQLException { return orderService.getReadyItems(branchId); }

    public void startItem(int orderItemId, Integer userId, int branchId) throws SQLException { orderService.startItem(orderItemId, userId, branchId); }
    public void bump(int orderItemId, int branchId) throws SQLException { orderService.bumpItem(orderItemId, branchId); }
    public void markReady(int orderItemId, Integer userId, int branchId) throws SQLException { orderService.markItemReady(orderItemId, userId, branchId); }
    public void markServed(int orderItemId, Integer userId, int branchId) throws SQLException { orderService.markItemServed(orderItemId, userId, branchId); }

    /** "Xong cả đơn" — trừ tồn + READY mọi món WAITING/MAKING của đơn. Trả số món đã xong. */
    public int markOrderReady(int orderId, Integer userId, int branchId) throws SQLException { return orderService.markOrderReady(orderId, userId, branchId); }

    /** "Không pha được" — huỷ một món còn WAITING/MAKING (không đụng ledger). Trả true nếu huỷ được. */
    public boolean cancelItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        return orderService.cancelItem(orderItemId, reason, userId, branchId);
    }

    /** Đánh dấu hết món (86) — khoá sản phẩm khỏi POS + QR ở chi nhánh; ghi audit ai bật 86. */
    public void set86(int branchId, int productId, boolean is86, java.time.LocalDateTime backInEta, Integer userId) throws SQLException {
        branchMenuService.set86(branchId, productId, is86, backInEta, userId);
    }
}
