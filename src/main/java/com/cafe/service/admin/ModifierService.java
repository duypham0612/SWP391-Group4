package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.ModifierGroupDao;
import com.cafe.dao.shared.ModifierIngredientImpactDao;
import com.cafe.dao.shared.ModifierOptionDao;
import com.cafe.dao.shared.ProductModifierGroupDao;
import com.cafe.model.ModifierGroup;
import com.cafe.model.ModifierIngredientImpact;
import com.cafe.model.ModifierOption;
import com.cafe.model.ProductModifierGroup;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * A4 · ModifierService (đặc tả mục 4) — Group → Option → IngredientImpact + gán nhóm cho product.
 */
public class ModifierService {

    private final ModifierGroupDao groupDao = new ModifierGroupDao();
    private final ModifierOptionDao optionDao = new ModifierOptionDao();
    private final ModifierIngredientImpactDao impactDao = new ModifierIngredientImpactDao();
    private final ProductModifierGroupDao pmgDao = new ProductModifierGroupDao();

    // ----- Group -----
    public List<ModifierGroup> getModifierGroups() throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return groupDao.findAll(c); }
    }
    public ModifierGroup getModifierGroup(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return groupDao.findById(c, id); }
    }
    public int createModifierGroup(ModifierGroup g) throws SQLException { return tx(c -> groupDao.insert(c, g)); }
    public void updateModifierGroup(ModifierGroup g) throws SQLException { txVoid(c -> groupDao.update(c, g)); }

    // ----- Option -----
    public List<ModifierOption> getModifierOptions(int groupId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return optionDao.findByGroup(c, groupId); }
    }
    public ModifierOption getModifierOption(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return optionDao.findById(c, id); }
    }
    public void createModifierOption(ModifierOption o) throws SQLException { txVoid(c -> optionDao.insert(c, o)); }
    public void deleteModifierOption(int optionId) throws SQLException {
        txVoid(c -> { impactDao.deleteByOption(c, optionId); optionDao.delete(c, optionId); });
    }

    // ----- Impact -----
    public List<ModifierIngredientImpact> getModifierImpacts(int optionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return impactDao.findByOption(c, optionId); }
    }
    public void saveModifierImpact(int optionId, int ingredientId, BigDecimal qtyDelta) throws SQLException {
        ModifierIngredientImpact m = new ModifierIngredientImpact();
        m.setModifierOptionId(optionId);
        m.setIngredientId(ingredientId);
        m.setQtyDelta(qtyDelta);
        txVoid(c -> impactDao.insert(c, m));
    }
    public void deleteModifierImpact(int impactId) throws SQLException { txVoid(c -> impactDao.delete(c, impactId)); }

    // ----- Product ↔ Group assignment -----
    public List<ProductModifierGroup> getProductGroups(int productId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return pmgDao.findByProduct(c, productId); }
    }
    public void assignGroupToProduct(int productId, int groupId) throws SQLException {
        txVoid(c -> { if (!pmgDao.exists(c, productId, groupId)) pmgDao.insert(c, productId, groupId); });
    }
    public void unassignGroupFromProduct(int productId, int groupId) throws SQLException {
        txVoid(c -> pmgDao.delete(c, productId, groupId));
    }

    // ----- tx helpers -----
    private interface TxFn<T> { T run(Connection c) throws SQLException; }
    private interface TxVoid { void run(Connection c) throws SQLException; }

    private <T> T tx(TxFn<T> fn) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { T r = fn.run(c); c.commit(); return r; }
            catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }
    private void txVoid(TxVoid fn) throws SQLException { tx(c -> { fn.run(c); return null; }); }
}
