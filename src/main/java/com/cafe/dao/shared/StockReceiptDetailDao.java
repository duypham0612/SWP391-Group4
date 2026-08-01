package com.cafe.dao.shared;

import com.cafe.model.StockReceipt;
import com.cafe.model.StockReceiptDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** DAO dòng inventory.StockReceiptLine; tên class được giữ để giảm phạm vi thay đổi. */
public class StockReceiptDetailDao {

    public List<StockReceiptDetail> findByReceiptAndBranch(Connection conn, String batchId, int branchId)
            throws SQLException {
        final String sql =
            "SELECT r.StockReceiptLineId,r.ReceiptBatchId,r.IngredientId,r.EnteredQuantity,r.BaseQuantity," +
            "       r.UnitCost,r.UnitNameAtEntry,r.FactorToBaseAtEntry," +
            "       i.Name AS IngredientName,i.Unit AS IngredientUnit " +
            "FROM inventory.StockReceiptLine r " +
            "JOIN catalog.Ingredient i ON r.IngredientId=i.IngredientId " +
            "WHERE r.ReceiptBatchId=? AND r.BranchId=? ORDER BY r.StockReceiptLineId";
        List<StockReceiptDetail> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public int insert(Connection conn, StockReceipt receipt, StockReceiptDetail line) throws SQLException {
        final String sql = "INSERT INTO inventory.StockReceiptLine(ReceiptBatchId,BranchId,SupplierId,ReceivedBy,"
                + "DocumentDate,Status,Note,CreatedAt,IngredientId,UnitCost,EnteredQuantity,UnitNameAtEntry,FactorToBaseAtEntry) "
                + "VALUES (?,?,?,?,?,'DRAFT',?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receipt.getReceiptBatchId());
            ps.setInt(2, receipt.getBranchId());
            if (receipt.getSupplierId() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, receipt.getSupplierId());
            ps.setInt(4, receipt.getReceivedBy());
            ps.setDate(5, java.sql.Date.valueOf(receipt.getDocumentDate()));
            if (receipt.getNote() == null || receipt.getNote().isBlank()) ps.setNull(6, Types.NVARCHAR); else ps.setString(6, receipt.getNote());
            ps.setTimestamp(7, Timestamp.valueOf(receipt.getCreatedAt()));
            ps.setInt(8, line.getIngredientId());
            ps.setBigDecimal(9, line.getUnitCost());
            ps.setBigDecimal(10, line.getEnteredQuantity());
            ps.setString(11, line.getUnitNameAtEntry());
            ps.setBigDecimal(12, line.getFactorToBaseAtEntry());
            return ps.executeUpdate();
        }
    }

    /** Không xóa dòng cuối vì schema phẳng không thể giữ một batch rỗng. */
    public int deleteDraftLine(Connection conn, int lineId, String batchId, int branchId)
            throws SQLException {
        final String sql = "DELETE r FROM inventory.StockReceiptLine r "
                + "WHERE r.StockReceiptLineId=? AND r.ReceiptBatchId=? AND r.BranchId=? AND r.Status='DRAFT' "
                + "AND EXISTS(SELECT 1 FROM inventory.StockReceiptLine other "
                + "           WHERE other.ReceiptBatchId=r.ReceiptBatchId AND other.StockReceiptLineId<>r.StockReceiptLineId)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lineId);
            ps.setString(2, batchId);
            ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }

    public java.math.BigDecimal findLatestUnitCost(Connection conn, int branchId, int ingredientId) throws SQLException {
        final String sql =
            "SELECT TOP 1 CONVERT(DECIMAL(18,6),r.UnitCost/r.FactorToBaseAtEntry) AS BaseUnitCost " +
            "FROM inventory.StockReceiptLine r " +
            "WHERE r.BranchId=? AND r.IngredientId=? AND r.Status='CONFIRMED' AND r.UnitCost>0 " +
            "ORDER BY r.DocumentDate DESC,r.StockReceiptLineId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, ingredientId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getBigDecimal("BaseUnitCost") : null; }
        }
    }

    private static StockReceiptDetail map(ResultSet rs) throws SQLException {
        StockReceiptDetail d = new StockReceiptDetail();
        d.setStockReceiptLineId(rs.getInt("StockReceiptLineId"));
        d.setReceiptBatchId(rs.getString("ReceiptBatchId"));
        d.setIngredientId(rs.getInt("IngredientId"));
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
