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
        "SELECT p.ProductId, p.CategoryId, p.Name, p.BasePrice, p.ImageUrl, p.IsActive, c.Name AS CategoryName " +
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
        final String sql = "INSERT INTO catalog.Product(CategoryId, Name, BasePrice, ImageUrl, IsActive) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getName());
            ps.setBigDecimal(3, p.getBasePrice());
            ps.setString(4, p.getImageUrl());
            ps.setBoolean(5, p.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(Connection conn, Product p) throws SQLException {
        final String sql = "UPDATE catalog.Product SET CategoryId=?, Name=?, BasePrice=?, ImageUrl=?, IsActive=? WHERE ProductId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getName());
            ps.setBigDecimal(3, p.getBasePrice());
            ps.setString(4, p.getImageUrl());
            ps.setBoolean(5, p.isActive());
            ps.setInt(6, p.getProductId());
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

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("ProductId"));
        p.setCategoryId(rs.getInt("CategoryId"));
        p.setName(rs.getString("Name"));
        p.setBasePrice(rs.getBigDecimal("BasePrice"));
        p.setImageUrl(rs.getString("ImageUrl"));
        p.setActive(rs.getBoolean("IsActive"));
        p.setCategoryName(rs.getString("CategoryName"));
        return p;
    }
}
