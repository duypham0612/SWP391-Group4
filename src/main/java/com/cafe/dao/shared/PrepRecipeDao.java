package com.cafe.dao.shared;

import com.cafe.model.PrepRecipe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PrepRecipeDao {

    public List<PrepRecipe> findByPrepped(Connection conn, int preppedIngredientId) throws SQLException {
        final String sql =
            "SELECT pr.PrepRecipeId, pr.PreppedIngredientId, pr.RawIngredientId, pr.Quantity, pr.YieldQty, " +
            "       i.Name AS RawName, i.Unit AS RawUnit " +
            "FROM catalog.PrepRecipe pr JOIN catalog.Ingredient i ON pr.RawIngredientId = i.IngredientId " +
            "WHERE pr.PreppedIngredientId = ? ORDER BY i.Name";
        List<PrepRecipe> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, preppedIngredientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrepRecipe pr = new PrepRecipe();
                    pr.setPrepRecipeId(rs.getInt("PrepRecipeId"));
                    pr.setPreppedIngredientId(rs.getInt("PreppedIngredientId"));
                    pr.setRawIngredientId(rs.getInt("RawIngredientId"));
                    pr.setQuantity(rs.getBigDecimal("Quantity"));
                    pr.setYieldQty(rs.getBigDecimal("YieldQty"));
                    pr.setRawIngredientName(rs.getString("RawName"));
                    pr.setRawIngredientUnit(rs.getString("RawUnit"));
                    out.add(pr);
                }
            }
        }
        return out;
    }

    public void insert(Connection conn, PrepRecipe pr) throws SQLException {
        final String sql = "INSERT INTO catalog.PrepRecipe(PreppedIngredientId, RawIngredientId, Quantity, YieldQty) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pr.getPreppedIngredientId());
            ps.setInt(2, pr.getRawIngredientId());
            ps.setBigDecimal(3, pr.getQuantity());
            ps.setBigDecimal(4, pr.getYieldQty());
            ps.executeUpdate();
        }
    }

    public int updateQuantity(Connection conn, int prepRecipeId, int preppedIngredientId,
                              java.math.BigDecimal quantity) throws SQLException {
        final String sql =
                "UPDATE catalog.PrepRecipe SET Quantity = ? " +
                "WHERE PrepRecipeId = ? AND PreppedIngredientId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setInt(2, prepRecipeId);
            ps.setInt(3, preppedIngredientId);
            return ps.executeUpdate();
        }
    }

    public int delete(Connection conn, int prepRecipeId, int preppedIngredientId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM catalog.PrepRecipe WHERE PrepRecipeId = ? AND PreppedIngredientId = ?")) {
            ps.setInt(1, prepRecipeId);
            ps.setInt(2, preppedIngredientId);
            return ps.executeUpdate();
        }
    }
}
