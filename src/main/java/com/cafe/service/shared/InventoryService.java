package com.cafe.service.shared;

import com.cafe.common.DeductionCalculator;
import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.common.TxnType;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchInventoryDao;
import com.cafe.dao.shared.InventoryTransactionDao;
import com.cafe.dao.shared.ModifierIngredientImpactDao;
import com.cafe.dao.shared.OrderItemModifierDao;
import com.cafe.dao.shared.PrepBatchDao;
import com.cafe.dao.shared.PrepRecipeDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.dao.shared.StockAdjustmentDao;
import com.cafe.dao.shared.StockReceiptDetailDao;
import com.cafe.dao.shared.WasteLogDao;
import com.cafe.model.BranchInventory;
import com.cafe.model.InventoryTransaction;
import com.cafe.model.ModifierIngredientImpact;
import com.cafe.model.PrepRecipe;
import com.cafe.model.ProductRecipe;
import com.cafe.model.StockReceiptDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CỬA DUY NHẤT đổi tồn kho (đặc tả mục 3 + nguyên tắc bất biến).
 * applyTxn = INSERT InventoryTransaction + UPDATE BranchInventory + publish stock.low — TẤT CẢ trong tx do caller mở.
 * KHÔNG nơi nào được UPDATE thẳng BranchInventory ngoài DAO này (gọi qua applyTxn).
 */
public class InventoryService {

    private final BranchInventoryDao biDao = new BranchInventoryDao();
    private final InventoryTransactionDao txnDao = new InventoryTransactionDao();
    private final StockReceiptDetailDao detailDao = new StockReceiptDetailDao();
    private final StockAdjustmentDao adjustmentDao = new StockAdjustmentDao();
    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();
    private final ModifierIngredientImpactDao impactDao = new ModifierIngredientImpactDao();
    private final OrderItemModifierDao oimDao = new OrderItemModifierDao();
    private final PrepRecipeDao prepRecipeDao = new PrepRecipeDao();
    private final PrepBatchDao prepBatchDao = new PrepBatchDao();
    private final WasteLogDao wasteLogDao = new WasteLogDao();

    /** LÕI — chạy trong transaction do caller mở (caller chịu trách nhiệm commit/rollback). */
    public void applyTxn(Connection conn, int branchId, int ingredientId, BigDecimal delta,
                         TxnType type, String refTable, Long refId, Integer userId) throws SQLException {
        // 1) Ghi sổ cái (append-only)
        txnDao.insert(conn, branchId, ingredientId, delta, type.name(), refTable, refId, userId);
        // 2) Cập nhật số dư cache
        biDao.applyDelta(conn, branchId, ingredientId, delta);
        // 3) Cảnh báo tồn thấp nếu chạm ngưỡng
        BigDecimal[] qt = biDao.findQtyAndThreshold(conn, branchId, ingredientId);
        if (qt != null && qt[0] != null && qt[1] != null && qt[0].compareTo(qt[1]) <= 0) {
            String payload = "{\"ingredientId\":" + ingredientId + ",\"qty\":" + qt[0] + ",\"min\":" + qt[1] + "}";
            EventPublisher.publish(conn, EventType.STOCK_LOW, String.valueOf(ingredientId), branchId, payload);
        }
    }

    /**
     * ★ Modifier-Aware Auto-Deduction (Contract #1, #2) — trừ tồn khi Barista bấm READY.
     * Chạy TRONG tx của caller (OrderService.markItemReady). Đọc công thức + modifier đã chọn,
     * tính required qua {@link DeductionCalculator}, trừ đúng ingredient công thức tham chiếu
     * (PREPPED trừ tồn PREPPED — KHÔNG trừ RAW lần 2). Publish inventory.deducted.
     */
    public void deductForOrderItem(Connection conn, int branchId, int orderItemId, int productId,
                                   int quantity, Integer userId) throws SQLException {
        List<ProductRecipe> recipe = productRecipeDao.findByProduct(conn, productId);
        List<ModifierIngredientImpact> impacts = new ArrayList<>();
        for (Integer optionId : oimDao.findOptionIds(conn, orderItemId)) {
            impacts.addAll(impactDao.findByOption(conn, optionId));
        }
        Map<Integer, BigDecimal> required = DeductionCalculator.computeRequired(recipe, impacts, quantity);
        for (Map.Entry<Integer, BigDecimal> e : required.entrySet()) {
            applyTxn(conn, branchId, e.getKey(), e.getValue().negate(),
                    TxnType.DEDUCT, "OrderItem", (long) orderItemId, userId);
        }
        EventPublisher.publish(conn, EventType.INVENTORY_DEDUCTED, String.valueOf(orderItemId), branchId,
                "{\"orderItemId\":" + orderItemId + ",\"productId\":" + productId + ",\"qty\":" + quantity + "}");
    }

