package com.cafe.service.manager;
import com.cafe.service.shared.InventoryService;

import com.cafe.config.DBConnection;
import com.cafe.common.BusinessException;
import com.cafe.dao.manager.StockReceiptDao;
import com.cafe.dao.shared.StockReceiptDetailDao;
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

    private final StockReceiptDao receiptDao = new StockReceiptDao();
    private final StockReceiptDetailDao detailDao = new StockReceiptDetailDao();
    private final InventoryService inventoryService = new InventoryService();

    public List<StockReceipt> getReceiptList(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return receiptDao.findByBranch(c, branchId); }
    }

    public StockReceipt getReceipt(int branchId, int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            StockReceipt r = receiptDao.findById(c, id);
            return r != null && r.getBranchId() == branchId ? r : null;
        }
    }

    public List<StockReceiptDetail> getReceiptDetails(int branchId, int receiptId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            requireReceipt(c, receiptId, branchId, false);
            return detailDao.findByReceipt(c, receiptId);
        }
    }

    public int createDraftReceipt(StockReceipt r) throws SQLException {
        if (r == null || r.getBranchId() <= 0 || r.getReceivedBy() <= 0) throw new BusinessException("Thông tin phiếu nhập không hợp lệ.");
        if (r.getNote() != null && r.getNote().length() > 255) throw new BusinessException("Ghi chú không được vượt quá 255 ký tự.");
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { int id = receiptDao.insertDraft(c, r); c.commit(); return id; }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    public void addReceiptLine(int branchId, int receiptId, int ingredientId, BigDecimal qty, BigDecimal unitCost, String unit) throws SQLException {
        validateLine(ingredientId, qty, unitCost, unit);
        StockReceiptDetail d = new StockReceiptDetail();
        d.setStockReceiptId(receiptId);
        d.setIngredientId(ingredientId);
        d.setQuantity(qty);
        d.setUnitCost(unitCost);
        d.setUnit(unit);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { requireReceipt(c, receiptId, branchId, true); detailDao.insert(c, d); c.commit(); }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    /** Thêm nhiều dòng cùng lúc (tickbox chọn nhiều nguyên liệu) — 1 transaction. */
    public void addReceiptLines(int branchId, int receiptId, List<StockReceiptDetail> lines) throws SQLException {
        if (lines == null || lines.isEmpty()) throw new BusinessException("Vui lòng chọn ít nhất một nguyên liệu và nhập số lượng.");
        for (StockReceiptDetail d : lines) validateLine(d.getIngredientId(), d.getQuantity(), d.getUnitCost(), d.getUnit());
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireReceipt(c, receiptId, branchId, true);
                for (StockReceiptDetail d : lines) {
                    d.setStockReceiptId(receiptId);
                    detailDao.insert(c, d);
                }
                c.commit();
            } catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    public void removeReceiptLine(int branchId, int receiptId, int detailId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { requireReceipt(c, receiptId, branchId, true); detailDao.delete(c, detailId); c.commit(); }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    /** Xác nhận phiếu: cộng tồn qua ledger + chốt CONFIRMED, nguyên tử. */
    public void confirmReceipt(int receiptId, int branchId, int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                requireReceipt(c, receiptId, branchId, true);
                if (detailDao.findByReceipt(c, receiptId).isEmpty()) throw new BusinessException("Phiếu nhập phải có ít nhất một nguyên liệu.");
                BigDecimal total = inventoryService.confirmReceiptStock(c, receiptId, branchId, userId);
                receiptDao.confirm(c, receiptId, total);
                c.commit();
            } catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    public void cancelReceipt(int branchId, int receiptId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { requireReceipt(c, receiptId, branchId, true); receiptDao.cancel(c, receiptId); c.commit(); }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    /** Huỷ nhiều phiếu cùng lúc — chỉ phiếu DRAFT bị huỷ (DAO guard Status='DRAFT'), 1 transaction. */
    public void cancelManyReceipts(int branchId, List<Integer> receiptIds) throws SQLException {
        if (receiptIds == null || receiptIds.isEmpty()) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (Integer id : receiptIds) if (id != null) { requireReceipt(c, id, branchId, true); receiptDao.cancel(c, id); }
                c.commit();
            } catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    private StockReceipt requireReceipt(Connection c, int receiptId, int branchId, boolean draftRequired) throws SQLException {
        StockReceipt r = receiptDao.findById(c, receiptId);
        if (r == null || r.getBranchId() != branchId) throw new BusinessException("Phiếu nhập không thuộc chi nhánh của bạn.");
        if (draftRequired && !"DRAFT".equals(r.getStatus())) throw new BusinessException("Chỉ được thay đổi phiếu đang ở trạng thái nháp.");
        return r;
    }

    static void validateLine(int ingredientId, BigDecimal qty, BigDecimal unitCost, String unit) {
        if (ingredientId <= 0) throw new BusinessException("Nguyên liệu không hợp lệ.");
        if (qty == null || qty.signum() <= 0) throw new BusinessException("Số lượng nhập phải lớn hơn 0.");
        if (unitCost == null || unitCost.signum() < 0) throw new BusinessException("Đơn giá không được âm.");
        if (unit != null && unit.trim().length() > 20) throw new BusinessException("Đơn vị không được vượt quá 20 ký tự.");
    }
}
