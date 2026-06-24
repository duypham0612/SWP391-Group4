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
        // Bổ sung cột Description vào câu lệnh SELECT
        String sql = "SELECT ProductID, ProductName, BasePrice, CategoryID, ImageURL, Description FROM Products WHERE IsAvailable = 1";
        try (Connection conn = new DBContext().getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = new Product(
                        rs.getInt("ProductID"),
                        rs.getString("ProductName"),
                        rs.getDouble("BasePrice"),
                        rs.getInt("CategoryID"),
                        rs.getString("ImageURL")
                );
                p.setDescription(rs.getString("Description")); // Đổ dữ liệu Description vào Model
                list.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Lấy danh sách các món ĐÃ BỊ ẨN (IsAvailable = 0)
    public List<Product> getHiddenProducts() {
        List<Product> list = new ArrayList<>();
        // Bổ sung cột Description vào câu lệnh SELECT
        String sql = "SELECT ProductID, ProductName, BasePrice, CategoryID, ImageURL, Description FROM Products WHERE IsAvailable = 0";
        try (Connection conn = new DBContext().getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql); 
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Product p = new Product(
                        rs.getInt("ProductID"),
                        rs.getString("ProductName"),
                        rs.getDouble("BasePrice"),
                        rs.getInt("CategoryID"),
                        rs.getString("ImageURL")
                );
                p.setDescription(rs.getString("Description")); // Đổ dữ liệu Description vào Model
                list.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // Thêm sản phẩm mới kèm Hình ảnh và Mô tả
    public boolean addProduct(Product p) {
        // Bổ sung cột Description vào INSERT
        String sql = "INSERT INTO Products (ProductName, BasePrice, CategoryID, ImageURL, Description, IsAvailable) VALUES (?, ?, ?, ?, ?, 1)";
        try (Connection conn = new DBContext().getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getProductName());
            ps.setDouble(2, p.getBasePrice());
            ps.setInt(3, p.getCategoryId());
            ps.setString(4, p.getImageUrl());
            ps.setString(5, p.getDescription()); // Set giá trị Description vào câu lệnh SQL
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Cập nhật thông tin sản phẩm, Hình ảnh mới và Mô tả
    public boolean updateProduct(Product p) {
        // Bổ sung cột Description vào UPDATE
        String sql = "UPDATE Products SET ProductName = ?, BasePrice = ?, CategoryID = ?, ImageURL = ?, Description = ? WHERE ProductID = ?";
        try (Connection conn = new DBContext().getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getProductName());
            ps.setDouble(2, p.getBasePrice());
            ps.setInt(3, p.getCategoryId());
            ps.setString(4, p.getImageUrl());
            ps.setString(5, p.getDescription()); // Set giá trị Description vào câu lệnh SQL
            ps.setInt(6, p.getProductId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Xóa mềm sản phẩm (IsAvailable = 0)
    public boolean deleteProduct(int productId) {
        String sql = "UPDATE Products SET IsAvailable = 0 WHERE ProductID = ?";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Khôi phục sản phẩm (IsAvailable = 1)
    public boolean restoreProduct(int productId) {
        String sql = "UPDATE Products SET IsAvailable = 1 WHERE ProductID = ?";
        try (Connection conn = new DBContext().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
