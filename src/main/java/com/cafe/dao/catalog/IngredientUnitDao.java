package com.cafe.dao.catalog;

import com.cafe.model.InventoryUnitChoice;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Đọc đơn vị gốc và tối đa một đơn vị mua phụ từ catalog.Ingredient. */
public class IngredientUnitDao {
    public static final int BASE_UNIT = 0;
    public static final int PURCHASE_UNIT = 1;

    public List<InventoryUnitChoice> findByIngredient(Connection conn, int ingredientId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT IngredientId,Unit,PurchaseUnitName,PurchaseFactorToBase "
                        + "FROM catalog.Ingredient WHERE IngredientId=?")) {
            ps.setInt(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? choices(rs) : List.of();
            }
        }
    }

    public List<InventoryUnitChoice> findAllActive(Connection conn) throws SQLException {
        List<InventoryUnitChoice> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT IngredientId,Unit,PurchaseUnitName,PurchaseFactorToBase "
                        + "FROM catalog.Ingredient WHERE IsActive=1 ORDER BY IngredientId");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.addAll(choices(rs));
        }
        return out;
    }

    public InventoryUnitChoice findForUse(Connection conn, int choiceCode, int ingredientId)
            throws SQLException {
        for (InventoryUnitChoice unit : findByIngredient(conn, ingredientId))
            if (unit.getChoiceCode() == choiceCode) return unit;
        return null;
    }

    public InventoryUnitChoice findBaseForUse(Connection conn, int ingredientId) throws SQLException {
        return findForUse(conn, BASE_UNIT, ingredientId);
    }

    private List<InventoryUnitChoice> choices(ResultSet rs) throws SQLException {
        int ingredientId = rs.getInt("IngredientId");
        List<InventoryUnitChoice> out = new ArrayList<>(2);
        out.add(choice(ingredientId, BASE_UNIT, rs.getString("Unit"), BigDecimal.ONE, true));
        String purchaseName = rs.getString("PurchaseUnitName");
        BigDecimal purchaseFactor = rs.getBigDecimal("PurchaseFactorToBase");
        if (purchaseName != null && purchaseFactor != null)
            out.add(choice(ingredientId, PURCHASE_UNIT, purchaseName, purchaseFactor, false));
        return out;
    }

    private InventoryUnitChoice choice(int ingredientId, int code, String name,
                                       BigDecimal factor, boolean base) {
        InventoryUnitChoice unit = new InventoryUnitChoice();
        unit.setChoiceCode(code);
        unit.setIngredientId(ingredientId);
        unit.setUnitName(name);
        unit.setFactorToBase(factor);
        unit.setBaseUnit(base);
        return unit;
    }
}
