package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A3 · ProductService (đặc tả mục 4).
 */
public class ProductService {
    private static final String GROUP_SIZE = "Size";
    private static final String GROUP_SUGAR = "\u0110\u01b0\u1eddng";
    private static final String GROUP_ICE = "\u0110\u00e1";

    private final ProductDao dao = new ProductDao();
    private final BranchMenuDao branchMenuDao = new BranchMenuDao();

    public List<Product> getProductList() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAll(conn); }
    }

    public List<Product> getProductListByCategory(int categoryId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findByCategory(conn, categoryId); }
    }

    public Product getProduct(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public ProductSizeConfig getSizeConfig(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return loadSizeConfig(conn, productId);
        }
    }

    public int createProduct(Product p) throws SQLException {
        return createProduct(p, ProductSizeConfig.defaults());
    }

    public int createProduct(Product p, ProductSizeConfig sizeConfig) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = dao.insert(conn, p);
                saveDrinkChoices(conn, id, sizeConfig);
                conn.commit();
                return id;
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateProduct(Product p) throws SQLException {
        updateProduct(p, ProductSizeConfig.defaults());
    }

    public void updateProduct(Product p, ProductSizeConfig sizeConfig) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                dao.update(conn, p);
                saveDrinkChoices(conn, p.getProductId(), sizeConfig);
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void setProductActive(int id, boolean active) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.updateActive(conn, id, active); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Đảo trạng thái active (đọc + flip trong 1 tx) — bật/tắt 2 chiều. */
    public void toggleActive(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Product p = dao.findById(conn, id);
                if (p != null) dao.updateActive(conn, id, !p.isActive());
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Publish 1 product vào BranchMenu của 1 chi nhánh (mặc định bán, chưa 86, giá gốc). */
    public void publishToBranch(int productId, int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { branchMenuDao.upsert(conn, branchId, productId, true, null, false); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Publish nhiều product vào BranchMenu của 1 chi nhánh trong cùng 1 transaction. */
    public void publishManyToBranch(int[] productIds, int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (int productId : productIds) {
                    branchMenuDao.upsert(conn, branchId, productId, true, null, false);
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private void saveDrinkChoices(Connection conn, int productId, ProductSizeConfig sizeConfig) throws SQLException {
        ProductSizeConfig cfg = sizeConfig == null ? ProductSizeConfig.defaults() : sizeConfig;
        int sizeGroupId = ensureProductSizeGroup(conn, productId);
        upsertOption(conn, sizeGroupId, "Size S", BigDecimal.ZERO);
        upsertOption(conn, sizeGroupId, "Size M", nonNegative(cfg.getSizeMDelta()));
        upsertOption(conn, sizeGroupId, "Size L", nonNegative(cfg.getSizeLDelta()));

        int sugarGroupId = ensureChoiceGroup(conn, GROUP_SUGAR, true);
        upsertOption(conn, sugarGroupId, "Kh\u00f4ng \u0111\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, sugarGroupId, "\u00cdt \u0111\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, sugarGroupId, "B\u00ecnh th\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, sugarGroupId, "Nhi\u1ec1u \u0111\u01b0\u1eddng", BigDecimal.ZERO);

        int iceGroupId = ensureChoiceGroup(conn, GROUP_ICE, true);
        upsertOption(conn, iceGroupId, "Kh\u00f4ng \u0111\u00e1", BigDecimal.ZERO);
        upsertOption(conn, iceGroupId, "\u00cdt \u0111\u00e1", BigDecimal.ZERO);
        upsertOption(conn, iceGroupId, "B\u00ecnh th\u01b0\u1eddng", BigDecimal.ZERO);
        upsertOption(conn, iceGroupId, "Nhi\u1ec1u \u0111\u00e1", BigDecimal.ZERO);

        ensureProductGroup(conn, productId, sugarGroupId);
        ensureProductGroup(conn, productId, iceGroupId);
    }

    private ProductSizeConfig loadSizeConfig(Connection conn, int productId) throws SQLException {
        int groupId = findProductSizeGroup(conn, productId);
        if (groupId == 0) return ProductSizeConfig.defaults();
        ProductSizeConfig cfg = ProductSizeConfig.defaults();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Name, PriceDelta FROM catalog.ModifierOption WHERE ModifierGroupId=?")) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("Name");
                    BigDecimal delta = rs.getBigDecimal("PriceDelta");
                    if ("Size M".equals(name)) cfg.setSizeMDelta(delta);
                    if ("Size L".equals(name)) cfg.setSizeLDelta(delta);
                }
            }
        }
        return cfg;
    }

    private int ensureProductSizeGroup(Connection conn, int productId) throws SQLException {
        int groupId = findProductSizeGroup(conn, productId);
        if (groupId == 0) {
            groupId = insertGroup(conn, GROUP_SIZE, true, 1, 1);
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

    private int ensureChoiceGroup(Connection conn, String name, boolean required) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TOP (1) ModifierGroupId FROM catalog.ModifierGroup WHERE Name=? ORDER BY ModifierGroupId")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return insertGroup(conn, name, required, required ? 1 : 0, 1);
    }

    private int insertGroup(Connection conn, String name, boolean required, int minSelect, int maxSelect) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO catalog.ModifierGroup(Name, IsRequired, MinSelect, MaxSelect) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setBoolean(2, required);
            ps.setInt(3, minSelect);
            ps.setInt(4, maxSelect);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    private void upsertOption(Connection conn, int groupId, String name, BigDecimal priceDelta) throws SQLException {
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

    public static class ProductSizeConfig {
        private BigDecimal sizeMDelta = BigDecimal.ZERO;
        private BigDecimal sizeLDelta = BigDecimal.ZERO;

        public static ProductSizeConfig defaults() { return new ProductSizeConfig(); }

        public BigDecimal getSizeMDelta() { return sizeMDelta; }
        public void setSizeMDelta(BigDecimal sizeMDelta) { this.sizeMDelta = sizeMDelta == null ? BigDecimal.ZERO : sizeMDelta; }

        public BigDecimal getSizeLDelta() { return sizeLDelta; }
        public void setSizeLDelta(BigDecimal sizeLDelta) { this.sizeLDelta = sizeLDelta == null ? BigDecimal.ZERO : sizeLDelta; }
    }
}
