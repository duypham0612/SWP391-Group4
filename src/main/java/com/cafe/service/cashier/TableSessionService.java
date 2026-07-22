package com.cafe.service.cashier;

import com.cafe.common.DiningTableValidator;
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
import java.util.Locale;
import java.util.UUID;

/** C3 · TableSessionService — phiên bàn (xương sống dine-in). */
public class TableSessionService {

    private final DiningTableDao tableDao = new DiningTableDao();
    private final TableSessionDao sessionDao = new TableSessionDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();

    public List<DiningTable> getFloorMap(int branchId) throws SQLException {
        return getFloorMap(branchId, false);
    }

    public List<DiningTable> getFloorMap(int branchId, boolean includeHidden) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return tableDao.findFloorMap(c, branchId, includeHidden); }
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
                DiningTable table = tableDao.findById(c, tableId);
                if (table == null || table.getBranchId() != branchId || !table.isVisible()) {
                    throw new IllegalArgumentException("Bàn không tồn tại hoặc đang bị ẩn.");
                }
                if ("CLEANING".equals(table.getStatus())) {
                    throw new IllegalStateException("Bàn đang dọn, chưa thể mở phiên.");
                }
                if (table.isMerged()) tableId = table.getMergedIntoTableId();
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
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
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
                    tableDao.releaseMergedChildren(c, s.getDiningTableId());
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
                tableDao.releaseMergedChildren(c, s.getDiningTableId());
                tableDao.updateStatus(c, s.getDiningTableId(), "EMPTY");
                c.commit();
                return true;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void setTableStatus(int tableId, String status) throws SQLException {
        if (!"EMPTY".equals(status) && !"CLEANING".equals(status)) {
            throw new IllegalArgumentException("Trạng thái bàn không hợp lệ.");
        }
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

    public int saveTable(Integer tableId, int branchId, String tableNumber, int capacity) throws SQLException {
        String normalizedName = DiningTableValidator.normalizeTableNumber(tableNumber);
        int normalizedCapacity = DiningTableValidator.requireCapacity(capacity);
        try (Connection c = DBConnection.getConnection()) {
            if (tableId == null) {
                String qrCode = "QR-B" + branchId + "-" + UUID.randomUUID().toString()
                        .replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
                return tableDao.insert(c, branchId, normalizedName, normalizedCapacity, qrCode);
            }
            DiningTable table = tableDao.findById(c, tableId);
            if (table == null || table.getBranchId() != branchId) {
                throw new IllegalArgumentException("Không tìm thấy bàn trong chi nhánh.");
            }
            if (tableDao.updateDetails(c, tableId, branchId, normalizedName, normalizedCapacity) == 0) {
                throw new IllegalStateException("Không thể cập nhật bàn.");
            }
            return tableId;
        }
    }

    public void setTableVisibility(int tableId, int branchId, boolean visible) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                DiningTable table = tableDao.findById(c, tableId);
                if (table == null || table.getBranchId() != branchId) {
                    throw new IllegalArgumentException("Không tìm thấy bàn trong chi nhánh.");
                }
                if (!visible && (table.isOccupied() || table.isMerged() || tableDao.hasMergedChildren(c, tableId)
                        || sessionDao.findOpenByTable(c, tableId) != null)) {
                    throw new IllegalStateException("Không thể ẩn bàn đang mở hoặc đang được ghép.");
                }
                tableDao.updateVisibility(c, tableId, branchId, visible);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally { c.setAutoCommit(true); }
        }
    }

    /** Merge a physical source table into a destination and consolidate both sessions. */
    public int mergeTables(int branchId, int sourceTableId, int destinationTableId, Integer cashierId) throws SQLException {
        DiningTableValidator.requireDifferentTables(sourceTableId, destinationTableId);
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                DiningTable source = tableDao.findById(c, sourceTableId);
                DiningTable destination = tableDao.findById(c, destinationTableId);
                if (source == null || destination == null
                        || source.getBranchId() != branchId || destination.getBranchId() != branchId) {
                    throw new IllegalArgumentException("Hai bàn phải thuộc chi nhánh hiện tại.");
                }
                if (!source.isVisible() || !destination.isVisible()) {
                    throw new IllegalStateException("Không thể ghép bàn đang bị ẩn.");
                }
                if (source.isMerged() || destination.isMerged() || tableDao.hasMergedChildren(c, sourceTableId)) {
                    throw new IllegalStateException("Bàn nguồn hoặc bàn đích đã nằm trong một nhóm ghép.");
                }

                TableSession destinationSession = sessionDao.findOpenByTable(c, destinationTableId);
                if (destinationSession == null) {
                    int destinationSessionId = sessionDao.insertOpen(c, branchId, destinationTableId, cashierId);
                    destinationSession = sessionDao.findById(c, destinationSessionId);
                }
                TableSession sourceSession = sessionDao.findOpenByTable(c, sourceTableId);
                if (sourceSession != null) {
                    sessionDao.reassignOrders(c, sourceSession.getTableSessionId(), destinationSession.getTableSessionId());
                    sessionDao.updateStatus(c, sourceSession.getTableSessionId(), "CLOSED", true);
                }
                tableDao.setMergedInto(c, sourceTableId, destinationTableId, "OCCUPIED");
                tableDao.updateStatus(c, destinationTableId, "OCCUPIED");
                c.commit();
                return destinationSession.getTableSessionId();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally { c.setAutoCommit(true); }
        }
    }

    public void unmergeTable(int branchId, int sourceTableId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                DiningTable source = tableDao.findById(c, sourceTableId);
                if (source == null || source.getBranchId() != branchId || !source.isMerged()) {
                    throw new IllegalArgumentException("Bàn này không thuộc nhóm ghép.");
                }
                tableDao.setMergedInto(c, sourceTableId, null, "EMPTY");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally { c.setAutoCommit(true); }
        }
    }
}
