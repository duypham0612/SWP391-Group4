package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.IngredientDao;
import com.cafe.model.Ingredient;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * A4 · IngredientService (đặc tả mục 4) — NƠI đặt cờ RAW/PREPPED.
 */
public class IngredientService {

    private final IngredientDao dao = new IngredientDao();

    public List<Ingredient> getIngredientList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public List<Ingredient> getIngredientListByType(String type) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findByType(conn, type); }
    }

    public Ingredient getIngredient(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public int createIngredient(Ingredient i) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { int id = dao.insert(conn, i); conn.commit(); return id; }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateIngredient(Ingredient i) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.update(conn, i); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void deleteIngredient(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.delete(conn, id); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
