package com.cafe.dao.shared;

import com.cafe.model.ProductRecipe;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductRecipeDao {

    public List<ProductRecipe> findByProduct(Connection conn, int productId) throws SQLException {
        final String sql =
            "SELECT pr.ProductRecipeId, pr.ProductId, pr.IngredientId, pr.Quantity, " +
            "       i.Name AS IngredientName, i.Unit AS IngredientUnit, i.IngredientType " +
            "FROM catalog.ProductRecipe pr JOIN catalog.Ingredient i ON pr.IngredientId = i.IngredientId " +
            "WHERE pr.ProductId = ? ORDER BY i.Name";
        List<ProductRecipe> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductRecipe r = new ProductRecipe();
                    r.setProductRecipeId(rs.getInt("ProductRecipeId"));
                    r.setProductId(rs.getInt("ProductId"));
                    r.setIngredientId(rs.getInt("IngredientId"));
                    r.setQuantity(rs.getBigDecimal("Quantity"));
                    r.setIngredientName(rs.getString("IngredientName"));
                    r.setIngredientUnit(rs.getString("IngredientUnit"));
                    r.setIngredientType(rs.getString("IngredientType"));
                    out.add(r);
                }
            }
        }
        return out;
    }

    public void insert(Connection conn, ProductRecipe r) throws SQLException {
        final String sql = "INSERT INTO catalog.ProductRecipe(ProductId, IngredientId, Quantity) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getProductId());
            ps.setInt(2, r.getIngredientId());
            ps.setBigDecimal(3, r.getQuantity());
            ps.executeUpdate();
        }
    }

    public void update(Connection conn, int productRecipeId, BigDecimal quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE catalog.ProductRecipe SET Quantity=? WHERE ProductRecipeId=?")) {
            ps.setBigDecimal(1, quantity);
            ps.setInt(2, productRecipeId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, int productRecipeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM catalog.ProductRecipe WHERE ProductRecipeId = ?")) {
            ps.setInt(1, productRecipeId);
            ps.executeUpdate();
        }
    }
}
