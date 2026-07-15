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

/** C3 · TableSessionService — phiên bàn (xương sống dine-in). */
public class TableSessionService {

    private final DiningTableDao tableDao = new DiningTableDao();
    private final TableSessionDao sessionDao = new TableSessionDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();

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
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Close only a still-empty draft session. Returns true when the table was released. */
    public boolean closeSessionIfNoActiveItems(int sessionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession s = sessionDao.findById(c, sessionId);
                if (s == null || !"OPEN".equals(s.getStatus())) {
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
                c.commit();
                return true;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void setTableStatus(int tableId, String status) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { tableDao.updateStatus(c, tableId, status); c.commit(); }
            catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Gộp bill: dồn đơn của phiên nguồn sang phiên đích, đóng phiên nguồn + trả bàn nguồn. */
    public void mergeSessions(int srcSessionId, int dstSessionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession src = sessionDao.findById(c, srcSessionId);
                sessionDao.reassignOrders(c, srcSessionId, dstSessionId);
                sessionDao.updateStatus(c, srcSessionId, "CLOSED", true);
                if (src != null) tableDao.updateStatus(c, src.getDiningTableId(), "EMPTY");
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }
}
