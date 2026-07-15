package com.cafe.service.manager;
import com.cafe.service.shared.InventoryService;

import com.cafe.config.DBConnection;
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

    public StockReceipt getReceipt(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return receiptDao.findById(c, id); }
    }

    public List<StockReceiptDetail> getReceiptDetails(int receiptId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return detailDao.findByReceipt(c, receiptId); }
    }

    public int createDraftReceipt(StockReceipt r) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { int id = receiptDao.insertDraft(c, r); c.commit(); return id; }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    public void addReceiptLine(int receiptId, int ingredientId, BigDecimal qty, BigDecimal unitCost, String unit) throws SQLException {
        StockReceiptDetail d = new StockReceiptDetail();
        d.setStockReceiptId(receiptId);
        d.setIngredientId(ingredientId);
        d.setQuantity(qty);
        d.setUnitCost(unitCost);
        d.setUnit(unit);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { detailDao.insert(c, d); c.commit(); }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    /** Thêm nhiều dòng cùng lúc (tickbox chọn nhiều nguyên liệu) — 1 transaction. */
    public void addReceiptLines(int receiptId, List<StockReceiptDetail> lines) throws SQLException {
        if (lines == null || lines.isEmpty()) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (StockReceiptDetail d : lines) {
                    d.setStockReceiptId(receiptId);
                    detailDao.insert(c, d);
                }
                c.commit();
            } catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    public void removeReceiptLine(int detailId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { detailDao.delete(c, detailId); c.commit(); }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    /** Xác nhận phiếu: cộng tồn qua ledger + chốt CONFIRMED, nguyên tử. */
    public void confirmReceipt(int receiptId, int branchId, int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                BigDecimal total = inventoryService.confirmReceiptStock(c, receiptId, branchId, userId);
                receiptDao.confirm(c, receiptId, total);
                c.commit();
            } catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    public void cancelReceipt(int receiptId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { receiptDao.cancel(c, receiptId); c.commit(); }
            catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }

    /** Huỷ nhiều phiếu cùng lúc — chỉ phiếu DRAFT bị huỷ (DAO guard Status='DRAFT'), 1 transaction. */
    public void cancelManyReceipts(List<Integer> receiptIds) throws SQLException {
        if (receiptIds == null || receiptIds.isEmpty()) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (Integer id : receiptIds) if (id != null) receiptDao.cancel(c, id);
                c.commit();
            } catch (SQLException e){ c.rollback(); throw e; } finally { c.setAutoCommit(true); }
        }
    }
}
