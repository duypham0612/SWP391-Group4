package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.config.Tx;
import com.cafe.dao.admin.BranchMenuDao;
import com.cafe.dao.admin.BranchDao;
import com.cafe.dao.admin.ProductChoiceDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.model.Product;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
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

    public Product getProduct(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findById(conn, id); }
    }

    public int createProduct(Product p) throws SQLException {
        normalizeAndValidate(p);
        try {
            return Tx.call(conn -> {
                int id = dao.insert(conn, p);
                productChoiceDao.saveStandardChoices(conn, id);
                return id;
            });
        } catch (SQLException e) { throw translateUnique(e); }
    }

    public void updateProduct(Product p) throws SQLException {
        normalizeAndValidate(p);
        if (p.getProductId() <= 0) throw new BusinessException("Mã sản phẩm không hợp lệ.");
        try {
            Tx.run(conn -> {
                dao.update(conn, p);
                productChoiceDao.saveStandardChoices(conn, p.getProductId());
            });
        } catch (SQLException e) { throw translateUnique(e); }
    }

    public void setProductActive(int id, boolean active) throws SQLException {
        Tx.run(conn -> dao.updateActive(conn, id, active));
    }

    /** Đảo trạng thái active (đọc + flip trong 1 tx) — bật/tắt 2 chiều. */
    public void toggleActive(int id) throws SQLException {
        Tx.run(conn -> {
            Product p = dao.findById(conn, id);
            if (p != null) dao.updateActive(conn, id, !p.isActive());
        });
    }

    /** Publish 1 product vào BranchMenu của 1 chi nhánh (mặc định bán, chưa 86, giá gốc). */
    public void publishToBranch(int productId, int branchId) throws SQLException {
        requirePositiveIds(productId, branchId);
        Tx.run(conn -> {
            requireProductAndBranch(conn, productId, branchId);
            branchMenuDao.upsert(conn, branchId, productId, true, null);
        });
    }

    /** Publish nhiều product vào BranchMenu của 1 chi nhánh trong cùng 1 transaction. */
    public void publishManyToBranch(int[] productIds, int branchId) throws SQLException {
        if (productIds == null || productIds.length == 0)
            throw new BusinessException("Vui lòng chọn ít nhất 1 sản phẩm hợp lệ.");
        if (branchId <= 0) throw new BusinessException("Vui lòng chọn chi nhánh.");
        Tx.run(conn -> {
            if (branchDao.findById(conn, branchId) == null)
                throw new BusinessException("Chi nhánh không tồn tại.");
            for (int productId : productIds) {
                if (productId <= 0 || dao.findById(conn, productId) == null)
                    throw new BusinessException("Danh sách sản phẩm chứa lựa chọn không hợp lệ.");
                branchMenuDao.upsert(conn, branchId, productId, true, null);
            }
        });
    }

    private static void normalizeAndValidate(Product product) {
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
}
