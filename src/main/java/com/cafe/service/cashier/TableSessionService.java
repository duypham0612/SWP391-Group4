package com.cafe.service.cashier;

import com.cafe.common.BusinessException;
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

    private final DiningTableDao tableDao;
    private final TableSessionDao sessionDao;
    private final OrderItemDao orderItemDao;
    private final com.cafe.dao.shared.OutboxEventDao outboxDao;

    public TableSessionService() {
        this(new DiningTableDao(), new TableSessionDao(), new OrderItemDao(),
                new com.cafe.dao.shared.OutboxEventDao());
    }
    public TableSessionService(DiningTableDao tableDao, TableSessionDao sessionDao,
                               OrderItemDao orderItemDao, com.cafe.dao.shared.OutboxEventDao outboxDao) {
        this.tableDao = java.util.Objects.requireNonNull(tableDao);
        this.sessionDao = java.util.Objects.requireNonNull(sessionDao);
        this.orderItemDao = java.util.Objects.requireNonNull(orderItemDao);
        this.outboxDao = java.util.Objects.requireNonNull(outboxDao);
    }

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
                DiningTable table = tableDao.findByIdForUpdate(c, tableId);
                if (table == null || table.getBranchId() != branchId) {
                    throw new BusinessException("Bàn không tồn tại hoặc không thuộc chi nhánh hiện tại.");
                }
                TableSession existing = sessionDao.findOpenByTable(c, tableId);
                int sessionId;
                if (existing != null) {
                    if (existing.getBranchId() != branchId) {
                        throw new BusinessException("Dữ liệu phiên bàn không khớp chi nhánh.");
                    }
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
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Chỉ đóng phiên nháp rỗng thuộc đúng chi nhánh caller. */
    public boolean closeSessionIfNoActiveItems(int sessionId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession s = sessionDao.findById(c, sessionId);
                if (s == null || !"OPEN".equals(s.getStatus())
                        || s.getBranchId() != branchId) {
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
