package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public final class KdsOrderWorkflowService {
    private final OrderRepository repository;
    public KdsOrderWorkflowService() { this(new OrderRepository()); }
    KdsOrderWorkflowService(OrderRepository repository) { this.repository = Objects.requireNonNull(repository); }

    private static String sanitizeReason(String reason) {
        if (reason == null) return null;
        String value = reason.trim();
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    // EN: Locks the item to this barista (WAITING->MAKING), logs CLAIM, publishes status. -> OrderItemWorkflowDao.claim()
    public boolean startItem(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            int rows = repository.itemWorkflowDao.claim(conn, orderItemId, sessionBranchId, userId);
            if (rows == 0) return false;
        repository.activityLogDao.insertOrderItem(conn, orderItemId, sessionBranchId, "CLAIM", "WAITING", "MAKING", null, userId);
            repository.publishStatus(conn, it, "MAKING");
            return true;
        });
    }

    // EN: Looks up the item, then delegates the actual MAKING->READY completion. -> completeInTx() below
    public boolean markItemReady(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            return completeInTx(conn, it, userId, sessionBranchId);
        });
    }

    // EN: Locks status MAKING->READY, then deducts stock (unless already reserved by a remake). -> OrderItemWorkflowDao.completeClaimed() + InventoryService.deductForOrderItem()
    private boolean completeInTx(Connection conn, OrderItem it, int userId, int sessionBranchId) throws SQLException {
        int orderItemId = it.getOrderItemId();
        int rows = repository.itemWorkflowDao.completeClaimed(conn, orderItemId, sessionBranchId, userId);
        if (rows == 0) return false;
        int branchId = repository.branchOf(it);
        if (!it.isRemakeInventoryReserved()) {
            repository.inventoryService.deductForOrderItem(conn, branchId, orderItemId, it.getProductId(), it.getQuantity(), userId);
        }
        repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "COMPLETE", "MAKING", "READY", null, userId);
        repository.publishStatus(conn, it, "READY");
        repository.outboxEventDao.insert(conn, EventType.ITEM_READY, String.valueOf(orderItemId), branchId,
                "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId + ",\"by\":" + userId + "}");
        return true;
    }

    // EN: Loops all items of an order, claims every WAITING one, skips ones already taken. -> OrderItemWorkflowDao.claim() per item
    public int startAllInOrder(int orderId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return 0;
        return repository.tx(conn -> {
            int count = 0;
            for (OrderItem it : repository.itemDao.findByOrder(conn, orderId)) {
                if (!"WAITING".equals(it.getStatus())) continue;
                if (repository.itemWorkflowDao.claim(conn, it.getOrderItemId(), sessionBranchId, userId) == 0) continue;
            repository.activityLogDao.insertOrderItem(conn, it.getOrderItemId(), sessionBranchId, "CLAIM", "WAITING", "MAKING", null, userId);
                repository.publishStatus(conn, it, "MAKING");
                count++;
            }
            return count;
        });
    }

    public static class BulkReadyResult {
        private final int completed;
        private final int skippedNoRecipe;

        public BulkReadyResult(int completed, int skippedNoRecipe) {
            this.completed = completed;
            this.skippedNoRecipe = skippedNoRecipe;
        }

        public int getCompleted() { return completed; }
        public int getSkippedNoRecipe() { return skippedNoRecipe; }
    }

    // EN: Loops items owned by this barista in the order, completes each with a recipe, counts skips otherwise. -> completeInTx() above
    public BulkReadyResult markOrderReady(int orderId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return new BulkReadyResult(0, 0);
        return repository.tx(conn -> {
            List<OrderItem> items = repository.itemDao.findByOrder(conn, orderId);
            java.util.Set<Integer> productIds = new java.util.HashSet<>();
            for (OrderItem it : items) productIds.add(it.getProductId());
            java.util.Set<Integer> withRecipe = repository.productRecipeDao.findProductIdsWithRecipe(conn, productIds);

            int completed = 0;
            int skipped = 0;
            for (OrderItem it : items) {
                if (!"MAKING".equals(it.getStatus())) continue;
                if (!userId.equals(it.getBaristaId())) continue;
                if (!withRecipe.contains(it.getProductId())) { skipped++; continue; }
                if (completeInTx(conn, it, userId, sessionBranchId)) completed++;
            }
            return new BulkReadyResult(completed, skipped);
        });
    }

    public int countMyMakingItems(int branchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.itemWorkflowDao.countMakingByBarista(conn, branchId, userId);
        }
    }

    // EN: Re-checks the owner is actually off-shift, then moves item MAKING->WAITING for someone else. -> OrderItemWorkflowDao.reclaim()
    public boolean reclaimItem(int orderItemId, Integer actorUserId, int branchId, String actorName,
                               java.util.Set<Integer> onDutyUserIds) throws SQLException {
        if (actorUserId == null) return false;
        java.util.Set<Integer> onDuty = onDutyUserIds == null ? java.util.Set.of() : onDutyUserIds;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || it.getBaristaId() == null) return false;
            if (actorUserId.equals(it.getBaristaId())) return false;
            if (onDuty.contains(it.getBaristaId())) {
                throw new BusinessException("Người này vẫn đang trong ca — nhờ họ bấm “Trả lại chờ” cho món này.");
            }
            if (repository.itemWorkflowDao.reclaim(conn, orderItemId, branchId, it.getBaristaId()) == 0) return false;
            String reason = "Thu hồi từ " + (it.getBaristaName() == null ? "barista đã rời ca" : it.getBaristaName())
                    + (actorName == null || actorName.isBlank() ? "" : " bởi " + actorName);
            repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "RETURN_QUEUE", "MAKING", "WAITING",
                    sanitizeReason(reason), actorUserId);
            repository.publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    // EN: Owner-only: releases item MAKING->WAITING voluntarily. -> OrderItemWorkflowDao.returnToQueue()
    public boolean returnItemToQueue(int orderItemId, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || repository.itemWorkflowDao.returnToQueue(conn, orderItemId, branchId, userId) == 0) return false;
        repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "RETURN_QUEUE", "MAKING", "WAITING", null, userId);
            repository.publishStatus(conn, it, "WAITING");
            return true;
        });
    }

}
