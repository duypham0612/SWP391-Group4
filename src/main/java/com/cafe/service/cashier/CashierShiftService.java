package com.cafe.service.cashier;

import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.cashier.CashierShiftDao;
import com.cafe.model.CashierShift;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** C1 · CashierShiftService — mở/đóng ca thu ngân. */
public class CashierShiftService {

    private final CashierShiftDao dao = new CashierShiftDao();
    private final BillDao billDao = new BillDao();

    /** Mở ca (idempotent: nếu đã có ca mở thì trả về ca đó). */
    public int openShift(int branchId, int cashierId, BigDecimal openingCash) throws SQLException {
        CashierCashReconciliation.requireValidMoney(openingCash, "Quỹ đầu ca");
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = openShift(c, branchId, cashierId, openingCash);
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /**
     * Lõi mở két dùng được trong transaction của caller. Mỗi chi nhánh chỉ có một két mở;
     * ca của chính Cashier được trả lại để thao tác lặp vẫn idempotent.
     */
    public int openShift(Connection c, int branchId, int cashierId, BigDecimal openingCash)
            throws SQLException {
        CashierCashReconciliation.requireValidMoney(openingCash, "Quỹ đầu ca");
        dao.acquireBranchOpenLock(c, branchId);
        List<CashierShift> openShifts = dao.findOpenByBranchForUpdate(c, branchId);

        CashierShift ownOpenShift = selectOwnOpenShift(openShifts, cashierId);
        if (ownOpenShift != null) return ownOpenShift.getCashierShiftId();
        return dao.insertOpen(c, branchId, cashierId, openingCash);
    }

    static CashierShift selectOwnOpenShift(List<CashierShift> openShifts, int cashierId) {
        if (openShifts.size() > 1) {
            throw new IllegalStateException(
                    "Chi nhánh đang có nhiều ca thu ngân chưa kết. "
                            + "Vui lòng nhờ Quản lý đối soát và xử lý các ca cũ trước khi bắt đầu ca mới.");
        }

        CashierShift ownOpenShift = null;
        for (CashierShift open : openShifts) {
            if (open.getCashierId() == cashierId) {
                ownOpenShift = open;
                continue;
            }
            throw new IllegalStateException(openShiftConflictMessage(open));
        }
        return ownOpenShift;
    }

    public void closeShift(int shiftId, int cashierId, int branchId, BigDecimal closingCash)
            throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                closeShift(c, shiftId, cashierId, branchId, closingCash);
                c.commit();
            }
            catch (SQLException e) { c.rollback(); throw e; }
            catch (RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    void closeShift(Connection c, int shiftId, int cashierId, int branchId, BigDecimal closingCash)
            throws SQLException {
        CashierShift open = dao.findOpenByCashierForUpdate(c, cashierId);
        if (open == null || open.getCashierShiftId() != shiftId || open.getBranchId() != branchId) {
            throw new IllegalStateException("Không tìm thấy ca két đang mở của bạn.");
        }
        BigDecimal cashRevenue = dao.sumPaidCashForClose(c, shiftId);
        CashierCashReconciliation.requireMatchingClosingCash(
                closingCash, open.getOpeningCash(), cashRevenue);
        if (dao.close(c, shiftId, closingCash) != 1) {
            throw new IllegalStateException("Ca đã được kết thúc bởi thao tác khác.");
        }
    }

    public CashierShift getCurrentShift(int cashierId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            CashierShift shift = dao.findOpenByCashier(c, cashierId);
            if (shift != null) dao.fillReport(c, shift);
            return shift;
        }
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

    /** R1/R2 · Tổng doanh thu đã thu (PAID) hôm nay của chi nhánh. */
    public BigDecimal getTodayRevenue(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return billDao.sumPaidToday(c, branchId); }
    }

    /** R1/R2 · Số hoá đơn đã thu hôm nay = "số đơn đã thực hiện". */
    public int getTodayBillCount(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return billDao.countPaidToday(c, branchId); }
    }

    private static String openShiftConflictMessage(CashierShift open) {
        String cashier = open.getCashierName() == null || open.getCashierName().isBlank()
                ? "thu ngân khác"
                : open.getCashierName();
        return "Chi nhánh còn ca thu ngân #" + open.getCashierShiftId() + " của " + cashier
                + " chưa kết. Vui lòng kết ca cũ hoặc nhờ Quản lý xử lý trước khi bắt đầu ca mới.";
    }
}
