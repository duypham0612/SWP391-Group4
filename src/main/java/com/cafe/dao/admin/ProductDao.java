package com.cafe.dao.admin;

import com.cafe.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    private static final String SELECT =
        "SELECT p.ProductId, p.CategoryId, p.Name, p.BasePrice, p.ImageUrl, p.IsActive, " +
        "p.ShowOnHome, p.HomeSortOrder, p.PrepSeconds, c.Name AS CategoryName " +
        "FROM catalog.Product p JOIN catalog.Category c ON p.CategoryId = c.CategoryId ";

    public List<Product> findAll(Connection conn) throws SQLException {
        List<Product> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY c.SortOrder, p.Name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<Product> findByCategory(Connection conn, int categoryId) throws SQLException {
        List<Product> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE p.CategoryId = ? ORDER BY p.Name")) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public Product findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE p.ProductId = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Product p) throws SQLException {
        final String sql = "INSERT INTO catalog.Product(CategoryId, Name, BasePrice, ImageUrl, IsActive, PrepSeconds) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getName());
            ps.setBigDecimal(3, p.getBasePrice());
            ps.setString(4, p.getImageUrl());
            ps.setBoolean(5, p.isActive());
            ps.setInt(6, p.getPrepSeconds());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(Connection conn, Product p) throws SQLException {
        final String sql = "UPDATE catalog.Product SET CategoryId=?, Name=?, BasePrice=?, ImageUrl=?, IsActive=?, PrepSeconds=? WHERE ProductId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getName());
            ps.setBigDecimal(3, p.getBasePrice());
            ps.setString(4, p.getImageUrl());
            ps.setBoolean(5, p.isActive());
            ps.setInt(6, p.getPrepSeconds());
            ps.setInt(7, p.getProductId());
            ps.executeUpdate();
        }
    }

    public void updateActive(Connection conn, int id, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE catalog.Product SET IsActive=? WHERE ProductId=?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // ===== Trang Home công khai =====

    /** Sản phẩm hiển thị trên Home: đang bán + ShowOnHome, theo danh mục rồi thứ tự Home. */
    public List<Product> findForHome(Connection conn) throws SQLException {
        List<Product> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE p.IsActive = 1 AND p.ShowOnHome = 1 ORDER BY c.SortOrder, p.HomeSortOrder, p.Name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    /** Toàn bộ sản phẩm đang bán cho màn quản trị Home (gồm cả món đang ẩn), theo thứ tự Home. */
    public List<Product> findActiveForHomeAdmin(Connection conn) throws SQLException {
        List<Product> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE p.IsActive = 1 ORDER BY c.SortOrder, p.HomeSortOrder, p.Name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<Product> findForRecipeLookup(Connection conn, String q, Integer categoryId,
                                             String recipeState, Integer branchId,
                                             int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + "WHERE p.IsActive = 1");
        appendRecipeWhere(sql, q, categoryId, recipeState, branchId);
        sql.append(" ORDER BY c.SortOrder, p.Name, p.ProductId OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        List<Product> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = bindRecipeFilters(ps, q, categoryId, recipeState, branchId);
            ps.setInt(i++, Math.max(0, offset));
            ps.setInt(i, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public int countForRecipeLookup(Connection conn, String q, Integer categoryId,
                                    String recipeState, Integer branchId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) " +
                "FROM catalog.Product p JOIN catalog.Category c ON p.CategoryId = c.CategoryId " +
                "WHERE p.IsActive = 1");
        appendRecipeWhere(sql, q, categoryId, recipeState, branchId);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindRecipeFilters(ps, q, categoryId, recipeState, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Đọc chi tiết một món chỉ khi món đó vẫn thuộc đúng tập kết quả mà Barista
     * đang tra cứu. Không dùng {@link #findById(Connection, int)} ở màn Barista
     * vì lời gọi trực tiếp productId có thể bỏ qua phạm vi chi nhánh / bộ lọc.
     */
    public Product findForRecipeLookupById(Connection conn, int productId, String q,
                                           Integer categoryId, String recipeState,
                                           Integer branchId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                SELECT + "WHERE p.ProductId = ? AND p.IsActive = 1");
        appendRecipeWhere(sql, q, categoryId, recipeState, branchId);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, productId);
            bindRecipeFilters(ps, 2, q, categoryId, recipeState, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Cập nhật trạng thái hiển thị + thứ tự trên Home cho 1 sản phẩm. */
    public void updateHomeDisplay(Connection conn, int id, boolean showOnHome, int homeSortOrder) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE catalog.Product SET ShowOnHome=?, HomeSortOrder=? WHERE ProductId=?")) {
            ps.setBoolean(1, showOnHome);
            ps.setInt(2, homeSortOrder);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    private void appendRecipeWhere(StringBuilder sql, String q, Integer categoryId,
                                   String recipeState, Integer branchId) {
        if (q != null && !q.isBlank()) {
            sql.append(" AND p.Name COLLATE SQL_Latin1_General_CP1_CI_AI LIKE ? COLLATE SQL_Latin1_General_CP1_CI_AI ESCAPE '\\'");
        }
        if (categoryId != null) sql.append(" AND p.CategoryId = ?");
        if ("HAS".equals(recipeState)) {
            sql.append(" AND EXISTS (SELECT 1 FROM catalog.ProductRecipe pr WHERE pr.ProductId = p.ProductId)");
        } else if ("NONE".equals(recipeState)) {
            sql.append(" AND NOT EXISTS (SELECT 1 FROM catalog.ProductRecipe pr WHERE pr.ProductId = p.ProductId)");
        }
        if (branchId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM catalog.BranchMenu bm WHERE bm.ProductId = p.ProductId AND bm.BranchId = ?)");
        }
    }

    private int bindRecipeFilters(PreparedStatement ps, String q, Integer categoryId,
                                  String recipeState, Integer branchId) throws SQLException {
        return bindRecipeFilters(ps, 1, q, categoryId, recipeState, branchId);
    }

    private int bindRecipeFilters(PreparedStatement ps, int startIndex, String q,
                                  Integer categoryId, String recipeState,
                                  Integer branchId) throws SQLException {
        int i = startIndex;
        if (q != null && !q.isBlank()) ps.setString(i++, "%" + escapeLike(q.trim()) + "%");
        if (categoryId != null) ps.setInt(i++, categoryId);
        if (branchId != null) ps.setInt(i++, branchId);
        return i;
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("[", "\\[");
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("ProductId"));
        p.setCategoryId(rs.getInt("CategoryId"));
        p.setName(rs.getString("Name"));
        p.setBasePrice(rs.getBigDecimal("BasePrice"));
        p.setImageUrl(rs.getString("ImageUrl"));
        p.setActive(rs.getBoolean("IsActive"));
        p.setShowOnHome(rs.getBoolean("ShowOnHome"));
        p.setHomeSortOrder(rs.getInt("HomeSortOrder"));
        p.setPrepSeconds(rs.getInt("PrepSeconds"));
        p.setCategoryName(rs.getString("CategoryName"));
        return p;
    }
}
