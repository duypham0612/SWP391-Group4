package com.cafe.service.admin;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.ProductDao;
import com.cafe.dao.shared.BranchDao;
import com.cafe.model.Branch;
import com.cafe.model.Product;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Quản trị trang Home công khai (Admin): chọn món hiển thị + thứ tự + nội dung hero.
 * Đọc qua DAO, ghi trong transaction (đúng quy ước: tx sống ở Service).
 */
public class HomeAdminService {

    private final ProductDao productDao;
    private final BranchDao branchDao;

    public HomeAdminService() { this(new ProductDao(), new BranchDao()); }
    public HomeAdminService(ProductDao productDao, BranchDao branchDao) {
        this.productDao = java.util.Objects.requireNonNull(productDao);
        this.branchDao = java.util.Objects.requireNonNull(branchDao);
    }

    /** Danh sách sản phẩm đang bán (gồm cả món đang ẩn) cho màn quản trị Home. */
    public List<Product> getProductsForAdmin() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productDao.findActiveForHomeAdmin(conn);
        }
    }

    public List<Branch> getBranches() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return branchDao.findAll(conn);
        }
    }

    /** Hero thuộc chi nhánh được chọn; mặc định là chi nhánh active đầu tiên theo BranchId. */
    public Branch getHomeBranch(Integer branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            if (branchId != null && branchId > 0) {
                Branch selected = branchDao.findById(conn, branchId);
                if (selected != null) return selected;
            }
            Branch active = branchDao.findFirstActive(conn);
            if (active != null) return active;
            List<Branch> branches = branchDao.findAll(conn);
            return branches.isEmpty() ? null : branches.get(0);
        }
    }

    /**
     * Lưu hiển thị + thứ tự Home cho NHIỀU sản phẩm trong 1 transaction (nút "Lưu tất cả").
     * 3 mảng song song theo cùng chỉ số; thứ tự âm được ép về 0.
     */
    public void saveProductHomeBatch(int[] ids, boolean[] shows, int[] orders) throws SQLException {
        if (ids == null || ids.length == 0) return;
        if (shows.length != ids.length || orders.length != ids.length)
            throw new IllegalArgumentException("Số phần tử showOnHome/homeSortOrder không khớp danh sách sản phẩm.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < ids.length; i++) {
                    int order = Math.max(0, orders[i]);
                    productDao.updateHomeDisplay(conn, ids[i], shows[i], order);
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Lưu nội dung hero trang Home. */
    public void saveContent(Branch branch) throws SQLException {
        if (branch == null || branch.getBranchId() <= 0) {
            throw new BusinessException("Vui lòng chọn chi nhánh cần cập nhật hero.");
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (branchDao.updateHero(conn, branch) != 1) {
                    throw new BusinessException("Không tìm thấy chi nhánh cần cập nhật hero.");
                }
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