    /**
     * Tạo mẻ pha sẵn (Contract #2) — NƠI DUY NHẤT đổi RAW→PREPPED. Own tx.
     * Trừ RAW theo PrepRecipe (consumed = qtyProduced/yield × qtyPerYield), cộng PREPPED qtyProduced.
     */
    public int createPrepBatch(int branchId, int preppedIngredientId, BigDecimal qtyProduced, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int batchId = prepBatchDao.insert(conn, branchId, preppedIngredientId, qtyProduced, userId);
                List<PrepRecipe> lines = prepRecipeDao.findByPrepped(conn, preppedIngredientId);
                for (PrepRecipe pr : lines) {
                    BigDecimal consumed = qtyProduced
                            .divide(pr.getYieldQty(), 6, RoundingMode.HALF_UP)
                            .multiply(pr.getQuantity());
                    applyTxn(conn, branchId, pr.getRawIngredientId(), consumed.negate(),
                            TxnType.PREP_OUT, "PrepBatch", (long) batchId, userId);
                }
                applyTxn(conn, branchId, preppedIngredientId, qtyProduced,
                        TxnType.PREP_IN, "PrepBatch", (long) batchId, userId);
                conn.commit();
                return batchId;
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Huỷ mẻ pha sẵn (Contract #2, #4) — HOÀN KHO BẰNG TXN BÙ, không hard-delete.
     * Đảo lại createPrepBatch: cộng RAW về (PREP_OUT dấu dương) + rút PREPPED đã cộng (PREP_IN dấu âm),
     * cùng RefTable/RefId nên ledger nets về 0 theo từng type. Đánh dấu Status='CANCELLED'. Own tx.
     */
    public void cancelPrepBatch(int branchId, int prepBatchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.PrepBatch b = prepBatchDao.findById(conn, prepBatchId);
                if (b == null || b.getBranchId() != branchId) throw new SQLException("Mẻ pha không tồn tại ở chi nhánh này.");
                if (!b.isActive()) { conn.rollback(); return; }   // idempotent: đã huỷ rồi
                BigDecimal qtyProduced = b.getQuantityProduced();
                List<PrepRecipe> lines = prepRecipeDao.findByPrepped(conn, b.getPreppedIngredientId());
                for (PrepRecipe pr : lines) {
                    BigDecimal consumed = qtyProduced
                            .divide(pr.getYieldQty(), 6, RoundingMode.HALF_UP)
                            .multiply(pr.getQuantity());
                    applyTxn(conn, branchId, pr.getRawIngredientId(), consumed,   // hoàn RAW (+)
                            TxnType.PREP_OUT, "PrepBatch", (long) prepBatchId, userId);
                }
                applyTxn(conn, branchId, b.getPreppedIngredientId(), qtyProduced.negate(),  // rút PREPPED (-)
                        TxnType.PREP_IN, "PrepBatch", (long) prepBatchId, userId);
                prepBatchDao.updateStatus(conn, prepBatchId, "CANCELLED");
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Sửa sản lượng mẻ pha sẵn (Contract #2, #4) — áp TXN cho phần CHÊNH LỆCH (delta = new − old).
     * delta>0: trừ thêm RAW + cộng thêm PREPPED; delta<0: hoàn RAW + rút bớt PREPPED. Own tx.
     */
    public void updatePrepBatch(int branchId, int prepBatchId, BigDecimal newQtyProduced, int userId) throws SQLException {
        if (newQtyProduced == null || newQtyProduced.signum() <= 0) throw new IllegalArgumentException("Sản lượng phải > 0");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.PrepBatch b = prepBatchDao.findById(conn, prepBatchId);
                if (b == null || b.getBranchId() != branchId) throw new SQLException("Mẻ pha không tồn tại ở chi nhánh này.");
                if (!b.isActive()) throw new SQLException("Mẻ đã huỷ — không sửa được.");
                BigDecimal delta = newQtyProduced.subtract(b.getQuantityProduced());
                if (delta.signum() != 0) {
                    List<PrepRecipe> lines = prepRecipeDao.findByPrepped(conn, b.getPreppedIngredientId());
                    for (PrepRecipe pr : lines) {
                        BigDecimal consumedDelta = delta
                                .divide(pr.getYieldQty(), 6, RoundingMode.HALF_UP)
                                .multiply(pr.getQuantity());
                        applyTxn(conn, branchId, pr.getRawIngredientId(), consumedDelta.negate(),  // delta>0 trừ thêm RAW
                                TxnType.PREP_OUT, "PrepBatch", (long) prepBatchId, userId);
                    }
                    applyTxn(conn, branchId, b.getPreppedIngredientId(), delta,                    // delta>0 cộng thêm PREPPED
                            TxnType.PREP_IN, "PrepBatch", (long) prepBatchId, userId);
                    prepBatchDao.updateQuantity(conn, prepBatchId, newQtyProduced);
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Ghi hao hụt (Barista) — insert WasteLog + applyTxn(-qty, WASTE). Own tx. */
    public int logWaste(int branchId, int ingredientId, BigDecimal qty, String wasteType, String reason, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = wasteLogDao.insert(conn, branchId, ingredientId, qty, wasteType, reason, userId);
                applyTxn(conn, branchId, ingredientId, qty.negate(), TxnType.WASTE, "WasteLog", (long) id, userId);
                conn.commit();
                return id;
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public List<com.cafe.model.PrepBatch> getPrepBatches(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return prepBatchDao.findByBranch(conn, branchId); }
    }

    public List<com.cafe.model.WasteLog> getWasteLogs(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return wasteLogDao.findByBranch(conn, branchId); }
    }

    /** Nhập kho (Manager) — cộng tồn theo từng dòng phiếu, trả về tổng tiền. Chạy trong tx của caller. */
    public BigDecimal confirmReceiptStock(Connection conn, int receiptId, int branchId, Integer userId) throws SQLException {
        List<StockReceiptDetail> details = detailDao.findByReceipt(conn, receiptId);
        BigDecimal totalCost = BigDecimal.ZERO;
        for (StockReceiptDetail d : details) {
            applyTxn(conn, branchId, d.getIngredientId(), d.getQuantity(),
                    TxnType.RECEIPT, "StockReceipt", (long) receiptId, userId);
            totalCost = totalCost.add(d.getLineCost());
        }
        return totalCost;
    }

    /** Điều chỉnh tồn sau kiểm kê (Manager) — ghi StockAdjustment + applyTxn(diff, ADJUST). */
    public void createAdjustment(int branchId, int ingredientId, BigDecimal actualQty, String reason, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal[] qt = biDao.findQtyAndThreshold(conn, branchId, ingredientId);
                BigDecimal systemQty = (qt == null || qt[0] == null) ? BigDecimal.ZERO : qt[0];
                int adjId = adjustmentDao.insert(conn, branchId, ingredientId, systemQty, actualQty, reason, userId);
                BigDecimal diff = actualQty.subtract(systemQty);
                if (diff.signum() != 0) {
                    applyTxn(conn, branchId, ingredientId, diff, TxnType.ADJUST, "StockAdjustment", (long) adjId, userId);
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    // ----- Đọc -----
    public List<BranchInventory> getBranchInventory(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return biDao.findByBranch(conn, branchId); }
    }

    public List<BranchInventory> getLowStock(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return biDao.findLowStock(conn, branchId); }
    }

    public List<InventoryTransaction> getIngredientLedger(int branchId, int ingredientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return txnDao.findByBranchIngredient(conn, branchId, ingredientId); }
    }

    public void setMinThreshold(int branchId, int ingredientId, BigDecimal threshold) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { biDao.updateThreshold(conn, branchId, ingredientId, threshold); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
