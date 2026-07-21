package com.cafe.service.shared;

import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.common.BusinessException;
import com.cafe.common.Menu86Validator;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.MenuBlockRequestDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.MenuBlockRequest;
import com.cafe.model.Suggest86Row;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BranchMenuService {

    private final BranchMenuDao dao = new BranchMenuDao();
    private final MenuBlockRequestDao menuBlockDao = new MenuBlockRequestDao();
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

    public List<MenuBlockRequest> getOpenRequests(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return menuBlockDao.findOpenByBranch(conn, branchId);
        }
    }

    public Map<Integer, MenuBlockRequest> getOpenRequestsMap(int branchId) throws SQLException {
        Map<Integer, MenuBlockRequest> out = new HashMap<>();
        for (MenuBlockRequest r : getOpenRequests(branchId)) out.put(r.getProductId(), r);
        return out;
    }

    public List<MenuBlockRequest> getRequestHistory(int branchId, int limit) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return menuBlockDao.findHistoryByBranch(conn, branchId, limit);
        }
    }

    public void save(int branchId, int productId, boolean available, BigDecimal localPrice, boolean is86) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.upsert(conn, branchId, productId, available, localPrice, is86); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
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
                if (dao.updateIs86(conn, branchId, productId, is86, ts) != 1) {
                    throw new BusinessException("Món này không còn trong menu chi nhánh. Vui lòng tải lại.");
                }
                String payload = "{\"productId\":" + productId + ",\"is86\":" + is86
                        + (backInEta == null ? "" : ",\"eta\":\"" + backInEta + "\"")
                        + (userId == null ? "" : ",\"by\":" + userId) + "}";
                EventPublisher.publish(conn, EventType.MENU_86_CHANGED, String.valueOf(productId), branchId, payload);
                conn.commit();
            }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void request86(int branchId, int productId, String reasonCode, String note,
                          LocalDateTime backInEta, int userId) throws SQLException {
        if (userId <= 0) throw new BusinessException("Không xác định được người báo tạm hết.");
        Menu86Validator.Validated v = Menu86Validator.validate(reasonCode, note, backInEta, LocalDateTime.now());
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePublished(conn, branchId, productId);
                if (menuBlockDao.findOpen(conn, branchId, productId) != null) {
                    throw new BusinessException("Món này đang có yêu cầu chờ xử lý.");
                }
                MenuBlockRequest r = new MenuBlockRequest();
                r.setBranchId(branchId);
                r.setProductId(productId);
                r.setReason(v.getReason().name());
                r.setNote(v.getNote());
                r.setBackInEta(v.getBackInEta());
                r.setRequestedBy(userId);
                int requestId = menuBlockDao.insert(conn, r);
                // ETA có thể null (sự cố bất định) — cột BackInEta đã cho NULL.
                Timestamp etaTs = v.getBackInEta() == null ? null : Timestamp.valueOf(v.getBackInEta());
                if (dao.updateIs86(conn, branchId, productId, true, etaTs) != 1) {
                    throw new BusinessException("Món này không còn trong menu chi nhánh. Vui lòng tải lại.");
                }
                String etaJson = v.getBackInEta() == null ? "null" : "\"" + v.getBackInEta() + "\"";
                String payload = "{\"productId\":" + productId
                        + ",\"is86\":true,\"eta\":" + etaJson
                        + ",\"reason\":\"" + v.getReason().name()
                        + "\",\"by\":" + userId
                        + ",\"requestId\":" + requestId + "}";
                EventPublisher.publish(conn, EventType.MENU_86_CHANGED, String.valueOf(productId), branchId, payload);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                if (isDuplicateOpenRequest(e)) {
                    throw new BusinessException("Món này đang có yêu cầu chờ xử lý.");
                }
                throw e;
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void requestReopen(int branchId, int productId, int userId) throws SQLException {
        if (userId <= 0) throw new BusinessException("Không xác định được người gửi yêu cầu.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                MenuBlockRequest open = menuBlockDao.findOpen(conn, branchId, productId);
                if (open == null) throw new BusinessException("Món này không còn chờ xử lý.");
                int affected = menuBlockDao.markReopenRequested(conn, open.getRequestId(), branchId);
                if (affected != 1) throw new BusinessException("Món này không còn chờ xử lý.");
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void reopen86(int branchId, int requestId, int reviewerId, String reviewNote, boolean rejected) throws SQLException {
        if (reviewerId <= 0) throw new BusinessException("Không xác định được người duyệt.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                MenuBlockRequest open = menuBlockDao.findOpenById(conn, requestId, branchId);
                if (open == null) throw new BusinessException("Yêu cầu đã được xử lý.");
                if (rejected && !"PENDING".equals(open.getStatus())) {
                    throw new BusinessException("Yêu cầu đã được duyệt, hãy mở bán lại khi món có lại.");
                }
                int affected = menuBlockDao.review(conn, requestId, branchId,
                        rejected ? "REJECTED" : "RESOLVED", reviewerId, reviewNote, true);
                if (affected != 1) throw new BusinessException("Yêu cầu đã được xử lý.");
                if (dao.updateIs86(conn, branchId, open.getProductId(), false, null) != 1) {
                    throw new BusinessException("Món này không còn trong menu chi nhánh. Vui lòng tải lại.");
                }
                String payload = "{\"productId\":" + open.getProductId()
                        + ",\"is86\":false,\"requestId\":" + requestId
                        + ",\"by\":" + reviewerId + "}";
                EventPublisher.publish(conn, EventType.MENU_86_CHANGED, String.valueOf(open.getProductId()), branchId, payload);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
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
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void remove(int branchId, int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { dao.remove(conn, branchId, productId); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private void ensurePublished(Connection conn, int branchId, int productId) throws SQLException {
        for (BranchMenuItem it : dao.listForBranch(conn, branchId)) {
            if (it.getProductId() == productId) {
                if (!it.isPublished()) throw new BusinessException("Món này chưa có trong menu chi nhánh.");
                return;
            }
        }
        throw new BusinessException("Món không hợp lệ.");
    }

    private boolean isDuplicateOpenRequest(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException se) {
                int code = se.getErrorCode();
                if (code == 2601 || code == 2627) return true;
                String msg = se.getMessage();
                if (msg != null && msg.contains("UX_MenuBlockRequest_Open")) return true;
            }
        }
        return false;
    }
}
