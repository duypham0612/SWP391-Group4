package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.math.*;
import java.sql.*;
import java.util.*;

/** Xác nhận receipt và workflow kiểm kê/điều chỉnh tồn base quantity. */
public final class StockAdjustmentWorkflowService {
    private final InventoryRepository repository; private final InventoryLedgerService ledgerService;
    public StockAdjustmentWorkflowService(){this(new InventoryRepository());}
    StockAdjustmentWorkflowService(InventoryRepository repository){this(repository,new InventoryLedgerService(repository));}
    StockAdjustmentWorkflowService(InventoryRepository repository,InventoryLedgerService ledgerService){this.repository=Objects.requireNonNull(repository);this.ledgerService=Objects.requireNonNull(ledgerService);}

    public BigDecimal confirmReceiptStock(Connection conn, List<StockReceiptDetail> details,
                                          String receiptBatchId, int branchId, Integer userId) throws SQLException {
        BigDecimal totalCost = BigDecimal.ZERO;
        for (StockReceiptDetail d : details) {
            if (d.getBaseQuantity() == null || d.getBaseQuantity().signum() <= 0) {
                throw new BusinessException("Dòng phiếu nhập chưa có số lượng quy đổi hợp lệ.");
            }
            ledgerService.applyTxn(conn, branchId, d.getIngredientId(), d.getBaseQuantity(),
                    TxnType.RECEIPT, InventoryReferenceType.STOCK_RECEIPT_LINE,
                    receiptBatchId, userId);
            totalCost = totalCost.add(d.getLineCost());
        }
        return totalCost;
    }

    /**
     * Điều chỉnh tồn 1 nguyên liệu (Manager) — ghi StockAdjustment + ledgerService.applyTxn(diff, ADJUST).
     *
     * <p>Vẫn mở một biên bản kiểm kê 1 dòng: đây là thao tác kiểm kê của Manager, nên phải
     * đếm được như một lần kiểm kê chứ không lẫn vào các dòng lẻ của Barista.
     */
    public void createAdjustment(int branchId, int ingredientId, BigDecimal countedQuantity,
                                 int conversionId, String reason, int userId) throws SQLException {
        validateActualQty(countedQuantity);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String countBatchId = UUID.randomUUID().toString();
                java.time.LocalDateTime countedAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
                applyAdjustmentLine(conn, branchId, countBatchId, countedAt, userId, null,
                        ingredientId, countedQuantity,
                        conversionId, reason, userId);
                conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Điều chỉnh nhiều nguyên liệu cùng lúc (tickbox kiểm kê) — TẤT CẢ trong 1 transaction.
     *
     * <p>MỘT lượt submit = MỘT {@code CountBatchId}, mọi dòng chênh lệch gắn
     * vào đó. N nguyên liệu không trở thành N phiên rời nên vẫn trả lời được
     * "biên bản kiểm kê lúc X gồm những gì, chênh tổng bao nhiêu", và màn Đối soát đếm số
     * dòng thành "N lần kiểm kê".
     */
    public void createAdjustments(int branchId, List<com.cafe.model.StockAdjustment> lines, int userId) throws SQLException {
        if (lines == null || lines.isEmpty()) return;
        java.util.Set<Integer> ingredients = new java.util.HashSet<>();
        for (com.cafe.model.StockAdjustment line : lines) {
            validateActualQty(line.getCountedQuantity());
            if (!ingredients.add(line.getIngredientId())) {
                throw new BusinessException("Một nguyên liệu chỉ được xuất hiện một lần trong biên bản kiểm kê.");
            }
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String countBatchId = UUID.randomUUID().toString();
                java.time.LocalDateTime countedAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
                for (com.cafe.model.StockAdjustment a : lines) {
                    applyAdjustmentLine(conn, branchId, countBatchId, countedAt, userId, null,
                            a.getIngredientId(),
                            a.getCountedQuantity(), a.getUnitChoice(), a.getReason(), userId);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Biên bản kiểm kê gần nhất của chi nhánh (kèm số dòng + tổng chênh, tính từ chi tiết). */
    public List<com.cafe.model.StockCount> getStockCounts(int branchId, int limit) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.stockCountDao.findByBranch(conn, branchId, limit);
        }
    }

    /**
     * Điều chỉnh tồn 1 nguyên liệu TRONG transaction của caller (không tự mở/commit connection).
     * Dùng khi việc chỉnh tồn phải nguyên tử cùng thao tác khác — vd Barista báo hết nguyên liệu
     * ngay tại màn pha chế: ghi kiểm kê về 0 và chặn món phải cùng thành công hoặc cùng rollback.
     */
    public void applyBaseAdjustmentInTx(Connection conn, int branchId, int ingredientId,
                                        BigDecimal actualBaseQty, String reason, int userId) throws SQLException {
        // countBatchId = null: đây KHÔNG phải kiểm kê theo phiên mà là chỉnh lẻ phát sinh
        // từ màn pha chế (báo hết nguyên liệu / đếm lại), không nên đếm vào số lần kiểm kê.
        applyAdjustmentLine(conn, branchId, null, null, null, null,
                ingredientId, actualBaseQty, null, reason, userId);
    }

    /**
     * 1 dòng điều chỉnh trong tx của caller: đọc tồn hệ thống → ghi StockAdjustment → applyTxn chênh lệch.
     *
     * @param countBatchId UUID phiên kiểm kê chứa dòng này; null = điều chỉnh lẻ.
     */
    private void applyAdjustmentLine(Connection conn, int branchId, String countBatchId,
                                     java.time.LocalDateTime countedAt, Integer countedBy, String countNote,
                                     int ingredientId,
                                     BigDecimal countedQuantity, Integer conversionId,
                                     String reason, int userId) throws SQLException {
        validateActualQty(countedQuantity);
        InventoryUnitChoice conversion = conversionId == null
                ? repository.unitDao.findBaseForUse(conn, ingredientId)
                : repository.unitDao.findForUse(conn, conversionId, ingredientId);
        if (conversion == null) {
            throw new BusinessException(
                    "Đơn vị kiểm kê không tồn tại, đã bị tắt hoặc không thuộc nguyên liệu.");
        }
        BigDecimal actualBaseQty = InventoryUnitConverter.toBase(
                countedQuantity, conversion.getFactorToBase());
        BigDecimal[] qt = repository.biDao.findQtyAndThresholdForUpdate(conn, branchId, ingredientId);
        if (qt == null) {
            throw new BusinessException("Nguyên liệu chưa được cấu hình tồn kho tại chi nhánh.");
        }
        BigDecimal systemQty = qt[0] == null ? BigDecimal.ZERO : qt[0];
        int adjId = repository.adjustmentDao.insert(conn, branchId, countBatchId, countedAt, countedBy,
                countNote, ingredientId,
                systemQty, actualBaseQty, countedQuantity,
                conversion.getUnitName(), conversion.getFactorToBase(), reason, userId);
        BigDecimal diff = actualBaseQty.subtract(systemQty);
        if (diff.signum() != 0) {
            ledgerService.applyTxn(conn, branchId, ingredientId, diff, TxnType.ADJUST,
                    InventoryReferenceType.STOCK_ADJUSTMENT, (long) adjId, userId);
        }
    }

    static void validateActualQty(BigDecimal actualQty) {
        if (actualQty == null || actualQty.signum() < 0) {
            throw new BusinessException("Tồn thực tế không được để trống hoặc nhỏ hơn 0.");
        }
    }

    // ----- Đọc -----

}
