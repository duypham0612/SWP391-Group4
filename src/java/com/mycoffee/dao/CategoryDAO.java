package com.mycoffee.dao;

import com.mycoffee.context.DBContext; // Đảm bảo lớp này chứa kết nối SQL của bạn
import com.mycoffee.model.Category;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    // 1. LẤY TẤT CẢ DANH MỤC (SELECT)
    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT CategoryID, CategoryName, Description FROM Categories";
        
        try (Connection conn = new DBContext().getConnection(); // Thay bằng cách lấy connection của bạn
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Category c = new Category(
                    rs.getInt("CategoryID"),
                    rs.getString("CategoryName"),
                    rs.getString("Description")
                );
                list.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. THÊM MỚI DANH MỤC (INSERT)
    public boolean insertCategory(Category category) {
        String sql = "INSERT INTO Categories (CategoryName, Description) VALUES (?, ?)";
        
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, category.getCategoryName());
            ps.setString(2, category.getDescription());
            
            return ps.executeUpdate() > 0; // Trả về true nếu thêm thành công
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 3. CẬP NHẬT DANH MỤC (UPDATE)
    public boolean updateCategory(Category category) {
        String sql = "UPDATE Categories SET CategoryName = ?, Description = ? WHERE CategoryID = ?";
        
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, category.getCategoryName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, category.getCategoryId());
            
            return ps.executeUpdate() > 0; // Trả về true nếu cập nhật thành công
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 4. XÓA DANH MỤC (DELETE)
    public boolean deleteCategory(int id) {
        String sql = "DELETE FROM Categories WHERE CategoryID = ?";
        
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            
            return ps.executeUpdate() > 0; // Trả về true nếu xóa thành công
        } catch (Exception e) {
            // Nếu dính khóa ngoại (bảng Products đang dùng CategoryID này), chương trình sẽ nhảy vào đây
            e.printStackTrace();
        }
        return false;
    }
}
