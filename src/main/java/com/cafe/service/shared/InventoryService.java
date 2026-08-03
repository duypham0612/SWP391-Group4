package com.cafe.service.shared;

import com.cafe.common.InventoryReferenceType;
import com.cafe.common.TxnType;
import com.cafe.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Facade tương thích; chỉ delegate tới các service tồn kho theo use case. */
public class InventoryService {
    private final InventoryLedgerService ledger;
    private final PrepInventoryService prep;
    private final WasteInventoryService waste;
    private final StockAdjustmentWorkflowService adjustments;
    private final InventoryQueryService query;

    public InventoryService() {
        InventoryRepository repository = new InventoryRepository();
        ledger = new InventoryLedgerService(repository);
        waste = new WasteInventoryService(repository, ledger);
        prep = new PrepInventoryService(repository, ledger, waste);
        adjustments = new StockAdjustmentWorkflowService(repository, ledger);
        query = new InventoryQueryService(repository);
    }

    public InventoryService(InventoryLedgerService ledger, PrepInventoryService prep,
                            WasteInventoryService waste, StockAdjustmentWorkflowService adjustments,
                            InventoryQueryService query) {
        this.ledger = Objects.requireNonNull(ledger);
        this.prep = Objects.requireNonNull(prep);
        this.waste = Objects.requireNonNull(waste);
        this.adjustments = Objects.requireNonNull(adjustments);
        this.query = Objects.requireNonNull(query);
    }

    public void applyTxn(Connection connection, int branchId, int ingredientId, BigDecimal delta,
                         TxnType type, InventoryReferenceType referenceType, Long referenceId,
                         Integer userId) throws SQLException {
        ledger.applyTxn(connection, branchId, ingredientId, delta, type, referenceType, referenceId, userId);
    }

    public void deductForOrderItem(Connection connection, int branchId, int orderItemId,
                                   int productId, int quantity, Integer userId) throws SQLException {
        ledger.deductForOrderItem(connection, branchId, orderItemId, productId, quantity, userId);
    }

    public void reserveRemakeForOrderItem(Connection connection, int branchId, int orderItemId,
                                          int productId, int quantity, String reason, int userId) throws SQLException {
        waste.reserveRemakeForOrderItem(connection, branchId, orderItemId, productId, quantity, reason, userId);
    }

    public void releaseRemakeReservation(Connection connection, int branchId, int orderItemId,
                                         Integer userId) throws SQLException {
        waste.releaseRemakeReservation(connection, branchId, orderItemId, userId);
    }

    public int createPrepBatch(int branchId, int ingredientId, BigDecimal quantity,
                               LocalDateTime expiresAt, int userId) throws SQLException {
        return prep.createPrepBatch(branchId, ingredientId, quantity, expiresAt, userId);
    }

    public PrepBatch createSuggestedPrepBatch(int branchId, int ingredientId, BigDecimal quantity,
                                              int userId, String requestId) throws SQLException {
        return prep.createSuggestedPrepBatch(branchId, ingredientId, quantity, userId, requestId);
    }

    public void createPrepBatches(int branchId, List<PrepBatchLine> lines, int userId) throws SQLException { prep.createPrepBatches(branchId, lines, userId); }
    public boolean cancelPrepBatch(int branchId, int batchId, int userId) throws SQLException { return prep.cancelPrepBatch(branchId, batchId, userId); }
    public boolean cancelPrepBatchByManager(int branchId, int batchId, int userId) throws SQLException { return prep.cancelPrepBatchByManager(branchId, batchId, userId); }
    public PrepBatch approvePrepBatch(int branchId, int batchId, int userId) throws SQLException { return prep.approvePrepBatch(branchId, batchId, userId); }
    public PrepBatch rejectPrepBatch(int branchId, int batchId, int userId) throws SQLException { return prep.rejectPrepBatch(branchId, batchId, userId); }
    public void markPrepBatchReviewed(int branchId, int batchId, int userId) throws SQLException { prep.markPrepBatchReviewed(branchId, batchId, userId); }
    public List<PrepBatch> getPendingApprovalBatches(int branchId) throws SQLException { return query.getPendingApprovalBatches(branchId); }
    public List<PrepBatch> getUnreviewedBatches(int branchId) throws SQLException { return query.getUnreviewedBatches(branchId); }
    public void updatePrepBatch(int branchId, int batchId, BigDecimal quantity, int userId) throws SQLException { prep.updatePrepBatch(branchId, batchId, quantity, userId); }
    public long writeOffExpiredPrepBatch(int branchId, int batchId, BigDecimal quantity, int userId) throws SQLException { return prep.writeOffExpiredPrepBatch(branchId, batchId, quantity, userId); }

