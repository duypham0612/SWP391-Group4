package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.CategoryDao;
import com.cafe.model.Category;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * A3 · CategoryService (đặc tả mục 4). Mở connection + transaction ở Service.
 */
public class CategoryService {

    private final CategoryDao dao = new CategoryDao();

    public List<Category> getCategoryList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public Category getCategory(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public int createCategory(Category c) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { int id = dao.insert(conn, c); conn.commit(); return id; }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateCategory(Category c) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.update(conn, c); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
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
}
