package com.cafe.dao.admin;

import com.cafe.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO mẫu (lát cắt CRUD). Nhận Connection từ Service; chỉ truy vấn, không nghiệp vụ,
 * không mở/đóng connection, không transaction.
 */
public class CategoryDao {

    public List<Category> findAll(Connection conn) throws SQLException {
        final String sql = "SELECT CategoryId, Name, SortOrder, IsActive FROM catalog.Category " +
                "ORDER BY SortOrder, Name";
        List<Category> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public Category findById(Connection conn, int id) throws SQLException {
        final String sql = "SELECT CategoryId, Name, SortOrder, IsActive FROM catalog.Category WHERE CategoryId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Category c) throws SQLException {
        final String sql = "INSERT INTO catalog.Category(Name, SortOrder, IsActive) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setInt(2, c.getSortOrder());
            ps.setBoolean(3, c.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(Connection conn, Category c) throws SQLException {
        final String sql = "UPDATE catalog.Category SET Name = ?, SortOrder = ?, IsActive = ? WHERE CategoryId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setInt(2, c.getSortOrder());
            ps.setBoolean(3, c.isActive());
            ps.setInt(4, c.getCategoryId());
            ps.executeUpdate();
        }
    }

    /** Xoá mềm để giữ ràng buộc FK với Product. */
    public void delete(Connection conn, int id) throws SQLException {
        final String sql = "UPDATE catalog.Category SET IsActive = 0 WHERE CategoryId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setCategoryId(rs.getInt("CategoryId"));
        c.setName(rs.getString("Name"));
        c.setSortOrder(rs.getInt("SortOrder"));
        c.setActive(rs.getBoolean("IsActive"));
        return c;
    }
}
