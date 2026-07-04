package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    public List<Product> getAllAvailableProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.ProductID, p.ProductName, p.BasePrice, p.CategoryID, "
                   + "c.CategoryName, p.ImageURL, p.Description "
                   + "FROM Products p "
                   + "LEFT JOIN Categories c ON p.CategoryID = c.CategoryID "
                   + "WHERE p.IsAvailable = 1 "
                   + "ORDER BY p.CategoryID, p.ProductName";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public Product getAvailableProductById(int productId) {
        String sql = "SELECT p.ProductID, p.ProductName, p.BasePrice, p.CategoryID, "
                   + "c.CategoryName, p.ImageURL, p.Description "
                   + "FROM Products p "
                   + "LEFT JOIN Categories c ON p.CategoryID = c.CategoryID "
                   + "WHERE p.ProductID = ? AND p.IsAvailable = 1";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Product> getHiddenProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.ProductID, p.ProductName, p.BasePrice, p.CategoryID, "
                   + "c.CategoryName, p.ImageURL, p.Description "
                   + "FROM Products p "
                   + "LEFT JOIN Categories c ON p.CategoryID = c.CategoryID "
                   + "WHERE ISNULL(p.IsAvailable, 1) = 0 "
                   + "ORDER BY p.ProductName";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addProduct(Product product) {
        String sql = "INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable) "
                   + "VALUES (?, ?, ?, ?, ?, 1)";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getProductName());
            ps.setInt(2, product.getCategoryId());
            ps.setDouble(3, product.getBasePrice());
            ps.setString(4, product.getImageUrl());
            ps.setString(5, product.getDescription());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateProduct(Product product) {
        String sql = "UPDATE Products "
                   + "SET ProductName = ?, CategoryID = ?, BasePrice = ?, ImageURL = ?, Description = ? "
                   + "WHERE ProductID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getProductName());
            ps.setInt(2, product.getCategoryId());
            ps.setDouble(3, product.getBasePrice());
            ps.setString(4, product.getImageUrl());
            ps.setString(5, product.getDescription());
            ps.setInt(6, product.getProductId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteProduct(int productId) {
        return setProductAvailability(productId, false);
    }

    public boolean restoreProduct(int productId) {
        return setProductAvailability(productId, true);
    }

    private boolean setProductAvailability(int productId, boolean isAvailable) {
        String sql = "UPDATE Products SET IsAvailable = ? WHERE ProductID = ?";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isAvailable);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private Product mapProduct(ResultSet rs) throws Exception {
        return new Product(
                rs.getInt("ProductID"),
                rs.getString("ProductName"),
                rs.getDouble("BasePrice"),
                rs.getInt("CategoryID"),
                rs.getString("CategoryName"),
                rs.getString("ImageURL"),
                rs.getString("Description")
        );
    }
}

