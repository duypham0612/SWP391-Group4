package com.cafe.dao.inventory;

import com.cafe.model.StockAdjustment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class StockAdjustmentDao {

    public List<StockAdjustment> findByBranch(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT a.StockAdjustmentId,a.BranchId,a.CountBatchId,a.CountedAt,a.CountedBy,a.CountNote, " +
            "       a.IngredientId,a.SystemBaseQty,a.ActualBaseQty,a.DiffQty,a.Reason, " +
            "       a.CountedQuantity,a.UnitNameAtCount,a.FactorToBaseAtCount, " +
            "       a.AdjustedBy, a.AdjustedAt, i.Name AS IngredientName, i.Unit AS IngredientUnit, u.FullName AS AdjustedByName " +
            "FROM inventory.StockAdjustment a " +
            "JOIN catalog.Ingredient i ON a.IngredientId = i.IngredientId " +
            "LEFT JOIN iam.UserAccount u ON a.AdjustedBy = u.UserId " +
            "WHERE a.BranchId = ? ORDER BY a.StockAdjustmentId DESC";
        List<StockAdjustment> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockAdjustment a = new StockAdjustment();
                    a.setStockAdjustmentId(rs.getInt("StockAdjustmentId"));
                    a.setBranchId(rs.getInt("BranchId"));
                    a.setCountBatchId(rs.getString("CountBatchId"));
                    Timestamp countedAt = rs.getTimestamp("CountedAt");
                    a.setCountedAt(countedAt == null ? null : countedAt.toLocalDateTime());
                    int countedBy = rs.getInt("CountedBy");
                    a.setCountedBy(rs.wasNull() ? null : countedBy);
                    a.setCountNote(rs.getString("CountNote"));
                    a.setIngredientId(rs.getInt("IngredientId"));
                    a.setSystemBaseQty(rs.getBigDecimal("SystemBaseQty"));
                    a.setActualBaseQty(rs.getBigDecimal("ActualBaseQty"));
                    a.setDiffQty(rs.getBigDecimal("DiffQty"));
                    a.setReason(rs.getString("Reason"));
                    a.setCountedQuantity(rs.getBigDecimal("CountedQuantity"));
                    a.setUnitNameAtCount(rs.getString("UnitNameAtCount"));
                    a.setFactorToBaseAtCount(rs.getBigDecimal("FactorToBaseAtCount"));
                    a.setAdjustedBy(rs.getInt("AdjustedBy"));
                    Timestamp ts = rs.getTimestamp("AdjustedAt");
                    a.setAdjustedAt(ts == null ? null : ts.toLocalDateTime());
                    a.setIngredientName(rs.getString("IngredientName"));
                    a.setIngredientUnit(rs.getString("IngredientUnit"));
                    a.setAdjustedByName(rs.getString("AdjustedByName"));
                    out.add(a);
                }
            }
        }
        return out;
    }

    /**
     * Chèn dòng điều chỉnh, trả về id (DiffQty do DB tự tính).
     *
     * @param countBatchId UUID phiên kiểm kê chứa dòng này; NULL cho điều chỉnh lẻ không
     *                     thuộc biên bản nào (Barista báo hết nguyên liệu / đếm lại ở màn pha).
     */
    public int insert(Connection conn, int branchId, String countBatchId, java.time.LocalDateTime countedAt,
                      Integer countedBy, String countNote, int ingredientId,
                      java.math.BigDecimal systemBaseQty, java.math.BigDecimal actualBaseQty,
                      java.math.BigDecimal countedQuantity, String unitNameAtCount,
                      java.math.BigDecimal factorToBaseAtCount, String reason, int adjustedBy) throws SQLException {
        final String sql = "INSERT INTO inventory.StockAdjustment(BranchId,CountBatchId,CountedAt,CountedBy,CountNote,IngredientId,"
                + "SystemBaseQty,ActualBaseQty,CountedQuantity,UnitNameAtCount,"
                + "FactorToBaseAtCount,Reason,AdjustedBy) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            if (countBatchId == null) ps.setNull(2, Types.VARCHAR); else ps.setString(2, countBatchId);
            if (countedAt == null) ps.setNull(3, Types.TIMESTAMP); else ps.setTimestamp(3, Timestamp.valueOf(countedAt));
            if (countedBy == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, countedBy);
            if (countNote == null || countNote.isBlank()) ps.setNull(5, Types.NVARCHAR); else ps.setString(5, countNote);
            ps.setInt(6, ingredientId);
            ps.setBigDecimal(7, systemBaseQty);
            ps.setBigDecimal(8, actualBaseQty);
            ps.setBigDecimal(9, countedQuantity);
            ps.setString(10, unitNameAtCount);
            ps.setBigDecimal(11, factorToBaseAtCount);
            if (reason == null || reason.isBlank()) ps.setNull(12, Types.NVARCHAR); else ps.setString(12, reason);
            ps.setInt(13, adjustedBy);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }
}
