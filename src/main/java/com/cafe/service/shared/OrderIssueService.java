package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public final class OrderIssueService {
    private final OrderRepository repository;
    public OrderIssueService() { this(new OrderRepository()); }
    OrderIssueService(OrderRepository repository) { this.repository = Objects.requireNonNull(repository); }

    public static class UnblockResult {
        private final boolean success;
        private final int remainingBlockedWithRecountedIngredients;
        public UnblockResult(boolean success, int remaining) {
            this.success = success;
            this.remainingBlockedWithRecountedIngredients = remaining;
        }
        public boolean isSuccess() { return success; }
        public int getRemainingBlockedWithRecountedIngredients() { return remainingBlockedWithRecountedIngredients; }
    }

    // EN: Flags a warning for cashier/manager without touching the item's status. -> OrderItemIssueDao.reportIssue()
    public boolean reportItemIssue(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn lý do sự cố.");
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || repository.itemIssueDao.reportIssue(conn, orderItemId, branchId, userId, clean) == 0) return false;
            repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "ISSUE", it.getStatus(), it.getStatus(), clean, userId);
            repository.outboxEventDao.insert(conn, EventType.ITEM_ISSUE_REPORTED, String.valueOf(orderItemId), branchId,
                    "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId
                            + ",\"reason\":\"" + clean + "\",\"by\":" + userId + "}");
            return true;
        });
    }

    // EN: Validates the reason, then delegates the actual block. -> blockInTx() below
    public boolean blockItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn lý do không pha được.");
        return repository.tx(conn -> blockInTx(conn, orderItemId, clean, userId, branchId));
    }

    // EN: Validates ingredients belong to this recipe, blocks the item, then zeroes their stock. -> blockInTx() + InventoryService.applyBaseAdjustmentInTx()
    public boolean blockItemForDepletedIngredients(int orderItemId, List<Integer> ingredientIds,
                                                   String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        if (ingredientIds == null || ingredientIds.isEmpty())
            throw new IllegalArgumentException("Vui lòng chọn nguyên liệu đã hết.");
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) clean = "Hết nguyên liệu";
        final String finalReason = clean;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            java.util.Set<Integer> recipeIngredientIds = new java.util.HashSet<>();
            for (Recipe line : repository.productRecipeDao.findByProduct(conn, it.getProductId())) {
                recipeIngredientIds.add(line.getIngredientId());
            }
            for (Integer ingredientId : ingredientIds) {
                if (ingredientId != null && !recipeIngredientIds.contains(ingredientId)) {
                    throw new BusinessException("Nguyên liệu báo hết không thuộc công thức của món này.");
                }
            }
            if (!blockInTx(conn, orderItemId, finalReason, userId, branchId)) return false;
            for (Integer ingredientId : ingredientIds) {
                if (ingredientId == null) continue;
                repository.inventoryService.applyBaseAdjustmentInTx(conn, branchId, ingredientId, BigDecimal.ZERO,
                        "Barista báo hết tại quầy pha chế", userId);
            }
            return true;
        });
    }

    // EN: Shared logic for both block use cases: sets BLOCKED, logs, publishes status. -> OrderItemIssueDao.blockItem()
    private boolean blockInTx(Connection conn, int orderItemId, String reason, int userId, int branchId)
            throws SQLException {
        OrderItem it = repository.itemDao.findById(conn, orderItemId);
        if (it == null) return false;
        String from = it.getStatus();
        if (repository.itemIssueDao.blockItem(conn, orderItemId, branchId, userId, reason) == 0) return false;
            repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "BLOCK", from, "BLOCKED", reason, userId);
        repository.publishStatus(conn, it, "BLOCKED");
        repository.outboxEventDao.insert(conn, EventType.ITEM_ISSUE_REPORTED, String.valueOf(orderItemId), branchId,
                "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId
                        + ",\"reason\":\"" + reason + "\",\"by\":" + userId + "}");
        return true;
    }

    // EN: Simple BLOCKED->WAITING, no stock write. -> OrderItemIssueDao.unblockItem()
    public boolean unblockItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || repository.itemIssueDao.unblockItem(conn, orderItemId, branchId) == 0) return false;
            repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "UNBLOCK", "BLOCKED", "WAITING", null, userId);
            repository.publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    // EN: BLOCKED->WAITING, then writes real recounted stock for each ingredient given. -> OrderItemIssueDao.unblockItem() + InventoryService.applyBaseAdjustmentInTx()
    public OrderIssueService.UnblockResult unblockItem(int orderItemId, List<StockAdjustment> recounts,
                                     Integer userId, int branchId) throws SQLException {
        if (userId == null) return new OrderIssueService.UnblockResult(false, 0);
        List<StockAdjustment> cleanRecounts = recounts == null ? List.of() : recounts;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || repository.itemIssueDao.unblockItem(conn, orderItemId, branchId) == 0) {
                return new OrderIssueService.UnblockResult(false, 0);
            }

            Map<Integer, String> unitByIngredient = new HashMap<>();
            for (Recipe line : repository.productRecipeDao.findByProduct(conn, it.getProductId())) {
                unitByIngredient.put(line.getIngredientId(), line.getIngredientUnit());
            }

            java.util.Set<Integer> recountedIds = new java.util.LinkedHashSet<>();
            for (StockAdjustment recount : cleanRecounts) {
                if (recount == null || recount.getActualBaseQty() == null) continue;
                if (!unitByIngredient.containsKey(recount.getIngredientId())) {
                    throw new BusinessException("Nguyên liệu kiểm kê không thuộc công thức của món này.");
                }
                String unit = unitByIngredient.get(recount.getIngredientId());
                repository.inventoryService.applyBaseAdjustmentInTx(conn, branchId, recount.getIngredientId(),
                        recount.getActualBaseQty(), "Barista kiểm lại khi bỏ chặn tại quầy pha chế", userId);
                recountedIds.add(recount.getIngredientId());
            }

            repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "UNBLOCK", "BLOCKED", "WAITING", null, userId);
            repository.publishStatus(conn, it, "WAITING");
            int remaining = repository.itemIssueDao.countBlockedUsingIngredients(conn, branchId, recountedIds);
            return new OrderIssueService.UnblockResult(true, remaining);
        });
    }

    // EN: Locks the item for redo (owner-only if MAKING), reserves waste, sends it back to WAITING with priority. -> OrderItemIssueDao.beginRemake/beginRemakeClaimed/finishRemake()
    public boolean remakeItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn lý do làm lại.");
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            boolean fromReady = "READY".equals(it.getStatus());
            boolean accepted = fromReady
                    ? repository.itemIssueDao.beginRemake(conn, orderItemId, branchId) == 1
                    : repository.itemIssueDao.beginRemakeClaimed(conn, orderItemId, branchId, userId) == 1;
            if (!accepted) return false;
            repository.inventoryService.reserveRemakeForOrderItem(conn, branchId, orderItemId, it.getProductId(), it.getQuantity(), clean, userId);
            repository.itemIssueDao.finishRemake(conn, orderItemId, branchId,
                    com.cafe.common.RemakeReservation.reservesNextPour(fromReady, it.isRemakeInventoryReserved()));
            repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "REMAKE", it.getStatus(), "WAITING", clean, userId);
            repository.outboxEventDao.insert(conn, EventType.ITEM_REMAKE_REQUESTED, String.valueOf(orderItemId), branchId,
                    "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId
                            + ",\"reason\":\"" + clean + "\",\"by\":" + userId + "}");
            repository.publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    public String cancelItem(int orderItemId, String reason, Integer userId, int sessionBranchId) throws SQLException {
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return "NOT_FOUND";
            String s = it.getStatus();
            if (!"WAITING".equals(s) && !"MAKING".equals(s) && !"BLOCKED".equals(s)) return "CONFLICT";
        if (repository.billLineDao.existsForOrderItem(conn, orderItemId)) return "ALREADY_BILLED";
            int rows = repository.itemDao.updateStatusIf(conn, orderItemId, "CANCELLED",
                    new String[]{"WAITING", "MAKING", "BLOCKED"}, sessionBranchId, false, false, false, false);
            if (rows == 0) return "CONFLICT";
            int branchId = repository.branchOf(it);
            if (it.isRemakeInventoryReserved()) {
                repository.inventoryService.releaseRemakeReservation(conn, branchId, orderItemId, userId);
            }
            String r = sanitizeReason(reason);
            repository.activityLogDao.insertOrderItem(conn, orderItemId, branchId, "CANCEL", s, "CANCELLED", r.isEmpty() ? null : r, userId);
            repository.outboxEventDao.insert(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(it.getOrderId()), branchId,
                    "{\"orderItemId\":" + orderItemId + ",\"status\":\"CANCELLED\""
                    + (r.isEmpty() ? "" : ",\"reason\":\"" + r + "\"")
                    + (userId == null ? "" : ",\"by\":" + userId) + "}");
            repository.completeOrderIfDone(conn, it.getOrderId(), branchId);
            return "OK";
        });
    }

    private static String sanitizeReason(String reason) {
        if (reason == null) return "";
        String r = reason.replaceAll("[\\\\\"\\p{Cntrl}]", " ").trim();
        return r.length() > 120 ? r.substring(0, 120) : r;
    }

    public boolean voidOrder(int orderId, Integer userId, int branchId) throws SQLException {
        return repository.tx(conn -> {
            Order o = repository.orderDao.findById(conn, orderId);
            if (o == null || o.getBranchId() != branchId || !"ACTIVE".equals(o.getStatus())) return false;
            List<OrderItem> items = repository.itemDao.findByOrder(conn, orderId);
            for (OrderItem it : items) {
                String s = it.getStatus();
                if ("MAKING".equals(s) || "READY".equals(s) || "PICKED_UP".equals(s) || "SERVED".equals(s)) return false;
            }
            for (OrderItem it : items) {
                if ("WAITING".equals(it.getStatus()) || "BLOCKED".equals(it.getStatus())) {
                    if (it.isRemakeInventoryReserved()) {
                        repository.inventoryService.releaseRemakeReservation(conn, o.getBranchId(), it.getOrderItemId(), userId);
                    }
                    repository.itemDao.updateStatus(conn, it.getOrderItemId(), "CANCELLED", false, false);
                    repository.publishStatus(conn, it, "CANCELLED");
                }
            }
            repository.orderDao.updateStatus(conn, orderId, "CANCELLED");
            repository.outboxEventDao.insert(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(orderId), o.getBranchId(),
                    "{\"orderId\":" + orderId + ",\"status\":\"CANCELLED\"}");
            return true;
        });
    }


}
