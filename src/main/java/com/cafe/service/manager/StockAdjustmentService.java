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

    private final StockAdjustmentDao dao;
    private final InventoryService inventoryService;

    public StockAdjustmentService() { this(new StockAdjustmentDao(), new InventoryService()); }
    public StockAdjustmentService(StockAdjustmentDao dao, InventoryService inventoryService) {
        this.dao = java.util.Objects.requireNonNull(dao);
        this.inventoryService = java.util.Objects.requireNonNull(inventoryService);
    }

    public List<StockAdjustment> getAdjustmentList(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByBranch(c, branchId); }
    }

    /**
     * Các biên bản kiểm kê gần nhất — dùng để đếm "bao nhiêu LẦN kiểm kê".
     *
     * <p>Trước đây màn Đối soát đếm số DÒNG điều chỉnh là số lần, nên một lượt kiểm kê tick
     * 20 nguyên liệu hiển thị thành "20 lần".
     */
    public List<com.cafe.model.StockCount> getStockCounts(int branchId) throws SQLException {
        return inventoryService.getStockCounts(branchId, 50);
    }

    public void createAdjustment(int branchId, int ingredientId, BigDecimal countedQuantity,
                                 int conversionId, String reason, int userId) throws SQLException {
        inventoryService.createAdjustment(branchId, ingredientId, countedQuantity,
                conversionId, reason, userId);
    }

    /** Điều chỉnh nhiều nguyên liệu cùng lúc (tickbox) — 1 transaction. */
    public void createAdjustments(int branchId, List<StockAdjustment> lines, int userId) throws SQLException {
        inventoryService.createAdjustments(branchId, lines, userId);
    }
}