    public long logWaste(int branchId, int ingredientId, BigDecimal quantity, String type, String reason, int userId) throws SQLException { return waste.logWaste(branchId, ingredientId, quantity, type, reason, userId); }
    public int logWasteLines(int branchId, List<WasteLogLine> lines, int userId) throws SQLException { return waste.logWasteLines(branchId, lines, userId); }
    public int logWasteLines(int branchId, List<WasteLogLine> lines, int userId, String requestId) throws SQLException { return waste.logWasteLines(branchId, lines, userId, requestId); }
    public void updateWaste(int branchId, long entryId, BigDecimal quantity, String type, String reason, int userId) throws SQLException { waste.updateWaste(branchId, entryId, quantity, type, reason, userId); }
    public void voidWaste(int branchId, long entryId, int userId) throws SQLException { waste.voidWaste(branchId, entryId, userId); }
    public List<WasteEventItem> getWasteLogs(int branchId) throws SQLException { return waste.getWasteLogs(branchId); }
    public List<WasteEventItem> getWasteLogs(int branchId, LocalDateTime fromUtc, LocalDateTime toUtc) throws SQLException { return waste.getWasteLogs(branchId, fromUtc, toUtc); }
    public List<WasteEventItem> getWasteLogs(int branchId, LocalDateTime fromUtc, LocalDateTime toUtc, boolean ingredientOnly) throws SQLException { return waste.getWasteLogs(branchId, fromUtc, toUtc, ingredientOnly); }
    public WasteEventItem getWasteLog(int branchId, long entryId) throws SQLException { return waste.getWasteLog(branchId, entryId); }
    public List<WasteEventReview> getOpenWasteReviews(int branchId) throws SQLException { return waste.getOpenWasteReviews(branchId); }
    public List<WasteEventAudit> getWasteCorrections(int branchId, LocalDateTime fromUtc, LocalDateTime toUtc, int limit) throws SQLException { return waste.getWasteCorrections(branchId, fromUtc, toUtc, limit); }
    public List<Ingredient> getActiveWasteIngredients(int branchId) throws SQLException { return query.getActiveWasteIngredients(branchId); }
    public boolean resolveWasteReview(int branchId, long reviewId, int userId, String note) throws SQLException { return waste.resolveWasteReview(branchId, reviewId, userId, note); }
    public BigDecimal estimateUnitCost(int branchId, int ingredientId) throws SQLException { return waste.estimateUnitCost(branchId, ingredientId); }

    public BigDecimal confirmReceiptStock(Connection connection, List<StockReceiptDetail> details,
                                          String receiptBatchId, int branchId, Integer userId) throws SQLException {
        return adjustments.confirmReceiptStock(connection, details, receiptBatchId, branchId, userId);
    }

    public void createAdjustment(int branchId, int ingredientId, BigDecimal quantity,
                                 int conversionId, String reason, int userId) throws SQLException { adjustments.createAdjustment(branchId, ingredientId, quantity, conversionId, reason, userId); }
    public void createAdjustments(int branchId, List<StockAdjustment> lines, int userId) throws SQLException { adjustments.createAdjustments(branchId, lines, userId); }
    public List<StockCount> getStockCounts(int branchId, int limit) throws SQLException { return adjustments.getStockCounts(branchId, limit); }
    public void applyBaseAdjustmentInTx(Connection connection, int branchId, int ingredientId,
                                        BigDecimal actualQuantity, String reason, int userId) throws SQLException { adjustments.applyBaseAdjustmentInTx(connection, branchId, ingredientId, actualQuantity, reason, userId); }
    static void validateActualQty(BigDecimal quantity) { StockAdjustmentWorkflowService.validateActualQty(quantity); }

