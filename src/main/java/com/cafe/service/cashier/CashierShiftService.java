package com.cafe.service.cashier;

import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.CashierShiftDao;
import com.cafe.model.CashierShift;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** C1 · CashierShiftService — mở/đóng ca thu ngân. */
public class CashierShiftService {

    private final CashierShiftDao dao = new CashierShiftDao();

    /** Mở ca (idempotent: nếu đã có ca mở thì trả về ca đó). */
    public int openShift(int branchId, int cashierId, BigDecimal openingCash) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                CashierShift open = dao.findOpenByCashier(c, cashierId);
                int id = open != null ? open.getCashierShiftId() : dao.insertOpen(c, branchId, cashierId, openingCash);
                c.commit();
                return id;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void closeShift(int shiftId, BigDecimal closingCash) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { dao.close(c, shiftId, closingCash); c.commit(); }
            catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public CashierShift getCurrentShift(int cashierId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findOpenByCashier(c, cashierId); }
    }

    public CashierShift getShiftReport(int shiftId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            CashierShift s = dao.findById(c, shiftId);
            if (s != null) dao.fillReport(c, s);
            return s;
        }
    }

    public List<CashierShift> getShiftList(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByBranch(c, branchId); }
    }
}
