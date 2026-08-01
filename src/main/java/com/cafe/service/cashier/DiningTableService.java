package com.cafe.service.cashier;

import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.shared.OutboxEventDao;
import com.cafe.model.DiningTable;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Nghiệp vụ bàn sau khi bỏ TableSession; tableId là aggregate duy nhất của luồng dine-in. */
public class DiningTableService {
    private final DiningTableDao tableDao;
    private final OutboxEventDao outboxDao;

    public DiningTableService() { this(new DiningTableDao(), new OutboxEventDao()); }
    public DiningTableService(DiningTableDao tableDao, OutboxEventDao outboxDao) {
        this.tableDao = java.util.Objects.requireNonNull(tableDao);
        this.outboxDao = java.util.Objects.requireNonNull(outboxDao);
    }

    public Map<Integer, LocalDateTime> getPendingOpenRequests(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return outboxDao.findPendingOpenRequests(conn, branchId);
        }
    }

    public Map<Integer, String> getPendingSignals(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return outboxDao.findPendingSignals(conn, branchId);
        }
    }

    public List<DiningTable> getFloorMap(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return tableDao.findFloorMap(conn, branchId); }
    }

    public DiningTable getTable(int tableId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return tableDao.findById(conn, tableId); }
    }

    public List<DiningTable> getOpenTables(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return tableDao.findOccupiedByBranch(conn, branchId);
        }
    }

    public int openTable(int branchId, int tableId, Integer cashierId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                DiningTable table = tableDao.findByIdForUpdate(conn, tableId);
                if (table == null || table.getBranchId() != branchId) {
                    throw new BusinessException("Bàn không tồn tại hoặc không thuộc chi nhánh hiện tại.");
                }
                if (!"OCCUPIED".equals(table.getStatus())) {
                    tableDao.updateStatus(conn, tableId, "OCCUPIED");
                }
                outboxDao.markOpenRequestsProcessed(conn, tableId);
                conn.commit();
                return tableId;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean closeTableIfNoUnpaidOrders(int tableId, int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                DiningTable table = tableDao.findByIdForUpdate(conn, tableId);
                if (table == null || table.getBranchId() != branchId
                        || !"OCCUPIED".equals(table.getStatus())
                        || tableDao.hasUnpaidOrders(conn, tableId, branchId)) {
                    conn.rollback();
                    return false;
                }
                tableDao.updateStatus(conn, tableId, "EMPTY");
                outboxDao.markSignalsProcessed(conn, tableId);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean acknowledgeSignals(int branchId, int tableId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                DiningTable table = tableDao.findById(conn, tableId);
                if (table == null || table.getBranchId() != branchId
                        || !"OCCUPIED".equals(table.getStatus())) {
                    conn.rollback();
                    return false;
                }
                int changed = outboxDao.markSignalsProcessed(conn, tableId);
                conn.commit();
                return changed > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean setTableStatus(int branchId, int tableId, String status) throws SQLException {
        if (!"EMPTY".equals(status) && !"OCCUPIED".equals(status)) return false;
        if ("OCCUPIED".equals(status)) {
            openTable(branchId, tableId, null);
            return true;
        }
        return closeTableIfNoUnpaidOrders(tableId, branchId);
    }

    public boolean mergeTables(int branchId, int sourceTableId, int targetTableId) throws SQLException {
        if (sourceTableId == targetTableId) return false;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int first = Math.min(sourceTableId, targetTableId);
                int second = Math.max(sourceTableId, targetTableId);
                DiningTable firstTable = tableDao.findByIdForUpdate(conn, first);
                DiningTable secondTable = tableDao.findByIdForUpdate(conn, second);
                DiningTable source = sourceTableId == first ? firstTable : secondTable;
                DiningTable target = targetTableId == first ? firstTable : secondTable;
                if (source == null || target == null
                        || source.getBranchId() != branchId || target.getBranchId() != branchId
                        || !"OCCUPIED".equals(source.getStatus())
                        || !"OCCUPIED".equals(target.getStatus())) {
                    conn.rollback();
                    return false;
                }
                tableDao.reassignUnpaidOrders(conn, sourceTableId, targetTableId, branchId);
                tableDao.updateStatus(conn, sourceTableId, "EMPTY");
                outboxDao.markSignalsProcessed(conn, sourceTableId);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
