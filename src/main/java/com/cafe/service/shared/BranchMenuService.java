package com.cafe.service.shared;

import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.Suggest86Row;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class BranchMenuService {

    private final BranchMenuDao dao = new BranchMenuDao();
    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();

    /** B3 · Gợi ý 86 (soft): món còn bán nhưng có nguyên liệu đã cạn (≤0) — để barista cân nhắc báo hết. */
    public List<Suggest86Row> getSuggested86(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productRecipeDao.findProductsWithDepletedIngredient(conn, branchId);
        }
    }

    public List<BranchMenuItem> listForBranch(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.listForBranch(conn, branchId); }
    }

    public void save(int branchId, int productId, boolean available, BigDecimal localPrice, boolean is86) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.upsert(conn, branchId, productId, available, localPrice, is86); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** B3 · 86 board (Barista) — bật/tắt hết món; khoá món khỏi POS + QR menu. */
    public void set86(int branchId, int productId, boolean is86) throws SQLException {
        set86(branchId, productId, is86, null, null);
    }

    /** B3.F3 · 86 kèm ETA dự kiến có lại (NULL = chưa rõ); mở bán lại tự xoá ETA. */
    public void set86(int branchId, int productId, boolean is86, java.time.LocalDateTime backInEta) throws SQLException {
        set86(branchId, productId, is86, backInEta, null);
    }

    /**
     * B3.F3 · 86 kèm ETA + AUDIT — ghi domain event {@code menu.86_changed} vào ops.OutboxEvent
     * trong CÙNG tx (ai/khi nào bật-tắt + lên bus cho QR/KDS realtime). {@code userId} null = không rõ.
     */
    public void set86(int branchId, int productId, boolean is86,
                      java.time.LocalDateTime backInEta, Integer userId) throws SQLException {
        java.sql.Timestamp ts = backInEta == null ? null : java.sql.Timestamp.valueOf(backInEta);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                dao.updateIs86(conn, branchId, productId, is86, ts);
                String payload = "{\"productId\":" + productId + ",\"is86\":" + is86
                        + (backInEta == null ? "" : ",\"eta\":\"" + backInEta + "\"")
                        + (userId == null ? "" : ",\"by\":" + userId) + "}";
                EventPublisher.publish(conn, EventType.MENU_86_CHANGED, String.valueOf(productId), branchId, payload);
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Danh sách món của chi nhánh (cho 86 board). */
    public List<BranchMenuItem> getMenuAvailability(int branchId) throws SQLException {
        return listForBranch(branchId);
    }

    /** M8 · Ẩn (ngừng bán) nhiều món cùng lúc — giữ nguyên giá địa phương & cờ 86, trong 1 transaction. */
    public void hideMany(int branchId, java.util.Set<Integer> productIds) throws SQLException {
        if (productIds == null || productIds.isEmpty()) return;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (BranchMenuItem it : dao.listForBranch(conn, branchId)) {
                    if (productIds.contains(it.getProductId()) && it.isAvailable()) {
                        dao.upsert(conn, branchId, it.getProductId(), false, it.getLocalPrice(), it.isIs86());
                    }
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void remove(int branchId, int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.remove(conn, branchId, productId); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
