package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.model.Product;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * A3 · ProductService (đặc tả mục 4).
 */
public class ProductService {

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

    public int createProduct(Product p) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { int id = dao.insert(conn, p); conn.commit(); return id; }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void updateProduct(Product p) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.update(conn, p); conn.commit(); }
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
}
