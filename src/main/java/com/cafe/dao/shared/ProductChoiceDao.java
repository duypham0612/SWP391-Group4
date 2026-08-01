package com.cafe.dao.shared;

import com.cafe.common.ModifierGroupNames;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistence adapter for the product choices shown by POS: size, sugar and ice.
 * The current schema stores these choices in the legacy Modifier tables.
 */
public class ProductChoiceDao {

    public Map<String, BigDecimal> findSizePriceDeltas(Connection conn, int productId)
            throws SQLException {
        int groupId = findProductSizeGroup(conn, productId);
        Map<String, BigDecimal> deltas = new HashMap<>();
        if (groupId == 0) return deltas;

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Name, PriceDelta FROM catalog.ModifierOption WHERE ModifierGroupId=?")) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    deltas.put(rs.getString("Name"), rs.getBigDecimal("PriceDelta"));
                }
            }
        }
        return deltas;
    }

    public void saveStandardChoices(Connection conn, int productId,
                                    BigDecimal sizeMDelta, BigDecimal sizeLDelta)
            throws SQLException {
        int sizeGroupId = ensureProductSizeGroup(conn, productId);
        upsertOption(conn, sizeGroupId, "Size S", BigDecimal.ZERO);
        upsertOption(conn, sizeGroupId, "Size M", nonNegative(sizeMDelta));
        upsertOption(conn, sizeGroupId, "Size L", nonNegative(sizeLDelta));

        int sugarGroupId = ensureChoiceGroup(conn, productId, ModifierGroupNames.SUGAR);
        upsertOption(conn, sugarGroupId, "Kh\u00f4ng \u0111\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, sugarGroupId, "\u00cdt \u0111\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, sugarGroupId, "B\u00ecnh th\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, sugarGroupId, "Nhi\u1ec1u \u0111\u01b0\u1eddng", BigDecimal.ZERO);

        int iceGroupId = ensureChoiceGroup(conn, productId, ModifierGroupNames.ICE);
        upsertOption(conn, iceGroupId, "Kh\u00f4ng \u0111\u00e1", BigDecimal.ZERO);
        upsertOption(conn, iceGroupId, "\u00cdt \u0111\u00e1", BigDecimal.ZERO);
        upsertOption(conn, iceGroupId, "B\u00ecnh th\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, iceGroupId, "Nhi\u1ec1u \u0111\u00e1", BigDecimal.ZERO);

    }

    private int ensureProductSizeGroup(Connection conn, int productId) throws SQLException {
        int groupId = findProductSizeGroup(conn, productId);
        if (groupId == 0) {
            groupId = insertGroup(conn, productId, ModifierGroupNames.SIZE);
        }
        return groupId;
    }

    private int findProductSizeGroup(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TOP (1) ModifierGroupId FROM catalog.ModifierGroup " +
                "WHERE ProductId=? AND Name IN(?,?) ORDER BY SortOrder,ModifierGroupId")) {
            ps.setInt(1, productId);
            ps.setString(2, ModifierGroupNames.SIZE);
            ps.setString(3, ModifierGroupNames.productSize(productId));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int ensureChoiceGroup(Connection conn, int productId, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TOP (1) ModifierGroupId FROM catalog.ModifierGroup WITH (UPDLOCK,HOLDLOCK) " +
                        "WHERE ProductId=? AND NameKey=UPPER(LTRIM(RTRIM(?))) COLLATE Latin1_General_100_CI_AI " +
                        "ORDER BY ModifierGroupId")) {
            ps.setInt(1, productId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return insertGroup(conn, productId, name);
    }

    private int insertGroup(Connection conn, int productId, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO catalog.ModifierGroup(ProductId,Name,IsRequired,MinSelect,MaxSelect,SortOrder) " +
                        "VALUES (?,?,1,1,1,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, productId);
            ps.setString(2, name);
            ps.setInt(3, sortOrder(name));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    private void upsertOption(Connection conn, int groupId, String name, BigDecimal priceDelta)
            throws SQLException {
        Integer optionId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ModifierOptionId FROM catalog.ModifierOption WHERE ModifierGroupId=? AND Name=?")) {
            ps.setInt(1, groupId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) optionId = rs.getInt(1);
            }
        }

        if (optionId == null) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO catalog.ModifierOption(ModifierGroupId, Name, PriceDelta, IsActive) VALUES (?,?,?,1)")) {
                ps.setInt(1, groupId);
                ps.setString(2, name);
                ps.setBigDecimal(3, nonNegative(priceDelta));
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE catalog.ModifierOption SET PriceDelta=?, IsActive=1 WHERE ModifierOptionId=?")) {
                ps.setBigDecimal(1, nonNegative(priceDelta));
                ps.setInt(2, optionId);
                ps.executeUpdate();
            }
        }
    }

    private int sortOrder(String name) {
        if (ModifierGroupNames.isSize(name)) return 1;
        if (ModifierGroupNames.SUGAR.equals(name)) return 2;
        if (ModifierGroupNames.ICE.equals(name)) return 3;
        if ("Topping".equalsIgnoreCase(name)) return 4;
        return 5;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
