package com.cafe.service.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.InventoryUnitConverter;
import com.cafe.config.DBConnection;
import com.cafe.dao.manager.StockReceiptDao;
import com.cafe.dao.shared.IngredientUnitDao;
import com.cafe.dao.shared.StockReceiptDetailDao;
import com.cafe.model.InventoryUnitChoice;
import com.cafe.model.StockReceipt;
import com.cafe.model.StockReceiptDetail;
import com.cafe.service.shared.InventoryService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** Workflow phiếu nhập đã được gộp vào inventory.StockReceiptLine. */
public class StockReceiptService {
    private final StockReceiptDao receiptDao;
    private final StockReceiptDetailDao detailDao;
    private final IngredientUnitDao unitDao;
    private final InventoryService inventoryService;

    public StockReceiptService() {
        this(new StockReceiptDao(), new StockReceiptDetailDao(), new IngredientUnitDao(), new InventoryService());
    }

    public StockReceiptService(StockReceiptDao receiptDao, StockReceiptDetailDao detailDao,
                               IngredientUnitDao unitDao, InventoryService inventoryService) {
        this.receiptDao = java.util.Objects.requireNonNull(receiptDao);
        this.detailDao = java.util.Objects.requireNonNull(detailDao);
        this.unitDao = java.util.Objects.requireNonNull(unitDao);
        this.inventoryService = java.util.Objects.requireNonNull(inventoryService);
    }

    public List<StockReceipt> getReceiptList(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return receiptDao.findByBranch(c, branchId); }
    }

    public StockReceipt getReceipt(String batchId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return receiptDao.findById(c, batchId, branchId); }
    }

    public List<StockReceiptDetail> getReceiptDetails(String batchId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return detailDao.findByReceiptAndBranch(c, batchId, branchId);
        }
    }

    /** Schema phẳng không biểu diễn được header rỗng, nên tạo DRAFT cùng dòng đầu tiên. */
    public String createDraftReceipt(StockReceipt receipt, StockReceiptDetail firstLine) throws SQLException {
        validateLine(firstLine.getEnteredQuantity(), firstLine.getUnitCost());
        receipt.setReceiptBatchId(UUID.randomUUID().toString());
        receipt.setDocumentDate(LocalDate.now(ZoneOffset.UTC));
        receipt.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        receipt.setStatus("DRAFT");
        firstLine.setReceiptBatchId(receipt.getReceiptBatchId());
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                applyConversionSnapshot(c, firstLine);
                detailDao.insert(c, receipt, firstLine);
                c.commit();
                return receipt.getReceiptBatchId();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally { c.setAutoCommit(true); }
        }
    }

    public void addReceiptLine(String batchId, int branchId, int ingredientId,
                               BigDecimal qty, BigDecimal unitCost, int unitChoice) throws SQLException {
        validateLine(qty, unitCost);
        StockReceiptDetail line = new StockReceiptDetail();
        line.setReceiptBatchId(batchId);
        line.setIngredientId(ingredientId);
        line.setEnteredQuantity(qty);
        line.setUnitCost(unitCost);
        line.setUnitChoice(unitChoice);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                StockReceipt receipt = requireDraft(c, batchId, branchId);
                applyConversionSnapshot(c, line);
                detailDao.insert(c, receipt, line);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void addReceiptLines(String batchId, int branchId, List<StockReceiptDetail> lines) throws SQLException {
        if (lines == null || lines.isEmpty()) return;
        for (StockReceiptDetail line : lines) validateLine(line.getEnteredQuantity(), line.getUnitCost());
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                StockReceipt receipt = requireDraft(c, batchId, branchId);
                for (StockReceiptDetail line : lines) {
                    line.setReceiptBatchId(batchId);
                    applyConversionSnapshot(c, line);
                    detailDao.insert(c, receipt, line);
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void removeReceiptLine(String batchId, int lineId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireDraft(c, batchId, branchId);
                if (detailDao.deleteDraftLine(c, lineId, batchId, branchId) != 1) {
                    throw new BusinessException("Không thể xoá dòng cuối của phiếu nháp; hãy huỷ cả phiếu nếu không dùng nữa.");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** DRAFT không ghi ledger; chỉ sau khi toàn batch chuyển CONFIRMED mới cộng tồn. */
    public void confirmReceipt(String batchId, int branchId, int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireDraft(c, batchId, branchId);
                List<StockReceiptDetail> details = detailDao.findByReceiptAndBranch(c, batchId, branchId);
                if (details.isEmpty()) throw new BusinessException("Phiếu nhập phải có ít nhất một dòng.");
                if (receiptDao.confirm(c, batchId, branchId) != details.size()) {
                    throw new BusinessException("Phiếu nhập đã được xử lý bởi yêu cầu khác.");
                }
                inventoryService.confirmReceiptStock(c, details, batchId, branchId, userId);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void cancelReceipt(String batchId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireDraft(c, batchId, branchId);
                if (receiptDao.cancel(c, batchId, branchId) <= 0) {
                    throw new BusinessException("Phiếu nhập đã được xử lý bởi yêu cầu khác.");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void cancelManyReceipts(List<String> batchIds, int branchId) throws SQLException {
        if (batchIds == null || batchIds.isEmpty()) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (String batchId : batchIds) {
                    if (batchId == null || batchId.isBlank()) continue;
                    requireDraft(c, batchId, branchId);
                    if (receiptDao.cancel(c, batchId, branchId) <= 0) {
                        throw new BusinessException("Có phiếu nhập đã được xử lý bởi yêu cầu khác.");
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    private StockReceipt requireDraft(Connection c, String batchId, int branchId) throws SQLException {
        if (batchId == null || batchId.isBlank() || batchId.length() > 36) {
            throw new BusinessException("Mã batch phiếu nhập không hợp lệ.");
        }
        StockReceipt receipt = receiptDao.findDraftForUpdate(c, batchId, branchId);
        if (receipt == null) {
            throw new BusinessException("Phiếu nhập không thuộc chi nhánh hiện tại hoặc không còn ở trạng thái nháp.");
        }
        return receipt;
    }

    static void validateLine(BigDecimal quantity, BigDecimal unitCost) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessException("Số lượng nhập phải lớn hơn 0.");
        }
        if (unitCost == null || unitCost.signum() <= 0) {
            throw new BusinessException("Đơn giá nhập phải lớn hơn 0.");
        }
    }

    private void applyConversionSnapshot(Connection c, StockReceiptDetail line) throws SQLException {
        InventoryUnitChoice conversion = unitDao.findForUse(c, line.getUnitChoice(), line.getIngredientId());
        if (conversion == null) {
            throw new BusinessException("Đơn vị quy đổi không tồn tại, đã bị tắt hoặc không thuộc nguyên liệu.");
        }
        BigDecimal base = InventoryUnitConverter.toBase(line.getEnteredQuantity(), conversion.getFactorToBase());
        if (base.signum() <= 0) throw new BusinessException("Số lượng sau quy đổi phải lớn hơn 0.");
        line.setUnitNameAtEntry(conversion.getUnitName());
        line.setFactorToBaseAtEntry(conversion.getFactorToBase());
        line.setBaseQuantity(base);
    }
}
