package com.cafe.dao.shared;

import com.cafe.common.ModifierGroupNames;
import com.cafe.common.StandardModifierPolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence adapter for the product choices shown by POS: size, sugar and ice.
 * The current schema stores these choices in the legacy Modifier tables.
 */
public class ProductChoiceDao {

    public void saveStandardChoices(Connection conn, int productId) throws SQLException {
        int sizeGroupId = ensureProductSizeGroup(conn, productId);
        normalizeGroup(conn, sizeGroupId, ModifierGroupNames.SIZE);
        syncOptions(conn, sizeGroupId, ModifierGroupNames.SIZE, StandardModifierPolicy.SIZE_OPTIONS);

        int sugarGroupId = ensureChoiceGroup(conn, productId, ModifierGroupNames.SUGAR);
        normalizeGroup(conn, sugarGroupId, ModifierGroupNames.SUGAR);
        syncOptions(conn, sugarGroupId, ModifierGroupNames.SUGAR, StandardModifierPolicy.SUGAR_OPTIONS);

        int iceGroupId = ensureChoiceGroup(conn, productId, ModifierGroupNames.ICE);
        normalizeGroup(conn, iceGroupId, ModifierGroupNames.ICE);
        syncOptions(conn, iceGroupId, ModifierGroupNames.ICE, StandardModifierPolicy.ICE_OPTIONS);
    }

    /** Repairs fixed choices for products inserted outside ProductService, including demo data. */
    public int saveStandardChoicesForAllProducts(Connection conn) throws SQLException {
        List<Integer> productIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT ProductId FROM catalog.Product ORDER BY ProductId");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) productIds.add(rs.getInt(1));
        }
        for (int productId : productIds) saveStandardChoices(conn, productId);
        return productIds.size();
    }

    private int ensureProductSizeGroup(Connection conn, int productId) throws SQLException {
        int groupId = findProductSizeGroup(conn, productId);
        if (groupId == 0) {
            groupId = insertGroup(conn, productId, ModifierGroupNames.SIZE);
        }
        return groupId;
    }

    private void normalizeGroup(Connection conn, int groupId, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE catalog.ModifierGroup SET IsRequired=1,MinSelect=1,MaxSelect=1,SortOrder=? "
                        + "WHERE ModifierGroupId=?")) {
            ps.setInt(1, sortOrder(name));
            ps.setInt(2, groupId);
            ps.executeUpdate();
        }
    }

    private void syncOptions(Connection conn, int groupId, String groupName, List<String> optionNames)
            throws SQLException {
        for (String optionName : optionNames) {
            upsertOption(conn, groupId, optionName,
                    StandardModifierPolicy.priceDelta(groupName, optionName));
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(optionNames.size(), "?"));
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE catalog.ModifierOption SET IsActive=0 WHERE ModifierGroupId=? AND Name NOT IN ("
                        + placeholders + ")")) {
            int index = 1;
            ps.setInt(index++, groupId);
            for (String optionName : optionNames) ps.setString(index++, optionName);
            ps.executeUpdate();
        }
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
                ps.setBigDecimal(3, priceDelta);
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE catalog.ModifierOption SET PriceDelta=?, IsActive=1 WHERE ModifierOptionId=?")) {
                ps.setBigDecimal(1, priceDelta);
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
}
