package com.mycoffee.dao;

import com.mycoffee.model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    
    private Connection getConnection() throws Exception {
        return new com.mycoffee.context.DBContext().getConnection(); 
    }

    // ==========================================
    // CÁC HÀM KIỂM TRA TRÙNG TÊN MÓN ĂN
    // ==========================================
    
    // Kiểm tra trùng tên khi THÊM MỚI sản phẩm
    public boolean isProductNameExists(String productName) {
        String sql = "SELECT COUNT(*) FROM Products WHERE ProductName = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            System.out.println("Loi tai isProductNameExists (DAO): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Kiểm tra trùng tên khi CHỈNH SỬA sản phẩm (Bỏ qua ID của chính nó)
    public boolean isProductNameExistsForEdit(String productName, int productId) {
        String sql = "SELECT COUNT(*) FROM Products WHERE ProductName = ? AND ProductID != ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productName.trim());
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            System.out.println("Loi tai isProductNameExistsForEdit (DAO): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ==========================================
    // HÀM CẬP NHẬT THÔNG TIN MÓN ĂN (EDIT)
    // ==========================================
    public boolean updateProduct(Product p) {
        String sql = "UPDATE Products SET ProductName = ?, CategoryID = ?, BasePrice = ?, "
                   + "ImageURL = ?, Description = ? WHERE ProductID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getProductName().trim());
            ps.setInt(2, p.getCategoryId());
            ps.setDouble(3, p.getBasePrice());
            ps.setString(4, p.getImageUrl());
            ps.setString(5, p.getDescription());
            ps.setInt(6, p.getProductId());
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Loi tai updateProduct (DAO): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ==========================================
    // THÊM MỚI: HÀM TÌM KIẾM SẢN PHẨM THEO TÊN
    // ==========================================
    public List<Product> searchProducts(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM Products WHERE ProductName LIKE ? ORDER BY ProductID DESC";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Product(
                        rs.getInt("ProductID"),
                        rs.getString("ProductName"),
                        rs.getInt("CategoryID"),
                        rs.getDouble("BasePrice"),
                        rs.getString("ImageURL"),
                        rs.getString("Description"),
                        rs.getBoolean("IsAvailable")
                    ));
                }
            }
        } catch (Exception e) {
            System.out.println("Loi tai searchProducts (DAO): " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    // 5. THÊM MỚI SẢN PHẨM
    public boolean addProduct(Product p) {
        String sql = "INSERT INTO Products (ProductName, CategoryID, BasePrice, ImageURL, Description, IsAvailable) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getProductName());
            ps.setInt(2, p.getCategoryId());
            ps.setDouble(3, p.getBasePrice());
            ps.setString(4, p.getImageUrl());
            ps.setString(5, p.getDescription());
            ps.setBoolean(6, p.isAvailable());
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Loi tai addProduct (DAO): " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // 1. Lấy toàn bộ danh sách sản phẩm phục vụ phân trang
    public List<Product> getProductsByPage(int page, int pageSize) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM Products ORDER BY ProductID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, (page - 1) * pageSize);
            ps.setInt(2, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Product(
                        rs.getInt("ProductID"),
                        rs.getString("ProductName"),
                        rs.getInt("CategoryID"),
                        rs.getDouble("BasePrice"),
                        rs.getString("ImageURL"),
                        rs.getString("Description"),
                        rs.getBoolean("IsAvailable")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. Tính tổng số sản phẩm để chia trang
    public int getTotalProductsCount() {
        String sql = "SELECT COUNT(*) FROM Products";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Tính số lượng món mới được tạo ra trong tháng và năm hiện tại
    public int getNewProductsCountThisMonth() {
        String sql = "SELECT COUNT(*) FROM Products WHERE MONTH(CreatedAt) = MONTH(GETDATE()) AND YEAR(CreatedAt) = YEAR(GETDATE())";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.out.println("Loi tai getNewProductsCountThisMonth (DAO): " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // 3. Thay đổi trạng thái Còn hàng / Hết hàng nhanh trên Table
    public boolean toggleAvailability(int productId, boolean currentStatus) {
        String sql = "UPDATE Products SET IsAvailable = ? WHERE ProductID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, !currentStatus);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 4. Xóa món vĩnh viễn
    public boolean deleteProduct(int productId) {
        String sql = "DELETE FROM Products WHERE ProductID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
