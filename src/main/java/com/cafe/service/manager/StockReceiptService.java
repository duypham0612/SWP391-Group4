package com.cafe.service.manager;
import com.cafe.service.shared.InventoryService;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.manager.StockReceiptDao;
import com.cafe.dao.shared.StockReceiptDetailDao;
import com.cafe.dao.shared.IngredientUnitConversionDao;
import com.cafe.common.InventoryUnitConverter;
import com.cafe.model.IngredientUnitConversion;
import com.cafe.model.StockReceipt;
import com.cafe.model.StockReceiptDetail;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * M6 · StockReceiptService (đặc tả mục 5).
 * confirmReceipt → InventoryService.confirmReceiptStock (cộng tồn qua ledger) + set CONFIRMED, CÙNG 1 transaction.
 */
public class StockReceiptService {

    private final StockReceiptDao receiptDao;
    private final StockReceiptDetailDao detailDao;
    private final IngredientUnitConversionDao conversionDao;
    private final InventoryService inventoryService;

    public StockReceiptService() {
        this(new StockReceiptDao(), new StockReceiptDetailDao(), new IngredientUnitConversionDao(),
                new InventoryService());
    }
    public StockReceiptService(StockReceiptDao receiptDao, StockReceiptDetailDao detailDao,
                               IngredientUnitConversionDao conversionDao, InventoryService inventoryService) {
        this.receiptDao = java.util.Objects.requireNonNull(receiptDao);
        this.detailDao = java.util.Objects.requireNonNull(detailDao);
        this.conversionDao = java.util.Objects.requireNonNull(conversionDao);
        this.inventoryService = java.util.Objects.requireNonNull(inventoryService);
    }

    public List<StockReceipt> getReceiptList(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return receiptDao.findByBranch(c, branchId); }
    }

    public StockReceipt getReceipt(int id, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return receiptDao.findById(c, id, branchId);
        }
    }

    public List<StockReceiptDetail> getReceiptDetails(int receiptId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return detailDao.findByReceiptAndBranch(c, receiptId, branchId);
        }
    }

    public int createDraftReceipt(StockReceipt r) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = receiptDao.insertDraft(c, r);
                if (id == 0) {
                    throw new BusinessException(
                            "Người nhận hàng không còn hoạt động tại chi nhánh hiện tại.");
                }
                c.commit();
                return id;
            }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    public void addReceiptLine(int receiptId, int branchId, int ingredientId,
                               BigDecimal qty, BigDecimal unitCost, int unitConversionId) throws SQLException {
        validateLine(qty, unitCost);
        StockReceiptDetail d = new StockReceiptDetail();
        d.setStockReceiptId(receiptId);
        d.setIngredientId(ingredientId);
        d.setEnteredQuantity(qty);
        d.setUnitCost(unitCost);
        d.setIngredientUnitConversionId(unitConversionId);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireDraft(c, receiptId, branchId);
                applyConversionSnapshot(c,d);
                detailDao.insert(c, d);
                c.commit();
            } catch (SQLException | RuntimeException e){ c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Thêm nhiều dòng cùng lúc (tickbox chọn nhiều nguyên liệu) — 1 transaction. */
    public void addReceiptLines(int receiptId, int branchId, List<StockReceiptDetail> lines) throws SQLException {
        if (lines == null || lines.isEmpty()) return;
        for (StockReceiptDetail line : lines) validateLine(line.getEnteredQuantity(), line.getUnitCost());
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireDraft(c, receiptId, branchId);
                for (StockReceiptDetail d : lines) {
                    d.setStockReceiptId(receiptId);
                    applyConversionSnapshot(c,d);
                    detailDao.insert(c, d);
                }
                c.commit();
            } catch (SQLException | RuntimeException e){ c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void removeReceiptLine(int receiptId, int detailId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireDraft(c, receiptId, branchId);
                if (detailDao.deleteDraftLine(c, detailId, receiptId, branchId) != 1) {
                    throw new BusinessException("Dòng phiếu nhập không tồn tại trong chi nhánh hiện tại.");
                }
                c.commit();
            } catch (SQLException | RuntimeException e){ c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Xác nhận phiếu: cộng tồn qua ledger + chốt CONFIRMED, nguyên tử. */
    public void confirmReceipt(int receiptId, int branchId, int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireDraft(c, receiptId, branchId);
                List<StockReceiptDetail> details = detailDao.findByReceiptAndBranch(c, receiptId, branchId);
                if (details.isEmpty()) throw new BusinessException("Phiếu nhập phải có ít nhất một dòng.");
                BigDecimal total = details.stream()
                        .map(StockReceiptDetail::getLineCost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                // Chuyển trạng thái trước khi ghi ledger. Nếu ghi tồn lỗi, transaction
                // rollback cả trạng thái và số dư nên không có phiếu chốt dở.
                if (receiptDao.confirm(c, receiptId, branchId, total) != 1) {
                    throw new BusinessException("Phiếu nhập đã được xử lý bởi yêu cầu khác.");
                }
                inventoryService.confirmReceiptStock(c, details, receiptId, branchId, userId);
                c.commit();
            } catch (SQLException | RuntimeException e){ c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void cancelReceipt(int receiptId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireDraft(c, receiptId, branchId);
                if (receiptDao.cancel(c, receiptId, branchId) != 1) {
                    throw new BusinessException("Phiếu nhập đã được xử lý bởi yêu cầu khác.");
                }
                c.commit();
            } catch (SQLException | RuntimeException e){ c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Huỷ nhiều phiếu cùng lúc — chỉ phiếu DRAFT bị huỷ (DAO guard Status='DRAFT'), 1 transaction. */
    public void cancelManyReceipts(List<Integer> receiptIds, int branchId) throws SQLException {
        if (receiptIds == null || receiptIds.isEmpty()) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (Integer id : receiptIds) {
                    if (id == null) continue;
                    requireDraft(c, id, branchId);
                    if (receiptDao.cancel(c, id, branchId) != 1) {
                        throw new BusinessException("Có phiếu nhập đã được xử lý bởi yêu cầu khác.");
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e){ c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    private StockReceipt requireDraft(Connection c, int receiptId, int branchId) throws SQLException {
        StockReceipt receipt = receiptDao.findDraftForUpdate(c, receiptId, branchId);
        if (receipt == null) {
            throw new BusinessException(
                    "Phiếu nhập không thuộc chi nhánh hiện tại hoặc không còn ở trạng thái nháp.");
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

    private void applyConversionSnapshot(Connection c,StockReceiptDetail d)throws SQLException{
        IngredientUnitConversion conversion=conversionDao.findForUse(
                c,d.getIngredientUnitConversionId(),d.getIngredientId());
        if(conversion==null)throw new BusinessException(
                "Đơn vị quy đổi không tồn tại, đã bị tắt hoặc không thuộc nguyên liệu.");
        BigDecimal base=InventoryUnitConverter.toBase(d.getEnteredQuantity(),conversion.getFactorToBase());
        if(base.signum()<=0)throw new BusinessException("Số lượng sau quy đổi phải lớn hơn 0.");
        d.setUnitNameAtEntry(conversion.getUnitName());
        d.setFactorToBaseAtEntry(conversion.getFactorToBase());
        d.setBaseQuantity(base);
    }
}
