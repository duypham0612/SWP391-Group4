package com.cafe.service.manager;
import com.cafe.service.shared.InventoryService;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.StockAdjustmentDao;
import com.cafe.model.StockAdjustment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** M7 · StockAdjustmentService — điều chỉnh tồn qua InventoryService (ledger). */
public class StockAdjustmentService {

    private final StockAdjustmentDao dao = new StockAdjustmentDao();
    private final InventoryService inventoryService = new InventoryService();

    public List<StockAdjustment> getAdjustmentList(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByBranch(c, branchId); }
    }

    public void createAdjustment(int branchId, int ingredientId, BigDecimal actualQty, String reason, String unit, int userId) throws SQLException {
        inventoryService.createAdjustment(branchId, ingredientId, actualQty, reason, unit, userId);
    }

    /** Điều chỉnh nhiều nguyên liệu cùng lúc (tickbox) — 1 transaction. */
    public void createAdjustments(int branchId, List<StockAdjustment> lines, int userId) throws SQLException {
        inventoryService.createAdjustments(branchId, lines, userId);
    }
}
