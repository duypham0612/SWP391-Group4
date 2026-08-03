package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.CategoryDao;
import com.cafe.model.Category;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * A3 · CategoryService (đặc tả mục 4). Mở connection + transaction ở Service.
 */
public class CategoryService {

    private final CategoryDao dao;

    public CategoryService() {
        this(new CategoryDao());
    }

    CategoryService(CategoryDao dao) {
        this.dao = Objects.requireNonNull(dao, "dao");
    }

    public List<Category> getCategoryList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public Category getCategory(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public int createCategory(Category c) throws SQLException {
        normalize(c);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { int id = dao.insert(conn, c); conn.commit(); return id; }
            catch (SQLException e) { conn.rollback(); throw translateUnique(e); }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateCategory(Category c) throws SQLException {
        normalize(c);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.update(conn, c); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw translateUnique(e); }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void deleteCategory(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.delete(conn, id); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private static void normalize(Category category) {
        String name = cleanName(category.getName());
        if (name == null || name.isBlank()) throw new BusinessException("Tên danh mục không được để trống.");
        if (name.length() > 100) throw new BusinessException("Tên danh mục tối đa 100 ký tự.");
        if (category.getSortOrder() < 0) throw new BusinessException("Thứ tự phải lớn hơn hoặc bằng 0.");
        category.setName(name);
    }

    private static String cleanName(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    private static SQLException translateUnique(SQLException error) {
        if (error.getErrorCode() == 2601 || error.getErrorCode() == 2627) {
            throw new BusinessException("Tên danh mục đã tồn tại.");
        }
        return error;
    }
}
