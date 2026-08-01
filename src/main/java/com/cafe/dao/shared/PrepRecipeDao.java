package com.cafe.dao.shared;

import com.cafe.model.PrepRecipe;
import com.cafe.model.PrepRecipeIngredient;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrepRecipeDao {

    private static final String SELECT =
        "SELECT recipe.PrepRecipeId, recipe.PreppedIngredientId, recipe.YieldQty, " +
        "       recipe.CreatedAt, recipe.UpdatedAt, line.PrepRecipeIngredientId, " +
        "       line.RawIngredientId, line.Quantity, ingredient.Name AS RawName, " +
        "       ingredient.Unit AS RawUnit " +
        "FROM catalog.PrepRecipe recipe " +
        "LEFT JOIN catalog.PrepRecipeIngredient line ON line.PrepRecipeId=recipe.PrepRecipeId " +
        "LEFT JOIN catalog.Ingredient ingredient ON ingredient.IngredientId=line.RawIngredientId ";

    public PrepRecipe findByPrepped(Connection conn, int preppedIngredientId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE recipe.PreppedIngredientId=? ORDER BY ingredient.Name")) {
            ps.setInt(1, preppedIngredientId);
            try (ResultSet rs = ps.executeQuery()) {
                PrepRecipe recipe = null;
                while (rs.next()) {
                    if (recipe == null) recipe = mapHeader(rs);
                    addLine(recipe, rs);
                }
                return recipe;
            }
        }
    }

    /** Nạp nhiều aggregate trong một query; id chưa có header không xuất hiện trong map. */
    public Map<Integer, PrepRecipe> findByPreppedIds(Connection conn, List<Integer> preppedIngredientIds)
            throws SQLException {
        Map<Integer, PrepRecipe> out = new LinkedHashMap<>();
        if (preppedIngredientIds == null || preppedIngredientIds.isEmpty()) return out;

        List<Integer> ids = new ArrayList<>();
        for (Integer id : preppedIngredientIds) if (id != null && !ids.contains(id)) ids.add(id);
        if (ids.isEmpty()) return out;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) placeholders.append(i == 0 ? "?" : ",?");
        String sql = SELECT + "WHERE recipe.PreppedIngredientId IN (" + placeholders + ") "
                + "ORDER BY recipe.PreppedIngredientId, ingredient.Name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setInt(i + 1, ids.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int preppedId = rs.getInt("PreppedIngredientId");
                    PrepRecipe recipe = out.computeIfAbsent(preppedId, ignored -> {
                        try { return mapHeader(rs); }
                        catch (SQLException e) { throw new MappingException(e); }
                    });
                    addLine(recipe, rs);
                }
            } catch (MappingException e) {
                throw (SQLException) e.getCause();
            }
        }
        return out;
    }

    private PrepRecipe mapHeader(ResultSet rs) throws SQLException {
        PrepRecipe recipe = new PrepRecipe();
        recipe.setPrepRecipeId(rs.getInt("PrepRecipeId"));
        recipe.setPreppedIngredientId(rs.getInt("PreppedIngredientId"));
        recipe.setYieldQty(rs.getBigDecimal("YieldQty"));
        if (rs.getTimestamp("CreatedAt") != null)
            recipe.setCreatedAt(rs.getTimestamp("CreatedAt").toLocalDateTime());
        if (rs.getTimestamp("UpdatedAt") != null)
            recipe.setUpdatedAt(rs.getTimestamp("UpdatedAt").toLocalDateTime());
        return recipe;
    }

    private void addLine(PrepRecipe recipe, ResultSet rs) throws SQLException {
        int lineId = rs.getInt("PrepRecipeIngredientId");
        if (rs.wasNull()) return;
        PrepRecipeIngredient line = new PrepRecipeIngredient();
        line.setPrepRecipeIngredientId(lineId);
        line.setPrepRecipeId(recipe.getPrepRecipeId());
        line.setRawIngredientId(rs.getInt("RawIngredientId"));
        line.setQuantity(rs.getBigDecimal("Quantity"));
        line.setRawIngredientName(rs.getString("RawName"));
        line.setRawIngredientUnit(rs.getString("RawUnit"));
        recipe.getIngredients().add(line);
    }

    public int insertHeader(Connection conn, int preppedIngredientId, BigDecimal yieldQty) throws SQLException {
        String sql = "INSERT INTO catalog.PrepRecipe(PreppedIngredientId,YieldQty) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, preppedIngredientId);
            ps.setBigDecimal(2, yieldQty);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Không lấy được PrepRecipeId sau INSERT.");
                return keys.getInt(1);
            }
        }
    }

    public int updateYield(Connection conn, int prepRecipeId, int preppedIngredientId,
                           BigDecimal yieldQty) throws SQLException {
        String sql = "UPDATE catalog.PrepRecipe SET YieldQty=?,UpdatedAt=SYSUTCDATETIME() "
                + "WHERE PrepRecipeId=? AND PreppedIngredientId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, yieldQty);
            ps.setInt(2, prepRecipeId);
            ps.setInt(3, preppedIngredientId);
            return ps.executeUpdate();
        }
    }

    public void insertIngredient(Connection conn, PrepRecipeIngredient line) throws SQLException {
        String sql = "INSERT INTO catalog.PrepRecipeIngredient(PrepRecipeId,RawIngredientId,Quantity) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, line.getPrepRecipeId());
            ps.setInt(2, line.getRawIngredientId());
            ps.setBigDecimal(3, line.getQuantity());
            ps.executeUpdate();
        }
    }

    public int updateQuantity(Connection conn, int lineId, int preppedIngredientId,
                              BigDecimal quantity) throws SQLException {
        String sql = "UPDATE line SET Quantity=? FROM catalog.PrepRecipeIngredient line "
                + "JOIN catalog.PrepRecipe recipe ON recipe.PrepRecipeId=line.PrepRecipeId "
                + "WHERE line.PrepRecipeIngredientId=? AND recipe.PreppedIngredientId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setInt(2, lineId);
            ps.setInt(3, preppedIngredientId);
            return ps.executeUpdate();
        }
    }

    public int delete(Connection conn, int lineId, int preppedIngredientId) throws SQLException {
        String sql = "DELETE line FROM catalog.PrepRecipeIngredient line "
                + "JOIN catalog.PrepRecipe recipe ON recipe.PrepRecipeId=line.PrepRecipeId "
                + "WHERE line.PrepRecipeIngredientId=? AND recipe.PreppedIngredientId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lineId);
            ps.setInt(2, preppedIngredientId);
            return ps.executeUpdate();
        }
    }

    private static final class MappingException extends RuntimeException {
        private MappingException(SQLException cause) { super(cause); }
    }
}
