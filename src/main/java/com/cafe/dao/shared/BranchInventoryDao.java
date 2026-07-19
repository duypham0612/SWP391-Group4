package com.cafe.dao.shared;

import com.cafe.model.BranchInventory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Số dư tồn theo chi nhánh. CHỈ được sửa qua applyDelta (gọi từ InventoryService). */
public class BranchInventoryDao {

    public List<BranchInventory> findByBranch(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT bi.BranchId, bi.IngredientId, bi.QuantityOnHand, bi.MinThreshold, " +
            "       i.Name AS IngredientName, i.Unit AS IngredientUnit, i.IngredientType " +
            "FROM inventory.BranchInventory bi JOIN catalog.Ingredient i ON bi.IngredientId = i.IngredientId " +
            "WHERE bi.BranchId = ? ORDER BY i.IngredientType, i.Name";
        List<BranchInventory> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public List<BranchInventory> findLowStock(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT bi.BranchId, bi.IngredientId, bi.QuantityOnHand, bi.MinThreshold, " +
            "       i.Name AS IngredientName, i.Unit AS IngredientUnit, i.IngredientType " +
            "FROM inventory.BranchInventory bi JOIN catalog.Ingredient i ON bi.IngredientId = i.IngredientId " +
            "WHERE bi.BranchId = ? AND bi.QuantityOnHand <= bi.MinThreshold ORDER BY i.Name";
        List<BranchInventory> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Tồn âm hiện tại. Outbox stock.oversold là audit trail; màn quản lý đọc số dư cache để phản ánh trạng thái đang còn lệch. */
    public List<BranchInventory> findOversold(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT bi.BranchId, bi.IngredientId, bi.QuantityOnHand, bi.MinThreshold, " +
            "       i.Name AS IngredientName, i.Unit AS IngredientUnit, i.IngredientType " +
            "FROM inventory.BranchInventory bi JOIN catalog.Ingredient i ON bi.IngredientId = i.IngredientId " +
            "WHERE bi.BranchId = ? AND bi.QuantityOnHand < 0 ORDER BY i.Name";
        List<BranchInventory> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Trả về [quantityOnHand, minThreshold] hoặc null nếu chưa có dòng. */
    public BigDecimal[] findQtyAndThreshold(Connection conn, int branchId, int ingredientId) throws SQLException {
        final String sql = "SELECT QuantityOnHand, MinThreshold FROM inventory.BranchInventory WHERE BranchId=? AND IngredientId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new BigDecimal[]{ rs.getBigDecimal(1), rs.getBigDecimal(2) };
                return null;
            }
        }
    }

    /** Cộng/trừ tồn (upsert). KHÔNG gọi trực tiếp từ controller — đi qua InventoryService.applyTxn. */
    public void applyDelta(Connection conn, int branchId, int ingredientId, BigDecimal delta) throws SQLException {
        final String upd = "UPDATE inventory.BranchInventory SET QuantityOnHand = QuantityOnHand + ?, " +
                "UpdatedAt = SYSUTCDATETIME() WHERE BranchId=? AND IngredientId=?";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            ps.setBigDecimal(1, delta);
            ps.setInt(2, branchId);
            ps.setInt(3, ingredientId);
            if (ps.executeUpdate() == 0) {
                final String ins = "INSERT INTO inventory.BranchInventory(BranchId, IngredientId, QuantityOnHand, MinThreshold) VALUES (?,?,?,0)";
                try (PreparedStatement ins2 = conn.prepareStatement(ins)) {
                    ins2.setInt(1, branchId);
                    ins2.setInt(2, ingredientId);
                    ins2.setBigDecimal(3, delta);
                    ins2.executeUpdate();
                }
            }
        }
    }

    public void updateThreshold(Connection conn, int branchId, int ingredientId, BigDecimal threshold) throws SQLException {
        final String upd = "UPDATE inventory.BranchInventory SET MinThreshold=? WHERE BranchId=? AND IngredientId=?";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            ps.setBigDecimal(1, threshold);
            ps.setInt(2, branchId);
            ps.setInt(3, ingredientId);
            if (ps.executeUpdate() == 0) {
                final String ins = "INSERT INTO inventory.BranchInventory(BranchId, IngredientId, QuantityOnHand, MinThreshold) VALUES (?,?,0,?)";
                try (PreparedStatement ins2 = conn.prepareStatement(ins)) {
                    ins2.setInt(1, branchId);
                    ins2.setInt(2, ingredientId);
                    ins2.setBigDecimal(3, threshold);
                    ins2.executeUpdate();
                }
            }
        }
    }

    private BranchInventory map(ResultSet rs) throws SQLException {
        BranchInventory bi = new BranchInventory();
        bi.setBranchId(rs.getInt("BranchId"));
        bi.setIngredientId(rs.getInt("IngredientId"));
        bi.setQuantityOnHand(rs.getBigDecimal("QuantityOnHand"));
        bi.setMinThreshold(rs.getBigDecimal("MinThreshold"));
        bi.setIngredientName(rs.getString("IngredientName"));
        bi.setIngredientUnit(rs.getString("IngredientUnit"));
        bi.setIngredientType(rs.getString("IngredientType"));
        return bi;
    }
}
