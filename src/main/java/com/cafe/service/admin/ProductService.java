package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.BranchMenuDao;
import com.cafe.dao.admin.BranchDao;
import com.cafe.dao.admin.ProductChoiceDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A3 · ProductService (đặc tả mục 4).
 */
public class ProductService {
    private final ProductDao dao;
    private final BranchMenuDao branchMenuDao;
    private final ProductChoiceDao productChoiceDao;
    private final BranchDao branchDao;

    public ProductService() {
        this(new ProductDao(), new BranchMenuDao(), new ProductChoiceDao(), new BranchDao());
    }

    ProductService(ProductDao dao, BranchMenuDao branchMenuDao,
                   ProductChoiceDao productChoiceDao, BranchDao branchDao) {
        this.dao = Objects.requireNonNull(dao, "dao");
        this.branchMenuDao = Objects.requireNonNull(branchMenuDao, "branchMenuDao");
        this.productChoiceDao = Objects.requireNonNull(productChoiceDao, "productChoiceDao");
        this.branchDao = Objects.requireNonNull(branchDao, "branchDao");
    }

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
        normalizeAndValidate(p, sizeConfig);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = dao.insert(conn, p);
                saveDrinkChoices(conn, id, sizeConfig);
                conn.commit();
                return id;
            }
            catch (SQLException e) { conn.rollback(); throw translateUnique(e); }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateProduct(Product p) throws SQLException {
        updateProduct(p, ProductSizeConfig.defaults());
    }

    public void updateProduct(Product p, ProductSizeConfig sizeConfig) throws SQLException {
        normalizeAndValidate(p, sizeConfig);
        if (p.getProductId() <= 0) throw new BusinessException("Mã sản phẩm không hợp lệ.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                dao.update(conn, p);
                saveDrinkChoices(conn, p.getProductId(), sizeConfig);
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw translateUnique(e); }
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
        requirePositiveIds(productId, branchId);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requireProductAndBranch(conn, productId, branchId);
                branchMenuDao.upsert(conn, branchId, productId, true, null);
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Publish nhiều product vào BranchMenu của 1 chi nhánh trong cùng 1 transaction. */
    public void publishManyToBranch(int[] productIds, int branchId) throws SQLException {
        if (productIds == null || productIds.length == 0)
            throw new BusinessException("Vui lòng chọn ít nhất 1 sản phẩm hợp lệ.");
        if (branchId <= 0) throw new BusinessException("Vui lòng chọn chi nhánh.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (branchDao.findById(conn, branchId) == null)
                    throw new BusinessException("Chi nhánh không tồn tại.");
                for (int productId : productIds) {
                    if (productId <= 0 || dao.findById(conn, productId) == null)
                        throw new BusinessException("Danh sách sản phẩm chứa lựa chọn không hợp lệ.");
                    branchMenuDao.upsert(conn, branchId, productId, true, null);
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
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

    private static void normalizeAndValidate(Product product, ProductSizeConfig sizeConfig) {
        if (product == null) throw new BusinessException("Thông tin sản phẩm là bắt buộc.");
        String name = product.getName() == null ? null
                : product.getName().trim().replaceAll("\\s+", " ");
        if (name == null || name.isBlank()) throw new BusinessException("Tên sản phẩm không được để trống.");
        if (name.length() > 120) throw new BusinessException("Tên sản phẩm tối đa 120 ký tự.");
        if (product.getCategoryId() <= 0) throw new BusinessException("Vui lòng chọn danh mục.");
        if (product.getBasePrice() == null || product.getBasePrice().signum() < 0)
            throw new BusinessException("Giá phải là số lớn hơn hoặc bằng 0.");
        if (product.getPrepSeconds() < 60)
            throw new BusinessException("Thời gian pha chuẩn phải lớn hơn hoặc bằng 1 phút.");
        ProductSizeConfig sizes = sizeConfig == null ? ProductSizeConfig.defaults() : sizeConfig;
        if (sizes.getSizeMDelta().signum() < 0 || sizes.getSizeLDelta().signum() < 0)
            throw new BusinessException("Giá tăng size phải lớn hơn hoặc bằng 0.");
        product.setName(name);
    }

    private static void requirePositiveIds(int productId, int branchId) {
        if (productId <= 0) throw new BusinessException("Sản phẩm không hợp lệ.");
        if (branchId <= 0) throw new BusinessException("Chi nhánh không hợp lệ.");
    }

    private void requireProductAndBranch(Connection conn, int productId, int branchId) throws SQLException {
        if (dao.findById(conn, productId) == null) throw new BusinessException("Sản phẩm không tồn tại.");
        if (branchDao.findById(conn, branchId) == null) throw new BusinessException("Chi nhánh không tồn tại.");
    }

    private static SQLException translateUnique(SQLException error) {
        if (error.getErrorCode() == 2601 || error.getErrorCode() == 2627) {
            throw new BusinessException("Tên sản phẩm đã tồn tại trong danh mục này.");
        }
        return error;
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
