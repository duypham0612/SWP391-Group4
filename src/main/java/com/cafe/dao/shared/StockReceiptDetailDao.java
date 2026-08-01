package com.cafe.dao.shared;

import com.cafe.model.StockReceiptDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockReceiptDetailDao {

    /** Chỉ trả detail khi header thuộc đúng chi nhánh caller. */
    public List<StockReceiptDetail> findByReceiptAndBranch(Connection conn, int receiptId, int branchId)
            throws SQLException {
        final String sql =
            "SELECT d.StockReceiptDetailId,d.StockReceiptId,d.IngredientId,d.IngredientUnitConversionId," +
            "       d.EnteredQuantity,d.BaseQuantity,d.UnitCost,d.UnitNameAtEntry,d.FactorToBaseAtEntry, " +
            "       i.Name AS IngredientName, i.Unit AS IngredientUnit " +
            "FROM inventory.StockReceiptDetail d " +
            "JOIN inventory.StockReceipt r ON r.StockReceiptId=d.StockReceiptId " +
            "JOIN catalog.Ingredient i ON d.IngredientId=i.IngredientId " +
            "WHERE d.StockReceiptId=? AND r.BranchId=? ORDER BY d.StockReceiptDetailId";
        List<StockReceiptDetail> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, receiptId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public void insert(Connection conn, StockReceiptDetail d) throws SQLException {
        final String sql = "INSERT INTO inventory.StockReceiptDetail(StockReceiptId,IngredientId,"
                + "IngredientUnitConversionId,EnteredQuantity,UnitCost,UnitNameAtEntry,FactorToBaseAtEntry) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d.getStockReceiptId());
            ps.setInt(2, d.getIngredientId());
            ps.setInt(3, d.getIngredientUnitConversionId());
            ps.setBigDecimal(4, d.getEnteredQuantity());
            ps.setBigDecimal(5, d.getUnitCost());
            ps.setString(6, d.getUnitNameAtEntry());
            ps.setBigDecimal(7, d.getFactorToBaseAtEntry());
            ps.executeUpdate();
        }
    }

    /** Xóa đúng dòng của phiếu nháp thuộc chi nhánh; trả số dòng bị xóa. */
    public int deleteDraftLine(Connection conn, int detailId, int receiptId, int branchId)
            throws SQLException {
        final String sql = "DELETE d FROM inventory.StockReceiptDetail d " +
                "JOIN inventory.StockReceipt r ON r.StockReceiptId=d.StockReceiptId " +
                "WHERE d.StockReceiptDetailId=? AND d.StockReceiptId=? " +
                "AND r.BranchId=? AND r.Status='DRAFT'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detailId);
            ps.setInt(2, receiptId);
            ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }

    public java.math.BigDecimal findLatestUnitCost(Connection conn, int branchId, int ingredientId) throws SQLException {
        final String sql =
            "SELECT TOP 1 CONVERT(DECIMAL(18,6),d.UnitCost/d.FactorToBaseAtEntry) AS BaseUnitCost " +
            "FROM inventory.StockReceiptDetail d " +
            "JOIN inventory.StockReceipt r ON r.StockReceiptId = d.StockReceiptId " +
            "WHERE r.BranchId = ? AND d.IngredientId = ? AND r.Status = 'CONFIRMED' AND d.UnitCost > 0 " +
            "ORDER BY r.DocumentDate DESC, d.StockReceiptDetailId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("BaseUnitCost") : null;
            }
        }
    }

    private StockReceiptDetail map(ResultSet rs) throws SQLException {
        StockReceiptDetail d = new StockReceiptDetail();
        d.setStockReceiptDetailId(rs.getInt("StockReceiptDetailId"));
        d.setStockReceiptId(rs.getInt("StockReceiptId"));
        d.setIngredientId(rs.getInt("IngredientId"));
        d.setIngredientUnitConversionId(rs.getInt("IngredientUnitConversionId"));
        d.setEnteredQuantity(rs.getBigDecimal("EnteredQuantity"));
        d.setBaseQuantity(rs.getBigDecimal("BaseQuantity"));
        d.setUnitCost(rs.getBigDecimal("UnitCost"));
        d.setUnitNameAtEntry(rs.getString("UnitNameAtEntry"));
        d.setFactorToBaseAtEntry(rs.getBigDecimal("FactorToBaseAtEntry"));
        d.setIngredientName(rs.getString("IngredientName"));
        d.setIngredientUnit(rs.getString("IngredientUnit"));
        return d;
    }
}
