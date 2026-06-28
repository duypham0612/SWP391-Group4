package com.cafe.dao.shared;

import com.cafe.model.StockAdjustment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class StockAdjustmentDao {

    public List<StockAdjustment> findByBranch(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT a.StockAdjustmentId, a.BranchId, a.IngredientId, a.SystemQty, a.ActualQty, a.DiffQty, a.Reason, " +
            "       a.AdjustedBy, a.AdjustedAt, i.Name AS IngredientName, i.Unit AS IngredientUnit, u.FullName AS AdjustedByName " +
            "FROM inventory.StockAdjustment a " +
            "JOIN catalog.Ingredient i ON a.IngredientId = i.IngredientId " +
            "LEFT JOIN iam.[User] u ON a.AdjustedBy = u.UserId " +
            "WHERE a.BranchId = ? ORDER BY a.StockAdjustmentId DESC";
        List<StockAdjustment> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockAdjustment a = new StockAdjustment();
                    a.setStockAdjustmentId(rs.getInt("StockAdjustmentId"));
                    a.setBranchId(rs.getInt("BranchId"));
                    a.setIngredientId(rs.getInt("IngredientId"));
                    a.setSystemQty(rs.getBigDecimal("SystemQty"));
                    a.setActualQty(rs.getBigDecimal("ActualQty"));
                    a.setDiffQty(rs.getBigDecimal("DiffQty"));
                    a.setReason(rs.getString("Reason"));
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

    /** Chèn dòng điều chỉnh, trả về id (DiffQty do DB tự tính). */
    public int insert(Connection conn, int branchId, int ingredientId, java.math.BigDecimal systemQty,
                      java.math.BigDecimal actualQty, String reason, int adjustedBy) throws SQLException {
        final String sql = "INSERT INTO inventory.StockAdjustment(BranchId, IngredientId, SystemQty, ActualQty, Reason, AdjustedBy) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, ingredientId);
            ps.setBigDecimal(3, systemQty);
            ps.setBigDecimal(4, actualQty);
            ps.setString(5, reason);
            ps.setInt(6, adjustedBy);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }
}
