package com.cafe.dao.admin;

import com.cafe.model.ProductStockStatus;
import com.cafe.model.Recipe;
import com.cafe.model.Suggest86Row;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** DAO duy nhất cho cả ba loại owner của catalog.Recipe. */
public class RecipeDao {
    private static final String SELECT_LINES =
            "SELECT r.RecipeId,r.OwnerType,r.OwnerId,r.IngredientId,r.Quantity," +
            " i.Name AS IngredientName,i.Unit AS IngredientUnit,i.IngredientType,i.PrepYieldQty " +
            "FROM catalog.Recipe r JOIN catalog.Ingredient i ON i.IngredientId=r.IngredientId ";

    public List<Recipe> findByOwner(Connection conn, String ownerType, int ownerId)
            throws SQLException {
        List<Recipe> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT_LINES + "WHERE r.OwnerType=? AND r.OwnerId=? ORDER BY i.Name")) {
            ps.setString(1, ownerType);
            ps.setInt(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<Recipe> findByProduct(Connection conn, int productId) throws SQLException {
        return findByOwner(conn, Recipe.OWNER_PRODUCT, productId);
    }

    public List<Recipe> findByPrepped(Connection conn, int preppedIngredientId) throws SQLException {
        return findByOwner(conn, Recipe.OWNER_PREPPED, preppedIngredientId);
    }

    public List<Recipe> findByOption(Connection conn, int modifierOptionId) throws SQLException {
        return findByOwner(conn, Recipe.OWNER_MODIFIER, modifierOptionId);
    }

    public Map<Integer, List<Recipe>> findByPreppedIds(Connection conn,
                                                       Collection<Integer> preppedIngredientIds)
            throws SQLException {
        return findByOwnerIds(conn, Recipe.OWNER_PREPPED, preppedIngredientIds);
    }

    public Map<Integer, List<Recipe>> findByOwnerIds(Connection conn, String ownerType,
                                                      Collection<Integer> ownerIds)
            throws SQLException {
        Map<Integer, List<Recipe>> out = new LinkedHashMap<>();
        if (ownerIds == null || ownerIds.isEmpty()) return out;
        List<Integer> ids = ownerIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return out;
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql = SELECT_LINES + "WHERE r.OwnerType=? AND r.OwnerId IN (" + placeholders + ") "
                + "ORDER BY r.OwnerId,i.Name";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerType);
            for (int index = 0; index < ids.size(); index++) ps.setInt(index + 2, ids.get(index));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Recipe line = map(rs);
                    out.computeIfAbsent(line.getOwnerId(), ignored -> new ArrayList<>()).add(line);
                }
            }
        }
        return out;
    }

    public void insert(Connection conn, Recipe line) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO catalog.Recipe(OwnerType,OwnerId,IngredientId,Quantity) VALUES (?,?,?,?)")) {
            ps.setString(1, line.getOwnerType());
            ps.setInt(2, line.getOwnerId());
            ps.setInt(3, line.getIngredientId());
            ps.setBigDecimal(4, line.getQuantity());
            ps.executeUpdate();
        }
    }

    public int update(Connection conn, int recipeId, String ownerType, int ownerId,
                      BigDecimal quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE catalog.Recipe SET Quantity=? WHERE RecipeId=? AND OwnerType=? AND OwnerId=?")) {
            ps.setBigDecimal(1, quantity);
            ps.setInt(2, recipeId);
            ps.setString(3, ownerType);
            ps.setInt(4, ownerId);
            return ps.executeUpdate();
        }
    }

    public int delete(Connection conn, int recipeId, String ownerType, int ownerId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM catalog.Recipe WHERE RecipeId=? AND OwnerType=? AND OwnerId=?")) {
            ps.setInt(1, recipeId);
            ps.setString(2, ownerType);
            ps.setInt(3, ownerId);
            return ps.executeUpdate();
        }
    }

    public List<Suggest86Row> findProductsWithDepletedIngredient(Connection conn, int branchId)
            throws SQLException {
        String sql = "SELECT p.ProductId,p.Name AS ProductName,MIN(i.Name) AS IngredientName "
                + "FROM catalog.Recipe r JOIN catalog.Product p ON p.ProductId=r.OwnerId "
                + "JOIN catalog.BranchMenu bm ON bm.ProductId=p.ProductId AND bm.BranchId=? "
                + "JOIN inventory.BranchInventory bi ON bi.IngredientId=r.IngredientId AND bi.BranchId=? "
                + "JOIN catalog.Ingredient i ON i.IngredientId=r.IngredientId "
                + "WHERE r.OwnerType='PRODUCT' AND bm.IsListed=1 "
                + "AND bm.IsTemporarilyUnavailable=0 AND bi.QuantityOnHand<=0 "
                + "GROUP BY p.ProductId,p.Name ORDER BY p.Name";
        List<Suggest86Row> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Suggest86Row row = new Suggest86Row();
                    row.setProductId(rs.getInt("ProductId"));
                    row.setProductName(rs.getString("ProductName"));
                    row.setIngredientName(rs.getString("IngredientName"));
                    out.add(row);
                }
            }
        }
        return out;
    }

    public Map<Integer, ProductStockStatus> findProductStockStatuses(Connection conn, int branchId)
            throws SQLException {
        String sql = "SELECT r.OwnerId AS ProductId,i.Name AS IngredientName,"
                + "CASE WHEN bi.IngredientId IS NULL OR bi.QuantityOnHand<=0 THEN 'OUT' "
                + "WHEN bi.QuantityOnHand<=bi.MinThreshold THEN 'LOW' ELSE 'AVAILABLE' END AS StockState "
                + "FROM catalog.Recipe r "
                + "JOIN catalog.BranchMenu bm ON bm.ProductId=r.OwnerId AND bm.BranchId=? "
                + "JOIN catalog.Ingredient i ON i.IngredientId=r.IngredientId "
                + "LEFT JOIN inventory.BranchInventory bi ON bi.IngredientId=r.IngredientId AND bi.BranchId=? "
                + "WHERE r.OwnerType='PRODUCT' ORDER BY r.OwnerId,i.Name";
        Map<Integer, ProductStockStatus> out = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int productId = rs.getInt("ProductId");
                    out.computeIfAbsent(productId, ProductStockStatus::new)
                            .include(rs.getString("StockState"), rs.getString("IngredientName"));
                }
            }
        }
        return out;
    }

    public List<Recipe> findDepletedByProduct(Connection conn, int branchId, int productId)
            throws SQLException {
        String sql = "SELECT r.RecipeId,r.OwnerType,r.OwnerId,r.IngredientId,r.Quantity,"
                + "i.Name AS IngredientName,i.Unit AS IngredientUnit,i.IngredientType,i.PrepYieldQty,"
                + "ISNULL(bi.QuantityOnHand,0) AS BranchQuantityOnHand "
                + "FROM catalog.Recipe r JOIN catalog.Ingredient i ON i.IngredientId=r.IngredientId "
                + "LEFT JOIN inventory.BranchInventory bi ON bi.IngredientId=r.IngredientId AND bi.BranchId=? "
                + "WHERE r.OwnerType='PRODUCT' AND r.OwnerId=? AND ISNULL(bi.QuantityOnHand,0)<=0 ORDER BY i.Name";
        List<Recipe> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Recipe line = map(rs);
                    line.setBranchQuantityOnHand(rs.getBigDecimal("BranchQuantityOnHand"));
                    out.add(line);
                }
            }
        }
        return out;
    }

    public Set<Integer> findProductIdsWithRecipe(Connection conn, Collection<Integer> productIds)
            throws SQLException {
        Set<Integer> out = new HashSet<>();
        if (productIds == null || productIds.isEmpty()) return out;
        String placeholders = String.join(",", java.util.Collections.nCopies(productIds.size(), "?"));
        String sql = "SELECT DISTINCT OwnerId FROM catalog.Recipe WHERE OwnerType='PRODUCT' "
                + "AND OwnerId IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (Integer id : productIds) ps.setInt(index++, id);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getInt(1)); }
        }
        return out;
    }

    private Recipe map(ResultSet rs) throws SQLException {
        Recipe line = new Recipe();
        line.setRecipeId(rs.getInt("RecipeId"));
        line.setOwnerType(rs.getString("OwnerType"));
        line.setOwnerId(rs.getInt("OwnerId"));
        line.setIngredientId(rs.getInt("IngredientId"));
        line.setQuantity(rs.getBigDecimal("Quantity"));
        line.setIngredientName(rs.getString("IngredientName"));
        line.setIngredientUnit(rs.getString("IngredientUnit"));
        line.setIngredientType(rs.getString("IngredientType"));
        line.setPrepYieldQty(rs.getBigDecimal("PrepYieldQty"));
        return line;
    }
}
