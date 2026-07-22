package com.cafe.dao.shared;

import com.cafe.model.ProductRecipe;
import com.cafe.model.Suggest86Row;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductRecipeDao {

    /**
     * B3 · Gợi ý 86 (soft): món CÒN BÁN (IsAvailable=1, Is86=0) tại chi nhánh mà có ít nhất một
     * nguyên liệu công thức tồn ≤ 0. Mỗi món trả một dòng kèm một nguyên liệu cạn đại diện.
     */
    public List<Suggest86Row> findProductsWithDepletedIngredient(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT p.ProductId, p.Name AS ProductName, MIN(i.Name) AS IngredientName " +
            "FROM catalog.ProductRecipe pr " +
            "JOIN catalog.Product p        ON p.ProductId = pr.ProductId " +
            "JOIN catalog.BranchMenu bm    ON bm.ProductId = p.ProductId AND bm.BranchId = ? " +
            "JOIN inventory.BranchInventory bi ON bi.IngredientId = pr.IngredientId AND bi.BranchId = ? " +
            "JOIN catalog.Ingredient i     ON i.IngredientId = pr.IngredientId " +
            "WHERE bm.IsAvailable = 1 AND bm.Is86 = 0 AND bi.QuantityOnHand <= 0 " +
            "GROUP BY p.ProductId, p.Name ORDER BY p.Name";
        List<Suggest86Row> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Suggest86Row r = new Suggest86Row();
                    r.setProductId(rs.getInt("ProductId"));
                    r.setProductName(rs.getString("ProductName"));
                    r.setIngredientName(rs.getString("IngredientName"));
                    out.add(r);
                }
            }
        }
        return out;
    }

    /**
     * Tập ID sản phẩm của chi nhánh đang HẾT THEO KHO: có ít nhất một nguyên liệu công thức
     * tồn ≤ 0. Đây là tín hiệu THUẦN kho (không lọc theo cờ Is86/IsAvailable) — dùng để tính
     * availability tự động cho POS/QR: món tự ẩn khi tồn cạn, tự hiện khi tồn có lại.
     */
    public java.util.Set<Integer> findDepletedProductIds(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT DISTINCT pr.ProductId " +
            "FROM catalog.ProductRecipe pr " +
            "JOIN catalog.BranchMenu bm        ON bm.ProductId = pr.ProductId AND bm.BranchId = ? " +
            "JOIN inventory.BranchInventory bi ON bi.IngredientId = pr.IngredientId AND bi.BranchId = ? " +
            "WHERE bi.QuantityOnHand <= 0";
        java.util.Set<Integer> out = new java.util.HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getInt("ProductId"));
            }
        }
        return out;
    }

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

    /** Nguyên liệu của món có tồn tại chi nhánh đang cạn (<= 0), dùng khi bỏ chặn món ở KDS. */
    public List<ProductRecipe> findDepletedByProduct(Connection conn, int branchId, int productId) throws SQLException {
        final String sql =
            "SELECT pr.ProductRecipeId, pr.ProductId, pr.IngredientId, pr.Quantity, " +
            "       i.Name AS IngredientName, i.Unit AS IngredientUnit, i.IngredientType, " +
            "       ISNULL(bi.QuantityOnHand, 0) AS BranchQuantityOnHand " +
            "FROM catalog.ProductRecipe pr " +
            "JOIN catalog.Ingredient i ON pr.IngredientId = i.IngredientId " +
            "LEFT JOIN inventory.BranchInventory bi ON bi.IngredientId = pr.IngredientId AND bi.BranchId = ? " +
            "WHERE pr.ProductId = ? AND ISNULL(bi.QuantityOnHand, 0) <= 0 " +
            "ORDER BY i.Name";
        List<ProductRecipe> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, productId);
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
                    r.setBranchQuantityOnHand(rs.getBigDecimal("BranchQuantityOnHand"));
                    out.add(r);
                }
            }
        }
        return out;
    }

    /** Tập productId (trong danh sách đầu vào) ĐÃ khai báo công thức — để KDS cảnh báo món chưa có recipe. */
    public java.util.Set<Integer> findProductIdsWithRecipe(Connection conn, java.util.Collection<Integer> productIds) throws SQLException {
        java.util.Set<Integer> out = new java.util.HashSet<>();
        if (productIds == null || productIds.isEmpty()) return out;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < productIds.size(); i++) in.append(i == 0 ? "?" : ",?");
        final String sql = "SELECT DISTINCT ProductId FROM catalog.ProductRecipe WHERE ProductId IN (" + in + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer id : productIds) ps.setInt(idx++, id);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getInt(1)); }
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
