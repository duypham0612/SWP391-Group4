package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.PrepRecipeDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.model.PrepRecipe;
import com.cafe.model.ProductRecipe;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * A4 · RecipeService (đặc tả mục 4) — BOM của product + công thức pha sẵn (PrepRecipe).
 */
public class RecipeService {

    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();
    private final PrepRecipeDao prepRecipeDao = new PrepRecipeDao();

    // ----- Product recipe (BOM) -----
    public List<ProductRecipe> getProductRecipe(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productRecipeDao.findByProduct(conn, productId);
        }
    }

    public void addRecipeLine(int productId, int ingredientId, BigDecimal qty) throws SQLException {
        ProductRecipe r = new ProductRecipe();
        r.setProductId(productId);
        r.setIngredientId(ingredientId);
        r.setQuantity(qty);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { productRecipeDao.insert(conn, r); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateRecipeLine(int lineId, BigDecimal qty) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { productRecipeDao.update(conn, lineId, qty); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void removeRecipeLine(int lineId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { productRecipeDao.delete(conn, lineId); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    // ----- Prep recipe (RAW -> PREPPED) -----
    public List<PrepRecipe> getPrepRecipe(int preppedIngredientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return prepRecipeDao.findByPrepped(conn, preppedIngredientId);
        }
    }

    public void addPrepLine(int preppedIngredientId, int rawIngredientId, BigDecimal quantity, BigDecimal yieldQty)
            throws SQLException {
        PrepRecipe pr = new PrepRecipe();
        pr.setPreppedIngredientId(preppedIngredientId);
        pr.setRawIngredientId(rawIngredientId);
        pr.setQuantity(quantity);
        pr.setYieldQty(yieldQty);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { prepRecipeDao.insert(conn, pr); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void removePrepLine(int prepRecipeId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { prepRecipeDao.delete(conn, prepRecipeId); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
