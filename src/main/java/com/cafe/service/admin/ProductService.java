package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.ProductChoiceDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * A3 · ProductService (đặc tả mục 4).
 */
public class ProductService {
    private final ProductDao dao = new ProductDao();
    private final BranchMenuDao branchMenuDao = new BranchMenuDao();
    private final ProductChoiceDao productChoiceDao = new ProductChoiceDao();

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
        productChoiceDao.saveStandardChoices(
                conn, productId, nonNegative(cfg.getSizeMDelta()), nonNegative(cfg.getSizeLDelta()));
    }

    private ProductSizeConfig loadSizeConfig(Connection conn, int productId) throws SQLException {
        ProductSizeConfig cfg = ProductSizeConfig.defaults();
        Map<String, BigDecimal> deltas = productChoiceDao.findSizePriceDeltas(conn, productId);
        cfg.setSizeMDelta(deltas.get("Size M"));
        cfg.setSizeLDelta(deltas.get("Size L"));
        return cfg;
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