    public List<PrepBatch> getPrepBatches(int branchId) throws SQLException { return query.getPrepBatches(branchId); }
    public List<PrepBatch> getTodayPrepBatches(int branchId) throws SQLException { return query.getTodayPrepBatches(branchId); }
    public List<PrepBatch> getExpiredActivePrepBatches(int branchId) throws SQLException { return query.getExpiredActivePrepBatches(branchId); }
    public List<PrepChecklistRow> getPrepChecklist(int branchId) throws SQLException { return query.getPrepChecklist(branchId); }
    public Map<Integer, List<Recipe>> getPrepRecipeMap(List<Integer> ids) throws SQLException { return query.getPrepRecipeMap(ids); }
    public List<BranchInventory> getBranchInventory(int branchId) throws SQLException { return query.getBranchInventory(branchId); }
    public List<BranchInventory> getLowStock(int branchId) throws SQLException { return query.getLowStock(branchId); }
    public List<BranchInventory> getOversoldStock(int branchId) throws SQLException { return query.getOversoldStock(branchId); }
    public List<InventoryTransaction> getIngredientLedger(int branchId, int ingredientId) throws SQLException { return query.getIngredientLedger(branchId, ingredientId); }
    public void setMinThreshold(int branchId, int ingredientId, BigDecimal quantity) throws SQLException { query.setMinThreshold(branchId, ingredientId, quantity); }
    public void setPrepPolicy(int branchId, int ingredientId, BigDecimal threshold, BigDecimal target) throws SQLException { query.setPrepPolicy(branchId, ingredientId, threshold, target); }
    public List<PrepBatch> getRecentPrepBatches(int branchId, int limit) throws SQLException { return query.getRecentPrepBatches(branchId, limit); }

    public PrepBatchPage getTodayPrepBatchPage(int branchId, String search, int ingredientId,
                                               String expiry, String status, int page, int pageSize) throws SQLException {
        InventoryQueryService.PrepBatchPage result = query.getTodayPrepBatchPage(branchId, search, ingredientId, expiry, status, page, pageSize);
        return new PrepBatchPage(result.getBatches(), result.getTotal(), result.getPage(), result.getPageSize());
    }

    public WasteLogPage getWasteLogPage(int branchId, LocalDateTime fromUtc, LocalDateTime toUtc,
                                        String search, String wasteType, String status, int page, int pageSize) throws SQLException {
        return getWasteLogPage(branchId, fromUtc, toUtc, search, wasteType, status, false, page, pageSize);
    }

    public WasteLogPage getWasteLogPage(int branchId, LocalDateTime fromUtc, LocalDateTime toUtc,
                                        String search, String wasteType, String status, boolean ingredientOnly,
                                        int page, int pageSize) throws SQLException {
        WasteInventoryService.WasteLogPage result = waste.getWasteLogPage(branchId, fromUtc, toUtc, search, wasteType, status, ingredientOnly, page, pageSize);
        return new WasteLogPage(result.getLogs(), result.getTotal(), result.getPage(), result.getPageSize());
    }

    public static class PrepBatchPage extends InventoryQueryService.PrepBatchPage {
        public PrepBatchPage(List<PrepBatch> batches, int total, int page, int pageSize) { super(batches, total, page, pageSize); }
    }
    public static class WasteLogPage extends WasteInventoryService.WasteLogPage {
        public WasteLogPage(List<WasteEventItem> logs, int total, int page, int pageSize) { super(logs, total, page, pageSize); }
    }
}
