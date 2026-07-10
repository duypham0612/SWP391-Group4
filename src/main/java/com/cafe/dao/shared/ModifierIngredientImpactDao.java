package com.cafe.dao.shared;

import com.cafe.model.ModifierIngredientImpact;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ModifierIngredientImpactDao {

    private static final String SELECT =
        "SELECT mii.ImpactId, mii.ModifierOptionId, mii.IngredientId, mii.QtyDelta, " +
        "       i.Name AS IngredientName, i.Unit AS IngredientUnit, i.IngredientType " +
        "FROM catalog.ModifierIngredientImpact mii JOIN catalog.Ingredient i ON mii.IngredientId = i.IngredientId ";

    public List<ModifierIngredientImpact> findByOption(Connection conn, int optionId) throws SQLException {
        List<ModifierIngredientImpact> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE mii.ModifierOptionId = ? ORDER BY i.Name")) {
            ps.setInt(1, optionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /** Mọi impact của MỌI option thuộc 1 group — lấy 1 lần cho workspace. */
    public List<ModifierIngredientImpact> findByGroup(Connection conn, int groupId) throws SQLException {
        final String sql = SELECT +
            "JOIN catalog.ModifierOption o ON o.ModifierOptionId = mii.ModifierOptionId " +
            "WHERE o.ModifierGroupId = ? ORDER BY mii.ModifierOptionId, i.Name";
        List<ModifierIngredientImpact> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private ModifierIngredientImpact map(ResultSet rs) throws SQLException {
        ModifierIngredientImpact m = new ModifierIngredientImpact();
        m.setImpactId(rs.getInt("ImpactId"));
        m.setModifierOptionId(rs.getInt("ModifierOptionId"));
        m.setIngredientId(rs.getInt("IngredientId"));
        m.setQtyDelta(rs.getBigDecimal("QtyDelta"));
        m.setIngredientName(rs.getString("IngredientName"));
        m.setIngredientUnit(rs.getString("IngredientUnit"));
        m.setIngredientType(rs.getString("IngredientType"));
        return m;
    }

    /** Đã có định mức cho cặp (option, ingredient) chưa — để chặn trùng UQ_MII. */
    public boolean exists(Connection conn, int optionId, int ingredientId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM catalog.ModifierIngredientImpact WHERE ModifierOptionId = ? AND IngredientId = ?")) {
            ps.setInt(1, optionId);
            ps.setInt(2, ingredientId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public void insert(Connection conn, ModifierIngredientImpact m) throws SQLException {
        final String sql = "INSERT INTO catalog.ModifierIngredientImpact(ModifierOptionId, IngredientId, QtyDelta) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, m.getModifierOptionId());
            ps.setInt(2, m.getIngredientId());
            ps.setBigDecimal(3, m.getQtyDelta());
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, int impactId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM catalog.ModifierIngredientImpact WHERE ImpactId = ?")) {
            ps.setInt(1, impactId);
            ps.executeUpdate();
        }
    }

    public void deleteByOption(Connection conn, int optionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM catalog.ModifierIngredientImpact WHERE ModifierOptionId = ?")) {
            ps.setInt(1, optionId);
            ps.executeUpdate();
        }
    }
}
