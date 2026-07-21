package com.cafe.service.shared;

import com.cafe.common.BusinessException;
import com.cafe.common.DeductionCalculator;
import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.common.ExpiryWasteCalculator;
import com.cafe.common.PrepConsumptionCalculator;
import com.cafe.common.TxnType;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.BranchInventoryDao;
import com.cafe.dao.shared.InventoryTransactionDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.dao.shared.PrepBatchDao;
import com.cafe.dao.shared.PrepRecipeDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.dao.shared.StockAdjustmentDao;
import com.cafe.dao.shared.StockReceiptDetailDao;
import com.cafe.dao.shared.WasteLogDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.BranchInventory;
import com.cafe.model.InventoryTransaction;
import com.cafe.model.OrderItem;
import com.cafe.model.PrepRecipe;
import com.cafe.model.ProductRecipe;
import com.cafe.model.StockReceiptDetail;
import com.cafe.model.WasteLog;
import com.cafe.model.WasteLogLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CỬA DUY NHẤT đổi tồn kho (đặc tả mục 3 + nguyên tắc bất biến).
 * applyTxn = INSERT InventoryTransaction + UPDATE BranchInventory + publish stock.low — TẤT CẢ trong tx do caller mở.
 * KHÔNG nơi nào được UPDATE thẳng BranchInventory ngoài DAO này (gọi qua applyTxn).
 */
public class InventoryService {

