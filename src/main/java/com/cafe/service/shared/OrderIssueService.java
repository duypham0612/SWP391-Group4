package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.*;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Báo/chặn/gỡ sự cố, remake, huỷ món và huỷ order. */
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

    public boolean reportItemIssue(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn lý do sự cố.");
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || repository.itemDao.reportIssue(conn, orderItemId, branchId, userId, clean) == 0) return false;
            repository.actionDao.insert(conn, orderItemId, branchId, "ISSUE", it.getStatus(), it.getStatus(), clean, userId);
            repository.outboxEventDao.insert(conn, EventType.ITEM_ISSUE_REPORTED, String.valueOf(orderItemId), branchId,
                    "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId
                            + ",\"reason\":\"" + clean + "\",\"by\":" + userId + "}");
            return true;
        });
    }

    /**
     * Nhóm B — món không pha được (hỏng máy, ngừng bán): WAITING/MAKING → BLOCKED.
     * Khác {@link #reportItemIssue} ở chỗ món RỜI hàng chờ thay vì chỉ gắn cờ, nên
     * barista khác không thể bấm "Nhận pha" rồi lại phát hiện y hệt vấn đề đó.
     */
    public boolean blockItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn lý do không pha được.");
        return repository.tx(conn -> blockInTx(conn, orderItemId, clean, userId, branchId));
    }

    /**
     * Nhóm A — hết nguyên liệu: kiểm kê nguyên liệu về 0 QUA SỔ CÁI rồi chặn món, trong CÙNG một transaction.
     * Sửa nguyên nhân gốc (sổ kho đang lạc quan hơn thực tế vì tồn chỉ bị trừ lúc markReady) thay vì
     * chỉ ghi cờ. Tồn về 0 → {@code findProductsWithDepletedIngredient} tự suy ra MỌI món dùng nguyên
     * liệu đó và gợi ý ở màn "Báo hết món" — việc khoá menu vẫn là thao tác có ý thức của người dùng,
     * KHÔNG tự động, vì một nguyên liệu nằm trong nhiều món và đó là quyết định doanh thu.
     */
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
            // Chỉ nhận nguyên liệu thuộc công thức của MÓN NÀY — đối xứng với luồng kiểm kê lúc bỏ chặn.
            // Thiếu chốt này, một POST tự soạn ép được tồn của nguyên liệu bất kỳ ở chi nhánh về 0,
            // kéo theo mọi món dùng nguyên liệu đó biến mất khỏi POS/QR.
            java.util.Set<Integer> recipeIngredientIds = new java.util.HashSet<>();
            for (ProductRecipe line : repository.productRecipeDao.findByProduct(conn, it.getProductId())) {
                recipeIngredientIds.add(line.getIngredientId());
            }
            for (Integer ingredientId : ingredientIds) {
                if (ingredientId != null && !recipeIngredientIds.contains(ingredientId)) {
                    throw new BusinessException("Nguyên liệu báo hết không thuộc công thức của món này.");
                }
            }
            // Chặn món trước: thua race (món vừa bị người khác xử lý) thì KHÔNG đụng tới sổ kho.
            if (!blockInTx(conn, orderItemId, finalReason, userId, branchId)) return false;
            for (Integer ingredientId : ingredientIds) {
                if (ingredientId == null) continue;
                repository.inventoryService.applyBaseAdjustmentInTx(conn, branchId, ingredientId, BigDecimal.ZERO,
                        "Barista báo hết tại quầy pha chế", userId);
            }
            return true;
        });
    }

    /** Dùng chung cho nhóm A và B — gọi TRONG tx của caller. */
    private boolean blockInTx(Connection conn, int orderItemId, String reason, int userId, int branchId)
            throws SQLException {
        OrderItem it = repository.itemDao.findById(conn, orderItemId);
        if (it == null) return false;
        String from = it.getStatus();
        if (repository.itemDao.blockItem(conn, orderItemId, branchId, userId, reason) == 0) return false;
        repository.actionDao.insert(conn, orderItemId, branchId, "BLOCK", from, "BLOCKED", reason, userId);
        repository.publishStatus(conn, it, "BLOCKED");
        repository.outboxEventDao.insert(conn, EventType.ITEM_ISSUE_REPORTED, String.valueOf(orderItemId), branchId,
                "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId
                        + ",\"reason\":\"" + reason + "\",\"by\":" + userId + "}");
        return true;
    }

    /** BLOCKED → WAITING khi nguyên liệu/máy đã có lại. Đường thoát bắt buộc, nếu không món kẹt vĩnh viễn. */
    public boolean unblockItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || repository.itemDao.unblockItem(conn, orderItemId, branchId) == 0) return false;
            repository.actionDao.insert(conn, orderItemId, branchId, "UNBLOCK", "BLOCKED", "WAITING", null, userId);
            repository.publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    /**
     * BLOCKED → WAITING kèm kiểm kê tồn thật cho nguyên liệu vừa có lại.
     * Chốt trạng thái trước; nếu món đã bị thao tác khác xử lý thì không ghi sổ kho.
     */
    public OrderIssueService.UnblockResult unblockItem(int orderItemId, List<StockAdjustment> recounts,
                                     Integer userId, int branchId) throws SQLException {
        if (userId == null) return new OrderIssueService.UnblockResult(false, 0);
        List<StockAdjustment> cleanRecounts = recounts == null ? List.of() : recounts;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || repository.itemDao.unblockItem(conn, orderItemId, branchId) == 0) {
                return new OrderIssueService.UnblockResult(false, 0);
            }

            Map<Integer, String> unitByIngredient = new HashMap<>();
            for (ProductRecipe line : repository.productRecipeDao.findByProduct(conn, it.getProductId())) {
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

            repository.actionDao.insert(conn, orderItemId, branchId, "UNBLOCK", "BLOCKED", "WAITING", null, userId);
            repository.publishStatus(conn, it, "WAITING");
            int remaining = repository.itemDao.countBlockedUsingIngredients(conn, branchId, recountedIds);
            return new OrderIssueService.UnblockResult(true, remaining);
        });
    }

    /** Nguyên liệu trong công thức của một món — dựng danh sách chọn ở modal "Hết nguyên liệu". */
    public boolean remakeItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn lý do làm lại.");
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            boolean fromReady = "READY".equals(it.getStatus());
            boolean accepted = fromReady
                    ? repository.itemDao.beginRemake(conn, orderItemId, branchId) == 1
                    : repository.itemDao.beginRemakeClaimed(conn, orderItemId, branchId, userId) == 1;
            if (!accepted) return false;
            repository.inventoryService.reserveRemakeForOrderItem(conn, branchId, orderItemId, it.getProductId(), it.getQuantity(), clean, userId);
            // Bỏ từ MAKING khi chưa hề trừ kho → dòng WASTE vừa ghi là sổ của chính lượt vừa bỏ,
            // nên lượt pha lại vẫn phải trừ. Giữ chỗ vô điều kiện như trước làm trừ THIẾU một lượt.
            repository.itemDao.finishRemake(conn, orderItemId, branchId,
                    com.cafe.common.RemakeReservation.reservesNextPour(fromReady, it.isRemakeInventoryReserved()));
            repository.actionDao.insert(conn, orderItemId, branchId, "REMAKE", it.getStatus(), "WAITING", clean, userId);
            repository.outboxEventDao.insert(conn, EventType.ITEM_REMAKE_REQUESTED, String.valueOf(orderItemId), branchId,
                    "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId
                            + ",\"reason\":\"" + clean + "\",\"by\":" + userId + "}");
            repository.publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    /**
     * API huỷ dòng món cho luồng quản lý/thu ngân; Quầy pha chế không expose thao tác này.
     * Chặn nếu món đã lên hoá đơn (để Cashier xử lý). Scope chi nhánh.
     * Trả mã: OK · NOT_FOUND · ALREADY_BILLED (đã lên bill) · CONFLICT (đã/đang xong hoặc khác chi nhánh).
     */
    public String cancelItem(int orderItemId, String reason, Integer userId, int sessionBranchId) throws SQLException {
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return "NOT_FOUND";
            String s = it.getStatus();
            // BLOCKED nằm trong danh sách huỷ được: món bị chặn (hết nguyên liệu/hỏng máy) thường
            // kết thúc bằng việc Thu ngân huỷ và hoàn tiền — thiếu nó thì món kẹt không lối thoát.
            if (!"WAITING".equals(s) && !"MAKING".equals(s) && !"BLOCKED".equals(s)) return "CONFLICT";
            if (repository.billItemDao.existsForOrderItem(conn, orderItemId)) return "ALREADY_BILLED";   // đã lên bill → Cashier xử lý
            int rows = repository.itemDao.updateStatusIf(conn, orderItemId, "CANCELLED",
                    new String[]{"WAITING", "MAKING", "BLOCKED"}, sessionBranchId, false, false, false, false);
            if (rows == 0) return "CONFLICT";   // đã bị đổi / khác chi nhánh
            int branchId = repository.branchOf(it);
            // Món đang giữ chỗ nguyên liệu cho lượt pha lại: huỷ ở đây nghĩa là lượt đó không bao giờ
            // xảy ra, phải hoàn phần đã trừ trước — bỏ qua thì kho ghi thừa đúng một lượt pha.
            if (it.isRemakeInventoryReserved()) {
                repository.inventoryService.releaseRemakeReservation(conn, branchId, orderItemId, userId);
            }
            String r = sanitizeReason(reason);
            // Ghi audit như mọi chuyển trạng thái khác (trước đây cancelItem là ngoại lệ thiếu log).
            repository.actionDao.insert(conn, orderItemId, branchId, "CANCEL", s, "CANCELLED", r.isEmpty() ? null : r, userId);
            repository.outboxEventDao.insert(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(it.getOrderId()), branchId,
                    "{\"orderItemId\":" + orderItemId + ",\"status\":\"CANCELLED\""
                    + (r.isEmpty() ? "" : ",\"reason\":\"" + r + "\"")
                    + (userId == null ? "" : ",\"by\":" + userId) + "}");
            // Huỷ món cuối còn dang dở có thể khiến cả đơn kết thúc (mọi món SERVED/CANCELLED).
            repository.completeOrderIfDone(conn, it.getOrderId(), branchId);
            return "OK";
        });
    }

    /** Làm sạch lý do huỷ để nhúng an toàn vào JSON payload (bỏ ký tự điều khiển/nháy, giới hạn độ dài). */
    private static String sanitizeReason(String reason) {
        if (reason == null) return "";
        String r = reason.replaceAll("[\\\\\"\\p{Cntrl}]", " ").trim();
        return r.length() > 120 ? r.substring(0, 120) : r;
    }

    /** READY → PICKED_UP: Thu ngân/Phục vụ xác nhận đã nhận món khỏi quầy. */
    /**
     * C4 · Void đơn sai — CHỈ huỷ được khi đơn còn ở giai đoạn chưa pha (mọi món WAITING).
     * Nếu barista đã nhận pha (MAKING/READY/PICKED_UP/SERVED) → trả false, KHÔNG đổi gì.
     * Khi huỷ: chuyển các món WAITING → CANCELLED + đơn → CANCELLED. KHÔNG đụng ledger.
     * Trả true nếu huỷ thành công.
     */
    public boolean voidOrder(int orderId, Integer userId, int branchId) throws SQLException {
        return repository.tx(conn -> {
            Order o = repository.orderDao.findById(conn, orderId);
            if (o == null || o.getBranchId() != branchId || !"ACTIVE".equals(o.getStatus())) return false;
            List<OrderItem> items = repository.itemDao.findByOrder(conn, orderId);
            // guard R5: có món đã/đang pha → chặn huỷ
            for (OrderItem it : items) {
                String s = it.getStatus();
                if ("MAKING".equals(s) || "READY".equals(s) || "PICKED_UP".equals(s) || "SERVED".equals(s)) return false;
            }
            for (OrderItem it : items) {
                if ("WAITING".equals(it.getStatus())) {
                    // Món đã qua làm lại có thể đang giữ chỗ nguyên liệu cho lượt kế tiếp — hoàn trước khi huỷ.
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
