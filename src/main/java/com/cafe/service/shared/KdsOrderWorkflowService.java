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

/** Nhận pha, hoàn tất và thu hồi món tại KDS. */
public final class KdsOrderWorkflowService {
    private final OrderRepository repository;
    public KdsOrderWorkflowService() { this(new OrderRepository()); }
    KdsOrderWorkflowService(OrderRepository repository) { this.repository = Objects.requireNonNull(repository); }

    private static String sanitizeReason(String reason) {
        if (reason == null) return null;
        String value = reason.trim();
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    public boolean startItem(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            int rows = repository.itemDao.claim(conn, orderItemId, sessionBranchId, userId);
            if (rows == 0) return false;   // sai trạng thái / khác chi nhánh / đã bị đổi
            repository.actionDao.insert(conn, orderItemId, sessionBranchId, "CLAIM", "WAITING", "MAKING", null, userId);
            repository.publishStatus(conn, it, "MAKING");
            return true;
        });
    }

    /**
     * ★ MAKING → READY — chỉ người đã nhận mới được hoàn thành.
     * Đây là điểm auto-deduct (Contract #1, #2). Chốt trạng thái NGUYÊN TỬ (updateStatusIf) TRƯỚC khi
     * trừ kho: chỉ request "claim" được món (rows==1) mới trừ → chống double-deduct khi 2 barista bấm
     * song song; scope chi nhánh chặn thao tác chéo chi nhánh.
     */
    public boolean markItemReady(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            return completeInTx(conn, it, userId, sessionBranchId);
        });
    }

    /**
     * Thân của MAKING → READY, gọi TRONG tx của caller — dùng chung cho bản một món và bản cả đơn.
     * Tách ra vì đây là điểm auto-deduct: hai bản sao chép logic thì chỉ cần một bên được sửa mà
     * bên kia quên là sổ kho lệch, và lỗi đó không lộ ra cho tới lúc kiểm kê.
     */
    private boolean completeInTx(Connection conn, OrderItem it, int userId, int sessionBranchId) throws SQLException {
        int orderItemId = it.getOrderItemId();
        int rows = repository.itemDao.completeClaimed(conn, orderItemId, sessionBranchId, userId);
        if (rows == 0) return false;   // đã READY/SERVED/CANCELLED / khác chi nhánh → không trừ kho
        int branchId = repository.branchOf(it);
        if (!it.isRemakeInventoryReserved()) {
            repository.inventoryService.deductForOrderItem(conn, branchId, orderItemId, it.getProductId(), it.getQuantity(), userId);
        }
        repository.actionDao.insert(conn, orderItemId, branchId, "COMPLETE", "MAKING", "READY", null, userId);
        repository.publishStatus(conn, it, "READY");
        repository.outboxEventDao.insert(conn, EventType.ITEM_READY, String.valueOf(orderItemId), branchId,
                "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId + ",\"by\":" + userId + "}");
        return true;
    }

    /**
     * Nhận pha MỌI món còn chờ của một đơn, trong một transaction. Trả số món nhận được.
     *
     * <p>Từng món vẫn đi qua đúng WHERE-guard của {@code claim} (đang WAITING · đơn ACTIVE · đúng
     * chi nhánh), nên món vừa bị barista khác nhận chỉ đơn giản không được tính — không kéo theo
     * huỷ cả lượt bấm.
     */
    public int startAllInOrder(int orderId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return 0;
        return repository.tx(conn -> {
            int count = 0;
            for (OrderItem it : repository.itemDao.findByOrder(conn, orderId)) {
                if (!"WAITING".equals(it.getStatus())) continue;
                if (repository.itemDao.claim(conn, it.getOrderItemId(), sessionBranchId, userId) == 0) continue;
                repository.actionDao.insert(conn, it.getOrderItemId(), sessionBranchId, "CLAIM", "WAITING", "MAKING", null, userId);
                repository.publishStatus(conn, it, "MAKING");
                count++;
            }
            return count;
        });
    }

    /** Kết quả "Xong cả đơn": số món đã hoàn thành và số món phải bỏ qua vì chưa khai công thức. */
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

    /**
     * Hoàn thành MỌI món mà chính barista này đang pha trong một đơn, trong MỘT transaction
     * (một đơn = một lần trừ kho trọn gói).
     *
     * <p>Món chưa khai công thức bị loại TRƯỚC vòng lặp: {@code deductForOrderItem} ném
     * {@link BusinessException} khi công thức rỗng, mà ném giữa vòng lặp thì cả đơn rollback chỉ
     * vì một dòng — các ly đã pha xong thật sẽ quay ngược về "đang pha".
     */
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
                if (!userId.equals(it.getBaristaId())) continue;          // chỉ món của chính mình
                if (!withRecipe.contains(it.getProductId())) { skipped++; continue; }
                if (completeInTx(conn, it, userId, sessionBranchId)) completed++;
            }
            return new BulkReadyResult(completed, skipped);
        });
    }

    /**
     * Số ly barista đang giữ dở — cổng tan ca. Barista tan ca mà còn món MAKING thì món đó bị
     * khoá dưới tên người đã về: cả `completeClaimed` lẫn `returnToQueue` đều guard theo BaristaId
     * nên ca sau không đụng được, khách ngồi đợi mà không ai pha tiếp.
     */
    public int countMyMakingItems(int branchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.itemDao.countMakingByBarista(conn, branchId, userId);
        }
    }

    /**
     * Thu hồi món của barista ĐÃ RỜI CA về hàng chờ, để ca sau pha tiếp thay vì Thu ngân phải huỷ món.
     *
     * <p>Điều kiện "đã rời ca" kiểm ở đây chứ không tin nút trên màn: bảng quầy có thể đã dựng vài
     * phút trước và chủ món vừa quay lại. Chủ món còn trực → ném {@link BusinessException} để barista
     * đi nhờ họ trả lại, thay vì giật món khỏi tay người đang pha.
     *
     * @param onDutyUserIds những người còn đang trực tại chi nhánh, do caller nạp một lần cho cả bảng.
     * @return false khi món vừa bị đổi bởi thao tác khác (kể cả chính chủ vừa bấm Xong).
     */
    public boolean reclaimItem(int orderItemId, Integer actorUserId, int branchId, String actorName,
                               java.util.Set<Integer> onDutyUserIds) throws SQLException {
        if (actorUserId == null) return false;
        java.util.Set<Integer> onDuty = onDutyUserIds == null ? java.util.Set.of() : onDutyUserIds;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || it.getBaristaId() == null) return false;
            if (actorUserId.equals(it.getBaristaId())) return false;   // món của chính mình: dùng Trả lại chờ
            if (onDuty.contains(it.getBaristaId())) {
                throw new BusinessException("Người này vẫn đang trong ca — nhờ họ bấm “Trả lại chờ” cho món này.");
            }
            if (repository.itemDao.reclaim(conn, orderItemId, branchId, it.getBaristaId()) == 0) return false;
            String reason = "Thu hồi từ " + (it.getBaristaName() == null ? "barista đã rời ca" : it.getBaristaName())
                    + (actorName == null || actorName.isBlank() ? "" : " bởi " + actorName);
            repository.actionDao.insert(conn, orderItemId, branchId, "RETURN_QUEUE", "MAKING", "WAITING",
                    sanitizeReason(reason), actorUserId);
            repository.publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    /** MAKING → WAITING, chỉ chủ món được trả lại. */
    public boolean returnItemToQueue(int orderItemId, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        return repository.tx(conn -> {
            OrderItem it = repository.itemDao.findById(conn, orderItemId);
            if (it == null || repository.itemDao.returnToQueue(conn, orderItemId, branchId, userId) == 0) return false;
            repository.actionDao.insert(conn, orderItemId, branchId, "RETURN_QUEUE", "MAKING", "WAITING", null, userId);
            repository.publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    /** Báo sự cố không hủy món; WAITING ai cũng báo, MAKING chỉ chủ món báo. */

}
