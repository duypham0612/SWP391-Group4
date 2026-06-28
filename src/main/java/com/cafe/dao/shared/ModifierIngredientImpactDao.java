package com.cafe.dao.shared;

import com.cafe.model.ModifierIngredientImpact;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ModifierIngredientImpactDao {

    public List<ModifierIngredientImpact> findByOption(Connection conn, int optionId) throws SQLException {
        final String sql =
            "SELECT mii.ImpactId, mii.ModifierOptionId, mii.IngredientId, mii.QtyDelta, " +
            "       i.Name AS IngredientName, i.Unit AS IngredientUnit, i.IngredientType " +
            "FROM catalog.ModifierIngredientImpact mii JOIN catalog.Ingredient i ON mii.IngredientId = i.IngredientId " +
            "WHERE mii.ModifierOptionId = ? ORDER BY i.Name";
        List<ModifierIngredientImpact> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, optionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ModifierIngredientImpact m = new ModifierIngredientImpact();
                    m.setImpactId(rs.getInt("ImpactId"));
                    m.setModifierOptionId(rs.getInt("ModifierOptionId"));
                    m.setIngredientId(rs.getInt("IngredientId"));
                    m.setQtyDelta(rs.getBigDecimal("QtyDelta"));
                    m.setIngredientName(rs.getString("IngredientName"));
                    m.setIngredientUnit(rs.getString("IngredientUnit"));
                    m.setIngredientType(rs.getString("IngredientType"));
                    out.add(m);
                }
            }
        }
        return out;
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
