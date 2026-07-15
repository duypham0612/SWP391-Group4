package com.cafe.dao.shared;

import com.cafe.model.InventoryTransaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Sổ cái tồn kho — chỉ INSERT (append-only) + đọc. */
public class InventoryTransactionDao {

    public void insert(Connection conn, int branchId, int ingredientId, BigDecimal changeQty,
                       String txnType, String refTable, Long refId, Integer createdBy) throws SQLException {
        final String sql = "INSERT INTO inventory.InventoryTransaction" +
                "(BranchId, IngredientId, ChangeQty, TxnType, RefTable, RefId, CreatedBy) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, ingredientId);
            ps.setBigDecimal(3, changeQty);
            ps.setString(4, txnType);
            if (refTable == null) ps.setNull(5, Types.VARCHAR); else ps.setString(5, refTable);
            if (refId == null) ps.setNull(6, Types.BIGINT); else ps.setLong(6, refId);
            if (createdBy == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, createdBy);
            ps.executeUpdate();
        }
    }

    public List<InventoryTransaction> findByBranchIngredient(Connection conn, int branchId, int ingredientId) throws SQLException {
        final String sql =
            "SELECT t.InventoryTxnId, t.BranchId, t.IngredientId, t.ChangeQty, t.TxnType, t.RefTable, t.RefId, " +
            "       t.CreatedBy, t.CreatedAt, i.Name AS IngredientName, i.Unit AS IngredientUnit, u.FullName AS CreatedByName " +
            "FROM inventory.InventoryTransaction t " +
            "JOIN catalog.Ingredient i ON t.IngredientId = i.IngredientId " +
            "LEFT JOIN iam.[User] u ON t.CreatedBy = u.UserId " +
            "WHERE t.BranchId = ? AND t.IngredientId = ? ORDER BY t.InventoryTxnId DESC";
        List<InventoryTransaction> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, ingredientId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    private InventoryTransaction map(ResultSet rs) throws SQLException {
        InventoryTransaction t = new InventoryTransaction();
        t.setInventoryTxnId(rs.getLong("InventoryTxnId"));
        t.setBranchId(rs.getInt("BranchId"));
        t.setIngredientId(rs.getInt("IngredientId"));
        t.setChangeQty(rs.getBigDecimal("ChangeQty"));
        t.setTxnType(rs.getString("TxnType"));
        t.setRefTable(rs.getString("RefTable"));
        long rid = rs.getLong("RefId");
        t.setRefId(rs.wasNull() ? null : rid);
        int cb = rs.getInt("CreatedBy");
        t.setCreatedBy(rs.wasNull() ? null : cb);
        Timestamp ts = rs.getTimestamp("CreatedAt");
        t.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
        t.setIngredientName(rs.getString("IngredientName"));
        t.setIngredientUnit(rs.getString("IngredientUnit"));
        t.setCreatedByName(rs.getString("CreatedByName"));
        return t;
    }
}
