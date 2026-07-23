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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A4 · ModifierService (đặc tả mục 4) — Group → Option → IngredientImpact + gán nhóm cho product.
 */
public class ModifierService {
    private static final String GROUP_SIZE = "Size";
    private static final String GROUP_SUGAR = "\u0110\u01b0\u1eddng";
    private static final String GROUP_ICE = "\u0110\u00e1";

    private final ModifierGroupDao groupDao = new ModifierGroupDao();
    private final ModifierOptionDao optionDao = new ModifierOptionDao();
    private final ModifierIngredientImpactDao impactDao = new ModifierIngredientImpactDao();
    private final ProductModifierGroupDao pmgDao = new ProductModifierGroupDao();

    // ----- Group -----
    public List<ModifierGroup> getModifierGroups() throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return groupDao.findAll(c); }
    }
    public List<ModifierGroup> getChoiceGroups() throws SQLException {
        List<ModifierGroup> out = new ArrayList<>();
        for (ModifierGroup g : getModifierGroups()) {
            if (isChoiceGroup(g.getName())) out.add(g);
        }
        return out;
    }
    public ModifierGroup getModifierGroup(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return groupDao.findById(c, id); }
    }
    public int createModifierGroup(ModifierGroup g) throws SQLException { return tx(c -> groupDao.insert(c, g)); }
    public void updateModifierGroup(ModifierGroup g) throws SQLException { txVoid(c -> groupDao.update(c, g)); }
    /** Xoá group: dọn dependent (impact→option, link product) trước rồi xoá group — 1 tx. */
    public void deleteModifierGroup(int groupId) throws SQLException {
        txVoid(c -> {
            for (ModifierOption o : optionDao.findByGroup(c, groupId)) {
                impactDao.deleteByOption(c, o.getModifierOptionId());
                optionDao.delete(c, o.getModifierOptionId());
            }
            pmgDao.deleteByGroup(c, groupId);
            groupDao.delete(c, groupId);
        });
    }

    // ----- Option -----
    public List<ModifierOption> getModifierOptions(int groupId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return optionDao.findByGroup(c, groupId); }
    }
    public ModifierOption getModifierOption(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return optionDao.findById(c, id); }
    }
    public void createModifierOption(ModifierOption o) throws SQLException { txVoid(c -> optionDao.insert(c, o)); }
    public void updateModifierOption(ModifierOption o) throws SQLException { txVoid(c -> optionDao.update(c, o)); }
    public void deleteModifierOption(int optionId) throws SQLException {
        txVoid(c -> { impactDao.deleteByOption(c, optionId); optionDao.delete(c, optionId); });
    }

    // ----- Impact -----
    public List<ModifierIngredientImpact> getModifierImpacts(int optionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return impactDao.findByOption(c, optionId); }
    }
    /** Định mức của mọi option trong 1 group, gom theo optionId — cho workspace render 1 lần. */
    public Map<Integer, List<ModifierIngredientImpact>> getImpactsByOptionMap(int groupId) throws SQLException {
        Map<Integer, List<ModifierIngredientImpact>> map = new HashMap<>();
        try (Connection c = DBConnection.getConnection()) {
            for (ModifierIngredientImpact m : impactDao.findByGroup(c, groupId)) {
                map.computeIfAbsent(m.getModifierOptionId(), k -> new ArrayList<>()).add(m);
            }
        }
        return map;
    }
    /** @return true nếu thêm mới; false nếu nguyên liệu đã có trong option (trùng). */
    public boolean saveModifierImpact(int optionId, int ingredientId, BigDecimal qtyDelta) throws SQLException {
        return tx(c -> {
            if (impactDao.exists(c, optionId, ingredientId)) return false;
            ModifierIngredientImpact m = new ModifierIngredientImpact();
            m.setModifierOptionId(optionId);
            m.setIngredientId(ingredientId);
            m.setQtyDelta(qtyDelta);
            impactDao.insert(c, m);
            return true;
        });
    }
    public void deleteModifierImpact(int impactId) throws SQLException { txVoid(c -> impactDao.delete(c, impactId)); }

    // ----- Product ↔ Group assignment -----
    public List<ProductModifierGroup> getProductGroups(int productId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return pmgDao.findByProduct(c, productId); }
    }
    public List<ProductModifierGroup> getProductChoiceGroups(int productId) throws SQLException {
        List<ProductModifierGroup> out = new ArrayList<>();
        for (ProductModifierGroup group : getProductGroups(productId)) {
            if (isChoiceGroup(group.getGroupName())) out.add(group);
        }
        return out;
    }
    public void assignGroupToProduct(int productId, int groupId) throws SQLException {
        txVoid(c -> { if (!pmgDao.exists(c, productId, groupId)) pmgDao.insert(c, productId, groupId); });
    }
    public void unassignGroupFromProduct(int productId, int groupId) throws SQLException {
        txVoid(c -> pmgDao.delete(c, productId, groupId));
    }

    public boolean isChoiceGroup(String name) {
        return GROUP_SIZE.equals(name) || GROUP_SUGAR.equals(name) || GROUP_ICE.equals(name);
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
