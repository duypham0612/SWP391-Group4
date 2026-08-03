package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.dao.cashier.*;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.sql.SQLException;
import java.util.*;

/** Bàn giao READY → PICKED_UP → SERVED và hoàn tác giao nhầm. */
public final class OrderHandoffService {
    private final OrderRepository repository;
    public OrderHandoffService() { this(new OrderRepository()); }
    OrderHandoffService(OrderRepository repository) { this.repository = Objects.requireNonNull(repository); }

    public boolean markItemPickedUp(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || repository.itemDao.pickUp(conn, orderItemId, sessionBranchId, userId) == 0) return false;
            int branchId = repository.branchOf(it);
        repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "PICK_UP", "READY", "PICKED_UP", null, userId);
            repository.publishStatus(conn, it, "PICKED_UP");
            repository.outboxEventDao.insert(conn, EventType.ITEM_PICKED_UP, String.valueOf(orderItemId), branchId,
                    "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId + ",\"by\":" + userId + "}");
            return true;
        });
    }

    /** PICKED_UP → SERVED (đóng dấu ServedAt). Chỉ role Thu ngân/Phục vụ gọi qua route riêng. */
    public boolean markItemServed(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            int rows = repository.itemDao.updateStatusIf(conn, orderItemId, "SERVED",
                    new String[]{"PICKED_UP"}, sessionBranchId, false, false, true, false);
            if (rows == 0) return false;   // không còn READY / khác chi nhánh
            int branchId = repository.branchOf(it);
        repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "SERVE", "PICKED_UP", "SERVED", null, userId);
            repository.publishStatus(conn, it, "SERVED");
            repository.completeOrderIfDone(conn, it.getOrderId(), branchId);   // giao món cuối → đơn COMPLETED
            return true;
        });
    }

    /** Nhận tất cả món READY của một đơn, không xác nhận đã giao khách. */
    public int pickUpAllReady(int orderId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return 0;
        return repository.tx(conn -> {
            int count = 0;
            for (OrderItem it : repository.itemDao.findByOrder(conn, orderId)) {
                if (!"READY".equals(it.getStatus())) continue;
                if (repository.itemDao.pickUp(conn, it.getOrderItemId(), sessionBranchId, userId) == 0) continue;
                int branchId = repository.branchOf(it);
            repository.activityLogDao.insertOrderItem(conn, it.getOrderItemId(), branchId, "PICK_UP", "READY", "PICKED_UP", null, userId);
                repository.publishStatus(conn, it, "PICKED_UP");
                count++;
            }
            return count;
        });
    }

    /**
     * Giao tất cả món PICKED_UP của một hay nhiều đơn thuộc cùng nhóm bàn, trong một transaction.
     * Danh sách orderId từ client chỉ là gợi ý; updateStatusIf vẫn khóa đúng chi nhánh và trạng thái.
     */
    public int serveAllPickedUp(List<Integer> orderIds, String tableNumber,
                                Integer userId, int sessionBranchId)
            throws SQLException {
        if (userId == null || orderIds == null || orderIds.isEmpty()
                || tableNumber == null || tableNumber.isBlank()) return 0;
        String expectedTable = tableNumber.trim();
        List<Integer> safeOrderIds = orderIds.stream()
                .filter(java.util.Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .limit(50)
                .toList();
        if (safeOrderIds.isEmpty()) return 0;
        return repository.tx(conn -> {
            int done = 0;
            java.util.Set<Integer> affectedOrders = new java.util.LinkedHashSet<>();
            for (OrderItem it : repository.itemDao.findByOrders(conn, safeOrderIds)) {
                if (!"PICKED_UP".equals(it.getStatus())) continue;
                if (it.getTableNumber() == null
                        || !expectedTable.equalsIgnoreCase(it.getTableNumber().trim())) continue;
                int rows = repository.itemDao.updateStatusIf(conn, it.getOrderItemId(), "SERVED",
                        new String[]{"PICKED_UP"}, sessionBranchId, false, false, true, false);
                if (rows == 0) continue;   // đổi bởi người khác / khác chi nhánh
                int branchId = repository.branchOf(it);
            repository.activityLogDao.insertOrderItem(conn, it.getOrderItemId(), branchId, "SERVE", "PICKED_UP", "SERVED", null, userId);
                repository.publishStatus(conn, it, "SERVED");
                affectedOrders.add(it.getOrderId());
                done++;
            }
            for (Integer orderId : affectedOrders) {
                repository.completeOrderIfDone(conn, orderId, sessionBranchId);
            }
            return done;
        });
    }

    /**
     * Hoàn tác giao nhầm — SERVED → PICKED_UP (xoá ServedAt), vẫn giữ bước đã nhận khỏi quầy.
     * KHÔNG đụng ledger (kho đã trừ ở bước READY, giữ nguyên). Nếu đơn đã COMPLETED thì mở lại ACTIVE.
     * Trả true nếu hoàn tác được.
     */
    public boolean unserveItem(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            int rows = repository.itemDao.updateStatusIf(conn, orderItemId, "PICKED_UP",
                    new String[]{"SERVED"}, sessionBranchId, false, false, false, true);   // clearServed
            if (rows == 0) return false;   // không còn SERVED / khác chi nhánh
            int branchId = repository.branchOf(it);
            if (repository.orderDao.reopenIfCompleted(conn, it.getOrderId()) == 1) {
                repository.outboxEventDao.insert(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(it.getOrderId()), branchId,
                        "{\"orderId\":" + it.getOrderId() + ",\"status\":\"ACTIVE\"}");
            }
        repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "UNDO_SERVE", "SERVED", "PICKED_UP", null, userId);
            repository.publishStatus(conn, it, "PICKED_UP");
            return true;
        });
    }


}
