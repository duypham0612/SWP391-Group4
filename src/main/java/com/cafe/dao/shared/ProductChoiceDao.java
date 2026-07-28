package com.cafe.dao.shared;

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

    private static final String GROUP_SIZE = "Size";
    private static final String GROUP_SUGAR = "\u0110\u01b0\u1eddng";
    private static final String GROUP_ICE = "\u0110\u00e1";

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

        int sugarGroupId = ensureChoiceGroup(conn, GROUP_SUGAR);
        upsertOption(conn, sugarGroupId, "Kh\u00f4ng \u0111\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, sugarGroupId, "\u00cdt \u0111\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, sugarGroupId, "B\u00ecnh th\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, sugarGroupId, "Nhi\u1ec1u \u0111\u01b0\u1eddng", BigDecimal.ZERO);

        int iceGroupId = ensureChoiceGroup(conn, GROUP_ICE);
        upsertOption(conn, iceGroupId, "Kh\u00f4ng \u0111\u00e1", BigDecimal.ZERO);
        upsertOption(conn, iceGroupId, "\u00cdt \u0111\u00e1", BigDecimal.ZERO);
        upsertOption(conn, iceGroupId, "B\u00ecnh th\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, iceGroupId, "Nhi\u1ec1u \u0111\u00e1", BigDecimal.ZERO);

        ensureProductGroup(conn, productId, sugarGroupId);
        ensureProductGroup(conn, productId, iceGroupId);
    }

    private int ensureProductSizeGroup(Connection conn, int productId) throws SQLException {
        int groupId = findProductSizeGroup(conn, productId);
        if (groupId == 0) {
            groupId = insertGroup(conn, GROUP_SIZE);
            ensureProductGroup(conn, productId, groupId);
        }
        return groupId;
    }

    private int findProductSizeGroup(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TOP (1) mg.ModifierGroupId " +
                "FROM catalog.ProductModifierGroup pmg " +
                "JOIN catalog.ModifierGroup mg ON mg.ModifierGroupId=pmg.ModifierGroupId " +
                "WHERE pmg.ProductId=? AND mg.Name=? ORDER BY mg.ModifierGroupId")) {
            ps.setInt(1, productId);
            ps.setString(2, GROUP_SIZE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int ensureChoiceGroup(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TOP (1) ModifierGroupId FROM catalog.ModifierGroup WHERE Name=? ORDER BY ModifierGroupId")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return insertGroup(conn, name);
    }

    private int insertGroup(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO catalog.ModifierGroup(Name, IsRequired, MinSelect, MaxSelect) VALUES (?,1,1,1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
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

    private void ensureProductGroup(Connection conn, int productId, int groupId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "IF NOT EXISTS (SELECT 1 FROM catalog.ProductModifierGroup WHERE ProductId=? AND ModifierGroupId=?) " +
                "INSERT INTO catalog.ProductModifierGroup(ProductId, ModifierGroupId) VALUES (?,?)")) {
            ps.setInt(1, productId);
            ps.setInt(2, groupId);
            ps.setInt(3, productId);
            ps.setInt(4, groupId);
            ps.executeUpdate();
        }
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
