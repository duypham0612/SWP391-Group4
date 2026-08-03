package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.IngredientDao;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.math.*;
import java.sql.*;
import java.util.*;

/** Cửa ghi ledger và các mutation trừ/giữ tồn theo order item. */
public final class InventoryLedgerService {
    private final InventoryRepository repository;
    public InventoryLedgerService(){this(new InventoryRepository());}
    InventoryLedgerService(InventoryRepository repository){this.repository=Objects.requireNonNull(repository);}

    public void applyTxn(Connection conn, int branchId, int ingredientId, BigDecimal delta,
                         TxnType type, InventoryReferenceType referenceType,
                         Long referenceId, Integer userId) throws SQLException {
        applyTxn(conn, branchId, ingredientId, delta, type, referenceType,
                referenceId == null ? null : String.valueOf(referenceId), userId);
    }

    public void applyTxn(Connection conn, int branchId, int ingredientId, BigDecimal delta,
                         TxnType type, InventoryReferenceType referenceType,
                         String referenceId, Integer userId) throws SQLException {
        // 1) Ghi sổ cái (append-only)
        repository.txnDao.insert(conn, branchId, ingredientId, delta, type.name(),
                referenceType, referenceId, userId);
        // 2) Cập nhật số dư cache
        repository.biDao.applyDelta(conn, branchId, ingredientId, delta);
        // 3) Cảnh báo tồn: ÂM (oversold — cần đối soát) tách riêng khỏi THẤP (chạm ngưỡng)
        BigDecimal[] qt = repository.biDao.findQtyAndThreshold(conn, branchId, ingredientId);
        if (qt != null && qt[0] != null) {
            if (qt[0].signum() < 0) {
                String payload = "{\"ingredientId\":" + ingredientId + ",\"qty\":" + qt[0] + "}";
                repository.outboxEventDao.insert(conn, EventType.STOCK_OVERSOLD, String.valueOf(ingredientId), branchId, payload);
            } else if (qt[1] != null && qt[0].compareTo(qt[1]) <= 0) {
                String payload = "{\"ingredientId\":" + ingredientId + ",\"qty\":" + qt[0] + ",\"min\":" + qt[1] + "}";
                repository.outboxEventDao.insert(conn, EventType.STOCK_LOW, String.valueOf(ingredientId), branchId, payload);
            }
        }
    }

    /**
     * ★ Modifier-Aware Auto-Deduction (Contract #1, #2) — trừ tồn khi Barista bấm READY.
     * Chạy TRONG tx của caller (KdsOrderWorkflowService.markItemReady). Đọc công thức + modifier đã chọn,
     * tính required qua {@link DeductionCalculator}, trừ đúng ingredient công thức tham chiếu
     * (PREPPED trừ tồn PREPPED — KHÔNG trừ RAW lần 2). Publish inventory.deducted.
     */
    public void deductForOrderItem(Connection conn, int branchId, int orderItemId, int productId,
                                   int quantity, Integer userId) throws SQLException {
        List<Recipe> recipe = repository.productRecipeDao.findByProduct(conn, productId);
        if (recipe.isEmpty()) {
            // Nói rõ lối thoát: món này sẽ không bao giờ bấm Xong được cho tới khi Quản trị khai
            // công thức, nên barista cần chuyển nó ra khỏi hàng chờ chứ không phải thử lại.
            throw new BusinessException("Món chưa có công thức nên không xác định được nguyên liệu cần trừ. "
                    + "Hãy bấm Báo sự cố → \"Món đã ngừng bán\" để chuyển sang mục Cần xử lý, rồi báo Quản trị khai công thức.");
        }
        List<Recipe> impacts = new ArrayList<>();
        for (Integer optionId : repository.oimDao.findOptionIds(conn, orderItemId)) {
            impacts.addAll(repository.impactDao.findByOption(conn, optionId));
        }
        Map<Integer, BigDecimal> required = DeductionCalculator.computeRequired(recipe, impacts, quantity);
        if (required.isEmpty()) {
            throw new BusinessException("Công thức món không có định lượng hợp lệ — chưa thể hoàn thành.");
        }
        for (Map.Entry<Integer, BigDecimal> e : required.entrySet()) {
            applyTxn(conn, branchId, e.getKey(), e.getValue().negate(),
                    TxnType.DEDUCT, InventoryReferenceType.ORDER_ITEM, (long) orderItemId, userId);
        }
        repository.outboxEventDao.insert(conn, EventType.INVENTORY_DEDUCTED, String.valueOf(orderItemId), branchId,
                "{\"orderItemId\":" + orderItemId + ",\"productId\":" + productId + ",\"qty\":" + quantity + "}");
    }


}
