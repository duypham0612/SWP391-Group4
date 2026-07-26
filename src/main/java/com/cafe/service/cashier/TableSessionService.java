package com.cafe.service.cashier;

import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.cashier.TableSessionDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.model.DiningTable;
import com.cafe.model.OrderItem;
import com.cafe.model.TableSession;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** C3 · TableSessionService — phiên bàn (xương sống dine-in). */
public class TableSessionService {

    private final DiningTableDao tableDao = new DiningTableDao();
    private final TableSessionDao sessionDao = new TableSessionDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final com.cafe.dao.shared.OutboxEventDao outboxDao = new com.cafe.dao.shared.OutboxEventDao();

    /** Bàn đang có khách quét QR xin mở (tableId → lúc xin sớm nhất) — hiển thị trên sơ đồ bàn. */
    public java.util.Map<Integer, java.time.LocalDateTime> getPendingOpenRequests(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return outboxDao.findPendingOpenRequests(c, branchId); }
    }

    /** Tín hiệu khách đang chờ theo bàn: service.call hoặc bill.requested. */
    public Map<Integer, String> getPendingSignals(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return outboxDao.findPendingSignals(c, branchId);
        }
    }

    public List<DiningTable> getFloorMap(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return tableDao.findFloorMap(c, branchId); }
    }

    public TableSession getSession(int sessionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return sessionDao.findById(c, sessionId); }
    }

    public List<TableSession> getOpenSessions(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return sessionDao.findOpenByBranch(c, branchId); }
    }

    /** Mở phiên cho bàn (idempotent: nếu đã có phiên OPEN thì trả về phiên đó). */
    public int openSession(int branchId, int tableId, Integer cashierId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession existing = sessionDao.findOpenByTable(c, tableId);
                int sessionId;
                if (existing != null) {
                    sessionId = existing.getTableSessionId();
                } else {
                    sessionId = sessionDao.insertOpen(c, branchId, tableId, cashierId);
                    tableDao.updateStatus(c, tableId, "OCCUPIED");
                }
                // Khách quét QR ở bàn này có thể đã xin mở bàn — bàn mở rồi thì hạ tín hiệu,
                // trong cùng tx để sơ đồ bàn không còn báo chờ sau khi đã phục vụ.
                outboxDao.markOpenRequestsProcessed(c, tableId);
                c.commit();
                return sessionId;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Đóng phiên + trả bàn về EMPTY (Phase 5 sẽ chốt qua thanh toán; ở đây cho phép đóng thủ công). */
    public void closeSession(int sessionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession s = sessionDao.findById(c, sessionId);
                if (s != null) {
                    sessionDao.updateStatus(c, sessionId, "CLOSED", true);
                    tableDao.updateStatus(c, s.getDiningTableId(), "EMPTY");
                    outboxDao.markSignalsProcessed(c, sessionId);
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Close only a still-empty draft session. Returns true when the table was released. */
    public boolean closeSessionIfNoActiveItems(int sessionId) throws SQLException {
        return closeSessionIfNoActiveItems(sessionId, null);
    }

    /** Bản có kiểm tra chi nhánh để controller không thể đóng phiên của chi nhánh khác bằng ID đoán được. */
    public boolean closeSessionIfNoActiveItems(int sessionId, Integer branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession s = sessionDao.findById(c, sessionId);
                if (s == null || !"OPEN".equals(s.getStatus())
                        || (branchId != null && s.getBranchId() != branchId)) {
                    c.rollback();
                    return false;
                }
                for (OrderItem item : orderItemDao.findBySession(c, sessionId)) {
                    if (!"CANCELLED".equals(item.getStatus())) {
                        c.rollback();
                        return false;
                    }
                }
                sessionDao.updateStatus(c, sessionId, "CLOSED", true);
                tableDao.updateStatus(c, s.getDiningTableId(), "EMPTY");
                outboxDao.markSignalsProcessed(c, sessionId);
                c.commit();
                return true;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Thu ngân tiếp nhận tín hiệu của một phiên thuộc đúng chi nhánh đang đăng nhập. */
    public boolean acknowledgeSignals(int branchId, int sessionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession session = sessionDao.findById(c, sessionId);
                if (session == null || session.getBranchId() != branchId
                        || !"OPEN".equals(session.getStatus())) {
                    c.rollback();
                    return false;
                }
                int changed = outboxDao.markSignalsProcessed(c, sessionId);
                c.commit();
                return changed > 0;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Cập nhật trạng thái bàn sau khi xác minh bàn thuộc đúng chi nhánh của cashier. */
    public boolean setTableStatus(int branchId, int tableId, String status) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                DiningTable table = tableDao.findById(c, tableId);
                if (table == null || table.getBranchId() != branchId) {
                    c.rollback();
                    return false;
                }
                tableDao.updateStatus(c, tableId, status);
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Gộp bill: dồn đơn của phiên nguồn sang phiên đích, đóng phiên nguồn + trả bàn nguồn. */
    public boolean mergeSessions(int branchId, int srcSessionId, int dstSessionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession src = sessionDao.findById(c, srcSessionId);
                TableSession dst = sessionDao.findById(c, dstSessionId);
                if (src == null || dst == null
                        || srcSessionId == dstSessionId
                        || src.getBranchId() != branchId || dst.getBranchId() != branchId
                        || !"OPEN".equals(src.getStatus()) || !"OPEN".equals(dst.getStatus())) {
                    c.rollback();
                    return false;
                }
                sessionDao.reassignOrders(c, srcSessionId, dstSessionId);
                sessionDao.updateStatus(c, srcSessionId, "CLOSED", true);
                tableDao.updateStatus(c, src.getDiningTableId(), "EMPTY");
                outboxDao.markSignalsProcessed(c, srcSessionId);
                c.commit();
                return true;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }
}