    private final BranchInventoryDao biDao = new BranchInventoryDao();
    private final BranchMenuDao branchMenuDao = new BranchMenuDao();
    private final InventoryTransactionDao txnDao = new InventoryTransactionDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final StockReceiptDetailDao detailDao = new StockReceiptDetailDao();
    private final StockAdjustmentDao adjustmentDao = new StockAdjustmentDao();
    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();
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
        // 3) Cảnh báo tồn: ÂM (oversold — cần đối soát) tách riêng khỏi THẤP (chạm ngưỡng)
        BigDecimal[] qt = biDao.findQtyAndThreshold(conn, branchId, ingredientId);
        if (qt != null && qt[0] != null) {
            if (qt[0].signum() < 0) {
                String payload = "{\"ingredientId\":" + ingredientId + ",\"qty\":" + qt[0] + "}";
                EventPublisher.publish(conn, EventType.STOCK_OVERSOLD, String.valueOf(ingredientId), branchId, payload);
            } else if (qt[1] != null && qt[0].compareTo(qt[1]) <= 0) {
                String payload = "{\"ingredientId\":" + ingredientId + ",\"qty\":" + qt[0] + ",\"min\":" + qt[1] + "}";
                EventPublisher.publish(conn, EventType.STOCK_LOW, String.valueOf(ingredientId), branchId, payload);
            }
        }
    }

    /**
     * Auto-deduction (Contract #1, #2) — trừ tồn khi Barista bấm READY.
     * Chạy TRONG tx của caller (OrderService.markItemReady). Đọc công thức món,
     * tính required qua {@link DeductionCalculator}, trừ đúng ingredient công thức tham chiếu
     * (PREPPED trừ tồn PREPPED — KHÔNG trừ RAW lần 2). Publish inventory.deducted.
     */
    public void deductForOrderItem(Connection conn, int branchId, int orderItemId, int productId,
                                   int quantity, Integer userId) throws SQLException {
        List<ProductRecipe> recipe = productRecipeDao.findByProduct(conn, productId);
        if (recipe.isEmpty()) {
            throw new BusinessException("Món chưa có công thức — không thể hoàn thành vì chưa xác định nguyên liệu cần trừ.");
        }
        OrderItem item = orderItemDao.findById(conn, orderItemId);
        Map<Integer, BigDecimal> required = DeductionCalculator.computeRequired(recipe, quantity,
                item == null ? "M" : item.getSize(),
                item == null ? "Bình thường" : item.getIceLevel(),
                item == null ? "100%" : item.getSugarLevel());
        if (required.isEmpty()) {
            throw new BusinessException("Công thức món không có định lượng hợp lệ — chưa thể hoàn thành.");
        }
        for (Map.Entry<Integer, BigDecimal> e : required.entrySet()) {
            applyTxn(conn, branchId, e.getKey(), e.getValue().negate(),
                    TxnType.DEDUCT, "OrderItem", (long) orderItemId, userId);
        }
        EventPublisher.publish(conn, EventType.INVENTORY_DEDUCTED, String.valueOf(orderItemId), branchId,
                "{\"orderItemId\":" + orderItemId + ",\"productId\":" + productId + ",\"qty\":" + quantity + "}");
    }

    /**
     * Giữ nguyên liệu cho lượt làm lại ngay khi READY quay về hàng chờ. Các dòng được ghi là REMAKE/WASTE;
     * lần hoàn thành sau đó nhìn cờ RemakeInventoryReserved và không trừ lần nữa.
     */
    public void reserveRemakeForOrderItem(Connection conn, int branchId, int orderItemId, int productId,
                                          int quantity, String reason, int userId) throws SQLException {
        List<ProductRecipe> recipe = productRecipeDao.findByProduct(conn, productId);
        if (recipe.isEmpty()) throw new BusinessException("Món chưa có công thức — không thể ghi nhận hao hụt làm lại.");
        OrderItem item = orderItemDao.findById(conn, orderItemId);
        Map<Integer, BigDecimal> required = DeductionCalculator.computeRequired(recipe, quantity,
                item == null ? "M" : item.getSize(),
                item == null ? "Bình thường" : item.getIceLevel(),
                item == null ? "100%" : item.getSugarLevel());
        if (required.isEmpty()) throw new BusinessException("Công thức không có lượng nguyên liệu hợp lệ.");
        String note = cleanReason("Làm lại dòng món #" + orderItemId + (reason == null ? "" : " - " + reason));
        for (Map.Entry<Integer, BigDecimal> entry : required.entrySet()) {
            logWasteInTx(conn, branchId, entry.getKey(), entry.getValue(), "REMAKE", note, userId);
        }
    }

    /**
     * Tạo mẻ pha sẵn (Contract #2) — NƠI DUY NHẤT đổi RAW→PREPPED. Own tx.
     * Trừ RAW theo PrepRecipe (consumed = qtyProduced/yield × qtyPerYield), cộng PREPPED qtyProduced.
     */
    public int createPrepBatch(int branchId, int preppedIngredientId, BigDecimal qtyProduced,
                               java.time.LocalDateTime expiresAt, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int batchId = doCreatePrepBatch(conn, branchId, preppedIngredientId, qtyProduced, expiresAt, userId);
                conn.commit();
                return batchId;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }   // BusinessException → hoàn tác sạch
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Tạo NHIỀU mẻ pha sẵn trong MỘT transaction (Contract #2) — barista chọn nhiều món một lần.
     * Tất cả-hoặc-không: chỉ cần một dòng thiếu RAW/thiếu công thức → rollback toàn bộ, không mẻ nào được tạo.
     */
    public void createPrepBatches(int branchId, List<com.cafe.model.PrepBatchLine> lines, int userId) throws SQLException {
        if (lines == null || lines.isEmpty())
            throw new BusinessException("Chưa chọn nguyên liệu nào để pha.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (com.cafe.model.PrepBatchLine ln : lines) {
                    try {
                        doCreatePrepBatch(conn, branchId, ln.getPreppedIngredientId(),
                                ln.getQtyProduced(), ln.getExpiresAt(), userId);
                    } catch (BusinessException e) {
                        throw withPrepLineContext(ln, e);
                    }
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private BusinessException withPrepLineContext(com.cafe.model.PrepBatchLine line, BusinessException cause) {
        String name = line.getPreppedIngredientName();
        if (name == null || name.isBlank()) name = "Nguyên liệu #" + line.getPreppedIngredientId();
        String msg = cause.getMessage() == null ? "Không thể tạo mẻ." : cause.getMessage();
        if (msg.startsWith(name + ":")) return cause;
        return new BusinessException(name + ": " + msg);
    }

    /** Lõi tạo 1 mẻ — chạy TRONG tx của caller: chặn thiếu công thức, guard tồn RAW, insert + ledger. */
    private int doCreatePrepBatch(Connection conn, int branchId, int preppedIngredientId,
                                  BigDecimal qtyProduced, java.time.LocalDateTime expiresAt, int userId) throws SQLException {
        List<PrepRecipe> lines = prepRecipeDao.findByPrepped(conn, preppedIngredientId);
        // Chặn thiếu công thức: tránh cộng PREPPED mà không trừ RAW nào (sai Contract #2).
        if (lines.isEmpty())
            throw new BusinessException("Có nguyên liệu pha sẵn chưa khai báo công thức prep — không thể tạo mẻ.");
        // Tiền-kiểm đủ tồn RAW (chặn tồn âm). Tính lượng tiêu hao từng RAW.
        List<String> shortfalls = new ArrayList<>();
        for (PrepRecipe pr : lines) {
            BigDecimal consumed = PrepConsumptionCalculator.consumedRaw(qtyProduced, pr);
            BigDecimal[] qt = biDao.findQtyAndThreshold(conn, branchId, pr.getRawIngredientId());
            BigDecimal onHand = (qt == null || qt[0] == null) ? BigDecimal.ZERO : qt[0];
            if (onHand.compareTo(consumed) < 0)
                shortfalls.add(pr.getRawIngredientName() + ": cần " + plain(consumed)
                        + " / còn " + plain(onHand) + " " + pr.getRawIngredientUnit());
        }
        if (!shortfalls.isEmpty())
            throw new BusinessException("Không đủ nguyên liệu thô để pha: " + String.join("; ", shortfalls) + ".");

        int batchId = prepBatchDao.insert(conn, branchId, preppedIngredientId, qtyProduced, expiresAt, userId);
        for (PrepRecipe pr : lines) {
            applyTxn(conn, branchId, pr.getRawIngredientId(), PrepConsumptionCalculator.consumedRaw(qtyProduced, pr).negate(),
                    TxnType.PREP_OUT, "PrepBatch", (long) batchId, userId);
        }
        applyTxn(conn, branchId, preppedIngredientId, qtyProduced,
                TxnType.PREP_IN, "PrepBatch", (long) batchId, userId);
        return batchId;
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
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
                requirePreppedOnHandForReduction(conn, branchId, b.getPreppedIngredientId(),
                        b.getPreppedIngredientName(), b.getPreppedIngredientUnit(), qtyProduced, "huỷ mẻ");
                List<PrepRecipe> lines = prepRecipeDao.findByPrepped(conn, b.getPreppedIngredientId());
                for (PrepRecipe pr : lines) {
                    BigDecimal consumed = PrepConsumptionCalculator.consumedRaw(qtyProduced, pr);
                    applyTxn(conn, branchId, pr.getRawIngredientId(), consumed,   // hoàn RAW (+)
                            TxnType.PREP_OUT, "PrepBatch", (long) prepBatchId, userId);
                }
                applyTxn(conn, branchId, b.getPreppedIngredientId(), qtyProduced.negate(),  // rút PREPPED (-)
                        TxnType.PREP_IN, "PrepBatch", (long) prepBatchId, userId);
                if (prepBatchDao.updateStatus(conn, prepBatchId, "CANCELLED") != 1) {
                    throw new BusinessException("Mẻ đã được thay đổi bởi thao tác khác. Vui lòng tải lại.");
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
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
                    // Tăng sản lượng → trừ thêm RAW: tiền-kiểm đủ tồn (chặn tồn âm).
                    if (delta.signum() > 0) {
                        List<String> shortfalls = new ArrayList<>();
                        for (PrepRecipe pr : lines) {
                            BigDecimal need = PrepConsumptionCalculator.consumedRaw(delta, pr);
                            BigDecimal[] qt = biDao.findQtyAndThreshold(conn, branchId, pr.getRawIngredientId());
                            BigDecimal onHand = (qt == null || qt[0] == null) ? BigDecimal.ZERO : qt[0];
                            if (onHand.compareTo(need) < 0)
                                shortfalls.add(pr.getRawIngredientName() + ": cần thêm " + plain(need)
                                        + " / còn " + plain(onHand) + " " + pr.getRawIngredientUnit());
                        }
                        if (!shortfalls.isEmpty())
                            throw new BusinessException("Không đủ nguyên liệu thô để tăng sản lượng: " + String.join("; ", shortfalls) + ".");
                    } else {
                        requirePreppedOnHandForReduction(conn, branchId, b.getPreppedIngredientId(),
                                b.getPreppedIngredientName(), b.getPreppedIngredientUnit(), delta.abs(), "giảm sản lượng mẻ");
                    }
                    for (PrepRecipe pr : lines) {
                        BigDecimal consumedDelta = PrepConsumptionCalculator.consumedRaw(delta, pr);
                        applyTxn(conn, branchId, pr.getRawIngredientId(), consumedDelta.negate(),  // delta>0 trừ thêm RAW
                                TxnType.PREP_OUT, "PrepBatch", (long) prepBatchId, userId);
                    }
                    applyTxn(conn, branchId, b.getPreppedIngredientId(), delta,                    // delta>0 cộng thêm PREPPED
                            TxnType.PREP_IN, "PrepBatch", (long) prepBatchId, userId);
                    if (prepBatchDao.updateQuantity(conn, prepBatchId, newQtyProduced, b.getQuantityProduced()) != 1) {
                        throw new BusinessException("Mẻ đã được thay đổi bởi thao tác khác. Vui lòng tải lại.");
                    }
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }   // BusinessException → hoàn tác sạch
            finally { conn.setAutoCommit(true); }
        }
    }

    private void requirePreppedOnHandForReduction(Connection conn, int branchId, int preppedIngredientId,
                                                  String name, String unit, BigDecimal qtyToRemove,
                                                  String actionLabel) throws SQLException {
        BigDecimal[] qt = biDao.findQtyAndThreshold(conn, branchId, preppedIngredientId);
        BigDecimal onHand = (qt == null || qt[0] == null) ? BigDecimal.ZERO : qt[0];
        if (onHand.compareTo(qtyToRemove) >= 0) return;

        String ingredient = (name == null || name.isBlank()) ? "nguyên liệu pha sẵn" : name;
        String suffix = unit == null || unit.isBlank() ? "" : " " + unit;
        // BranchInventory gộp theo nguyên liệu, không theo từng mẻ; guard này chặn rút quá tồn hiện có.
        throw new BusinessException("Không thể " + actionLabel + " " + ingredient + ": cần rút "
                + plain(qtyToRemove) + suffix + " nhưng tồn hiện còn " + plain(onHand) + suffix
                + ". Phần còn lại có thể đã được dùng; hãy ghi hao hụt phần tồn còn lại hoặc báo Quản lý kiểm kê.");
    }

    /** Ghi hao hụt (Barista) — insert WasteLog + applyTxn(-qty, WASTE). Own tx. */
    public int logWaste(int branchId, int ingredientId, BigDecimal qty, String wasteType, String reason, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = logWasteInTx(conn, branchId, ingredientId, qty, wasteType, reason, userId);
                conn.commit();
                return id;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Ghi nhiều dòng hao hụt nguyên liệu trong một transaction. */
    public int logWasteLines(int branchId, List<WasteLogLine> lines, int userId) throws SQLException {
        if (lines == null || lines.isEmpty()) throw new BusinessException("Chưa có dòng hao hụt nào để ghi.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int count = 0;
                for (WasteLogLine line : lines) {
                    requireIngredientWasteType(line.getWasteType());
                    requirePositive(line.getQuantity(), "Số lượng phải > 0.");
                    logWasteInTx(conn, branchId, line.getIngredientId(), line.getQuantity(),
                            normalizeWasteType(line.getWasteType()), cleanReason(line.getReason()), userId);
                    count++;
                }
                conn.commit();
                return count;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public int remakeProduct(int branchId, int productId, int quantity, String reason, int userId) throws SQLException {
        return remakeProduct(branchId, productId, quantity, "M", "Bình thường", "100%", reason, userId);
    }

    public int remakeProduct(int branchId, int productId, int quantity,
                             String size, String iceLevel, String sugarLevel,
                             String reason, int userId) throws SQLException {
        if (quantity <= 0) throw new BusinessException("Số lượng món làm lại phải > 0.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BranchMenuItem product = findPublishedProduct(conn, branchId, productId);
                if (product == null) throw new BusinessException("Món không thuộc menu chi nhánh này.");

                List<ProductRecipe> recipe = productRecipeDao.findByProduct(conn, productId);
                if (recipe.isEmpty()) throw new BusinessException(product.getProductName() + ": Chưa khai báo công thức món.");

                Map<Integer, BigDecimal> required = DeductionCalculator.computeRequired(recipe, quantity, size, iceLevel, sugarLevel);
                if (required.isEmpty()) throw new BusinessException(product.getProductName() + ": Công thức không có lượng nguyên liệu hợp lệ.");

                String optionNote = "Size " + normalizeSize(size) + ", " + normalizeIce(iceLevel) + ", đường " + normalizeSugar(sugarLevel);
                String finalReason = cleanReason("Làm lại " + product.getProductName() + " (" + optionNote + ") x" + quantity
                        + (reason == null || reason.isBlank() ? "" : " - " + reason.trim()));
                int count = 0;
                for (Map.Entry<Integer, BigDecimal> e : required.entrySet()) {
                    logWasteInTx(conn, branchId, e.getKey(), e.getValue(), "REMAKE", finalReason, userId);
                    count++;
                }
                conn.commit();
                return count;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private static String normalizeSize(String size) {
        if ("S".equalsIgnoreCase(size) || "L".equalsIgnoreCase(size)) return size.toUpperCase(java.util.Locale.ROOT);
        return "M";
    }

    private static String normalizeIce(String iceLevel) {
        if ("Không đá".equals(iceLevel) || "Ít đá".equals(iceLevel) || "Nhiều đá".equals(iceLevel)) return iceLevel;
        return "Bình thường";
    }

    private static String normalizeSugar(String sugarLevel) {
        if ("0%".equals(sugarLevel) || "30%".equals(sugarLevel) || "50%".equals(sugarLevel)
                || "70%".equals(sugarLevel) || "100%".equals(sugarLevel)) return sugarLevel;
        return "100%";
    }

    /**
     * Sửa dòng hao hụt (Contract #4) — áp TXN cho phần chênh lệch số lượng (delta = new − old).
     * delta>0: trừ thêm; delta<0: hoàn lại. Cập nhật WasteLog. Own tx.
     */
    public void updateWaste(int branchId, int wasteLogId, BigDecimal newQty, String wasteType, String reason, int userId) throws SQLException {
        if (newQty == null || newQty.signum() <= 0) throw new IllegalArgumentException("Số lượng phải > 0");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.WasteLog w = wasteLogDao.findById(conn, wasteLogId);
                if (w == null || w.getBranchId() != branchId) throw new SQLException("Bản ghi hao hụt không tồn tại ở chi nhánh này.");
                if (!w.isActive()) throw new SQLException("Bản ghi đã huỷ — không sửa được.");
                if (w.isRemake()) throw new BusinessException("Dòng làm lại món không sửa lẻ; hãy huỷ rồi ghi lại nếu cần.");
                requireIngredientWasteType(wasteType);
                BigDecimal delta = newQty.subtract(w.getQuantity());
                if (delta.signum() != 0) {
                    applyTxn(conn, branchId, w.getIngredientId(), delta.negate(),  // delta>0 trừ thêm tồn
                            TxnType.WASTE, "WasteLog", (long) wasteLogId, userId);
                }
                if (wasteLogDao.update(conn, wasteLogId, newQty, normalizeWasteType(wasteType),
                        cleanReason(reason), w.getQuantity()) != 1) {
                    throw new BusinessException("Bản ghi hao hụt đã được thay đổi bởi thao tác khác. Vui lòng tải lại.");
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Huỷ dòng hao hụt (Contract #4) — HOÀN KHO BẰNG TXN BÙ (+qty WASTE), đánh dấu VOIDED.
     * KHÔNG hard-delete, KHÔNG UPDATE thẳng tồn. Own tx.
     */
    public void voidWaste(int branchId, int wasteLogId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.WasteLog w = wasteLogDao.findById(conn, wasteLogId);
                if (w == null || w.getBranchId() != branchId) throw new SQLException("Bản ghi hao hụt không tồn tại ở chi nhánh này.");
                if (!w.isActive()) { conn.rollback(); return; }   // idempotent
                if (w.isRemake()) {
                    throw new BusinessException("Dòng làm lại món gắn với ly đã pha nên không huỷ lẻ được. Nếu tồn kho sai, báo Quản lý kiểm kê lại.");
                }
                applyTxn(conn, branchId, w.getIngredientId(), w.getQuantity(),  // hoàn lại tồn (+)
                        TxnType.WASTE, "WasteLog", (long) wasteLogId, userId);
                if (wasteLogDao.updateStatus(conn, wasteLogId, "VOIDED") != 1) {
                    throw new BusinessException("Bản ghi hao hụt đã được thay đổi bởi thao tác khác. Vui lòng tải lại.");
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private int logWasteInTx(Connection conn, int branchId, int ingredientId, BigDecimal qty,
                             String wasteType, String reason, int userId) throws SQLException {
        requirePositive(qty, "Số lượng phải > 0.");
        int id = wasteLogDao.insert(conn, branchId, ingredientId, qty, normalizeWasteType(wasteType), cleanReason(reason), userId);
        applyTxn(conn, branchId, ingredientId, qty.negate(), TxnType.WASTE, "WasteLog", (long) id, userId);
        return id;
    }

    private BranchMenuItem findPublishedProduct(Connection conn, int branchId, int productId) throws SQLException {
        for (BranchMenuItem item : branchMenuDao.listForBranch(conn, branchId)) {
            if (item.getProductId() == productId && item.isPublished()) return item;
        }
        return null;
    }

    private static void requirePositive(BigDecimal qty, String message) {
        if (qty == null || qty.signum() <= 0) throw new BusinessException(message);
    }

    private static void requireIngredientWasteType(String wasteType) {
        String type = normalizeWasteType(wasteType);
        if (!"SPILL".equals(type) && !"EXPIRED".equals(type) && !"OTHER".equals(type)) {
            throw new BusinessException("Hao hụt nguyên liệu chỉ gồm Đổ/rơi, Hết hạn hoặc Khác. Làm lại món dùng form riêng.");
        }
    }

    private static String normalizeWasteType(String wasteType) {
        if (wasteType == null || wasteType.isBlank()) return "OTHER";
        String type = wasteType.trim().toUpperCase(java.util.Locale.ROOT);
        if ("SPILL".equals(type) || "EXPIRED".equals(type) || "REMAKE".equals(type) || "OTHER".equals(type)) return type;
        return "OTHER";
    }

    private static String cleanReason(String reason) {
        if (reason == null) return null;
        String value = reason.trim();
        if (value.isEmpty()) return null;
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    public List<com.cafe.model.PrepBatch> getPrepBatches(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return prepBatchDao.findByBranch(conn, branchId); }
    }

    /** B4 · Mẻ pha HÔM NAY (thay vì toàn bộ lịch sử). */
    public List<com.cafe.model.PrepBatch> getTodayPrepBatches(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return prepBatchDao.findTodayByBranch(conn, branchId); }
    }

    /** F4 · Me ACTIVE da qua han, kem so hao hut de xuat an toan. */
    public List<com.cafe.model.PrepBatch> getExpiredActivePrepBatches(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<com.cafe.model.PrepBatch> batches = prepBatchDao.findExpiredActive(conn, branchId);
            for (com.cafe.model.PrepBatch batch : batches) {
                batch.setSuggestedWasteQuantity(ExpiryWasteCalculator.suggestedWasteQuantity(batch));
            }
            return batches;
        }
    }

    /** Mẻ pha hôm nay theo trang — tìm kiếm, bộ lọc và phân trang đều được chạy ở database. */
    public PrepBatchPage getTodayPrepBatchPage(int branchId, String query, int ingredientId,
                                               String expiry, String status, int requestedPage, int pageSize) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            int total = prepBatchDao.countTodayByBranch(conn, branchId, query, ingredientId, expiry, status);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
            int page = Math.max(1, Math.min(requestedPage, totalPages));
            List<com.cafe.model.PrepBatch> batches = prepBatchDao.findTodayPageByBranch(conn, branchId,
                    query, ingredientId, expiry, status, (page - 1) * pageSize, pageSize);
            return new PrepBatchPage(batches, total, page, pageSize);
        }
    }

    public static class PrepBatchPage {
        private final List<com.cafe.model.PrepBatch> batches;
        private final int total;
        private final int page;
        private final int pageSize;

        public PrepBatchPage(List<com.cafe.model.PrepBatch> batches, int total, int page, int pageSize) {
            this.batches = batches;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<com.cafe.model.PrepBatch> getBatches() { return batches; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return Math.max(1, (int) Math.ceil((double) total / pageSize)); }
        public boolean isHasPrevious() { return page > 1; }
        public boolean isHasNext() { return page < getTotalPages(); }
        public int getStartRow() { return total == 0 ? 0 : (page - 1) * pageSize + 1; }
        public int getEndRow() { return Math.min(page * pageSize, total); }

        public List<Integer> getVisiblePages() {
            List<Integer> pages = new ArrayList<>();
            int totalPages = getTotalPages();
            int start = Math.max(1, page - 2);
            int end = Math.min(totalPages, start + 4);
            start = Math.max(1, end - 4);
            for (int value = start; value <= end; value++) pages.add(value);
            return pages;
        }
    }

    /** B4 · Checklist "cần pha": mọi PREPPED của chi nhánh + tồn/ngưỡng + có công thức hay chưa. */
    public List<com.cafe.model.PrepChecklistRow> getPrepChecklist(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<com.cafe.model.PrepChecklistRow> rows = new ArrayList<>();
            for (BranchInventory bi : biDao.findByBranch(conn, branchId)) {
                if (!"PREPPED".equals(bi.getIngredientType())) continue;
                boolean hasRecipe = !prepRecipeDao.findByPrepped(conn, bi.getIngredientId()).isEmpty();
                rows.add(new com.cafe.model.PrepChecklistRow(bi.getIngredientId(), bi.getIngredientName(),
                        bi.getIngredientUnit(), bi.getQuantityOnHand(), bi.getMinThreshold(), hasRecipe));
            }
            return rows;
        }
    }

    /** B4 · Công thức prep của từng PREPPED (cho preview tiêu hao RAW phía client). */
    public Map<Integer, List<PrepRecipe>> getPrepRecipeMap(List<Integer> preppedIds) throws SQLException {
        Map<Integer, List<PrepRecipe>> map = new java.util.LinkedHashMap<>();
        if (preppedIds == null || preppedIds.isEmpty()) return map;
        try (Connection conn = DBConnection.getConnection()) {
            for (Integer id : preppedIds) map.put(id, prepRecipeDao.findByPrepped(conn, id));
        }
        return map;
    }

    public List<com.cafe.model.WasteLog> getWasteLogs(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<WasteLog> logs = wasteLogDao.findByBranch(conn, branchId);
            enrichWasteCosts(conn, branchId, logs);
            return logs;
        }
    }

    public List<com.cafe.model.WasteLog> getWasteLogs(int branchId, java.time.LocalDateTime fromUtc,
                                                       java.time.LocalDateTime toUtc) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<WasteLog> logs = wasteLogDao.findByBranchBetween(conn, branchId, fromUtc, toUtc);
            enrichWasteCosts(conn, branchId, logs);
            return logs;
        }
    }

    /** Nhật ký hao hụt theo trang — điều kiện tìm/lọc và OFFSET/FETCH đều được xử lý tại database. */
    public WasteLogPage getWasteLogPage(int branchId, java.time.LocalDateTime fromUtc, java.time.LocalDateTime toUtc,
                                        String query, String wasteType, String status, int requestedPage, int pageSize) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            int total = wasteLogDao.countByBranchBetween(conn, branchId, fromUtc, toUtc, query, wasteType, status);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
            int page = Math.max(1, Math.min(requestedPage, totalPages));
            List<WasteLog> logs = wasteLogDao.findPageByBranchBetween(conn, branchId, fromUtc, toUtc,
                    query, wasteType, status, (page - 1) * pageSize, pageSize);
            enrichWasteCosts(conn, branchId, logs);
            return new WasteLogPage(logs, total, page, pageSize);
        }
    }

    public static class WasteLogPage {
        private final List<WasteLog> logs;
        private final int total;
        private final int page;
        private final int pageSize;

        public WasteLogPage(List<WasteLog> logs, int total, int page, int pageSize) {
            this.logs = logs;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<WasteLog> getLogs() { return logs; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return Math.max(1, (int) Math.ceil((double) total / pageSize)); }
        public boolean isHasPrevious() { return page > 1; }
        public boolean isHasNext() { return page < getTotalPages(); }
        public int getStartRow() { return total == 0 ? 0 : (page - 1) * pageSize + 1; }
        public int getEndRow() { return Math.min(page * pageSize, total); }

        /** Tối đa 5 số trang quanh trang hiện tại để pager không phình khi lịch sử dài. */
        public List<Integer> getVisiblePages() {
            List<Integer> pages = new ArrayList<>();
            int totalPages = getTotalPages();
            int start = Math.max(1, page - 2);
            int end = Math.min(totalPages, start + 4);
            start = Math.max(1, end - 4);
            for (int value = start; value <= end; value++) pages.add(value);
            return pages;
        }
    }

    public WasteLog getWasteLog(int branchId, int wasteLogId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            WasteLog log = wasteLogDao.findById(conn, wasteLogId);
            if (log == null || log.getBranchId() != branchId) return null;
            enrichWasteCosts(conn, branchId, List.of(log));
            return log;
        }
    }

    public BigDecimal estimateUnitCost(int branchId, int ingredientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return estimateUnitCost(conn, branchId, ingredientId, new HashSet<>());
        }
    }

    private void enrichWasteCosts(Connection conn, int branchId, List<WasteLog> logs) throws SQLException {
        if (logs == null || logs.isEmpty()) return;
        for (WasteLog log : logs) {
            log.setUnitCost(estimateUnitCost(conn, branchId, log.getIngredientId(), new HashSet<>()));
        }
    }

    private BigDecimal estimateUnitCost(Connection conn, int branchId, int ingredientId, Set<Integer> visiting) throws SQLException {
        BigDecimal direct = detailDao.findLatestUnitCost(conn, branchId, ingredientId);
        if (direct != null) return direct;

        if (!visiting.add(ingredientId)) return null;
        List<PrepRecipe> recipe = prepRecipeDao.findByPrepped(conn, ingredientId);
        if (recipe.isEmpty()) {
            visiting.remove(ingredientId);
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (PrepRecipe pr : recipe) {
            BigDecimal rawCost = estimateUnitCost(conn, branchId, pr.getRawIngredientId(), visiting);
            if (rawCost == null) {
                visiting.remove(ingredientId);
                return null;
            }
            BigDecimal rawPerUnit = pr.getQuantity().divide(pr.getYieldQty(), 6, RoundingMode.HALF_UP);
            total = total.add(rawPerUnit.multiply(rawCost));
        }
        visiting.remove(ingredientId);
        return total.setScale(2, RoundingMode.HALF_UP);
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

    /** Điều chỉnh tồn sau kiểm kê (Manager) — 1 nguyên liệu: ghi StockAdjustment + applyTxn(diff, ADJUST). */
    public void createAdjustment(int branchId, int ingredientId, BigDecimal actualQty, String reason, String unit, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                applyAdjustmentLine(conn, branchId, ingredientId, actualQty, reason, unit, userId);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Điều chỉnh nhiều nguyên liệu cùng lúc (tickbox kiểm kê) — TẤT CẢ trong 1 transaction. */
    public void createAdjustments(int branchId, List<com.cafe.model.StockAdjustment> lines, int userId) throws SQLException {
        if (lines == null || lines.isEmpty()) return;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (com.cafe.model.StockAdjustment a : lines) {
                    applyAdjustmentLine(conn, branchId, a.getIngredientId(), a.getActualQty(), a.getReason(), a.getUnit(), userId);
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Điều chỉnh tồn 1 nguyên liệu TRONG transaction của caller (không tự mở/commit connection).
     * Dùng khi việc chỉnh tồn phải nguyên tử cùng thao tác khác — vd Barista báo hết nguyên liệu
     * ngay tại màn pha chế: ghi kiểm kê về 0 và chặn món phải cùng thành công hoặc cùng rollback.
     */
    public void applyAdjustmentInTx(Connection conn, int branchId, int ingredientId, BigDecimal actualQty,
                                    String reason, String unit, int userId) throws SQLException {
        applyAdjustmentLine(conn, branchId, ingredientId, actualQty, reason, unit, userId);
    }

    /** 1 dòng điều chỉnh trong tx của caller: đọc tồn hệ thống → ghi StockAdjustment → applyTxn chênh lệch. */
    private void applyAdjustmentLine(Connection conn, int branchId, int ingredientId, BigDecimal actualQty,
                                     String reason, String unit, int userId) throws SQLException {
        BigDecimal[] qt = biDao.findQtyAndThreshold(conn, branchId, ingredientId);
        BigDecimal systemQty = (qt == null || qt[0] == null) ? BigDecimal.ZERO : qt[0];
        int adjId = adjustmentDao.insert(conn, branchId, ingredientId, systemQty, actualQty, reason, unit, userId);
        BigDecimal diff = actualQty.subtract(systemQty);
        if (diff.signum() != 0) {
            applyTxn(conn, branchId, ingredientId, diff, TxnType.ADJUST, "StockAdjustment", (long) adjId, userId);
        }
    }

    // ----- Đọc -----
    public List<BranchInventory> getBranchInventory(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return biDao.findByBranch(conn, branchId); }
    }

    public List<BranchInventory> getLowStock(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return biDao.findLowStock(conn, branchId); }
    }

    public List<BranchInventory> getOversoldStock(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return biDao.findOversold(conn, branchId); }
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
