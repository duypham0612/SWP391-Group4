package com.cafe.service.admin;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.HomeSettingDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.model.HomeSetting;
import com.cafe.model.Product;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Quản trị trang Home công khai (Admin): chọn món hiển thị + thứ tự + nội dung hero.
 * Đọc qua DAO, ghi trong transaction (đúng quy ước: tx sống ở Service).
 */
public class HomeAdminService {

    private final ProductDao productDao = new ProductDao();
    private final HomeSettingDao homeSettingDao = new HomeSettingDao();

    /** Danh sách sản phẩm đang bán (gồm cả món đang ẩn) cho màn quản trị Home. */
    public List<Product> getProductsForAdmin() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productDao.findActiveForHomeAdmin(conn);
        }
    }

    /** Nội dung hero hiện tại (null nếu chưa cấu hình). */
    public HomeSetting getHomeSetting() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return homeSettingDao.find(conn);
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
    public void saveContent(HomeSetting s) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { homeSettingDao.update(conn, s); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
