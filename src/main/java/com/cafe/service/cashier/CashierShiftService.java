package com.cafe.service.cashier;

import com.cafe.common.BusinessDay;
import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.config.Tx;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.cashier.CashierShiftDao;
import com.cafe.dao.shared.OutboxEventDao;
import com.cafe.model.CashierShift;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** C1 · CashierShiftService — mở/đóng ca thu ngân. */
public class CashierShiftService {

    private final CashierShiftDao dao;
    private final BillDao billDao;
    private final OutboxEventDao outboxEventDao;

    public CashierShiftService() { this(new CashierShiftDao(), new BillDao(), new OutboxEventDao()); }
    public CashierShiftService(CashierShiftDao dao, BillDao billDao, OutboxEventDao outboxEventDao) {
        this.dao = java.util.Objects.requireNonNull(dao);
        this.billDao = java.util.Objects.requireNonNull(billDao);
        this.outboxEventDao = java.util.Objects.requireNonNull(outboxEventDao);
    }

    /** Mở ca (idempotent: nếu đã có ca mở thì trả về ca đó). */
    public int openShift(int branchId, int cashierId, BigDecimal openingCash) throws SQLException {
        CashierCashReconciliation.requireValidMoney(openingCash, "Quỹ đầu ca");
        return Tx.call(c -> {
            int id = openShift(c, branchId, cashierId, openingCash);
            return id;
        });
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

    public void closeShift(int shiftId, int cashierId, int branchId, BigDecimal closingCash,
                           boolean handoverConfirmed)
            throws SQLException {
        Tx.run(c -> {
            closeShift(c, shiftId, cashierId, branchId, closingCash, handoverConfirmed);
        });
    }

    void closeShift(Connection c, int shiftId, int cashierId, int branchId, BigDecimal closingCash,
                    boolean handoverConfirmed)
            throws SQLException {
        CashierShift open = dao.findOpenByCashierForUpdate(c, cashierId, branchId);
        if (open == null || open.getCashierShiftId() != shiftId || open.getBranchId() != branchId) {
            throw new IllegalStateException("Không tìm thấy ca két đang mở của bạn.");
        }
        BigDecimal cashRevenue = dao.sumPaidCashForClose(c, shiftId);
        CashierCashReconciliation.requireMatchingClosingCash(
                closingCash, open.getOpeningCash(), cashRevenue);
        CashierShiftDao.PendingHandover pending = dao.pendingHandoverForClose(c, branchId);
        requireHandoverConfirmed(pending, handoverConfirmed);
        if (pending.totalOrderCount() > 0) {
            outboxEventDao.insert(c, EventType.CASHIER_SHIFT_HANDOVER, String.valueOf(shiftId), branchId,
                    "{\"shiftId\":" + shiftId
                            + ",\"cashierId\":" + cashierId
                            + ",\"readyOrders\":" + pending.readyOrderCount()
                            + ",\"inProgressOrders\":" + pending.inProgressOrderCount() + "}");
        }
        if (dao.close(c, shiftId, closingCash) != 1) {
            throw new IllegalStateException("Ca đã được kết thúc bởi thao tác khác.");
        }
    }

    static void requireHandoverConfirmed(CashierShiftDao.PendingHandover pending,
                                         boolean handoverConfirmed) {
        if (pending != null && pending.totalOrderCount() > 0 && !handoverConfirmed) {
            throw new IllegalArgumentException(
                    "Còn " + pending.totalOrderCount()
                            + " đơn chưa thu tiền. Hãy xác nhận bàn giao cho ca sau trước khi kết ca.");
        }
    }

    public CashierShift getCurrentShift(int cashierId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            CashierShift shift = dao.findOpenByCashier(c, cashierId, branchId);
            if (shift != null) {
                dao.fillReport(c, shift);
                dao.fillPendingHandover(c, shift);
            }
            return shift;
        }
    }

    public CashierShift getShiftReport(int shiftId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            CashierShift s = dao.findByIdAndBranch(c, shiftId, branchId);
            if (s != null) dao.fillReport(c, s);
            return s;
        }
    }

    public List<CashierShift> getShiftList(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return dao.findByBranch(c, branchId); }
    }

    /** R1/R2 · Tổng doanh thu đã thu (PAID) hôm nay của chi nhánh. */
    public BigDecimal getTodayRevenue(int branchId) throws SQLException {
        LocalDate today = BusinessDay.todayVn();
        LocalDateTime fromUtc = BusinessDay.vnDayStartUtc(today);
        LocalDateTime toUtc = BusinessDay.vnDayEndExclusiveUtc(today);
        try (Connection c = DBConnection.getConnection()) {
            return billDao.sumPaidBetween(c, branchId, fromUtc, toUtc);
        }
    }

    /** R1/R2 · Số hoá đơn đã thu hôm nay = "số đơn đã thực hiện". */
    public int getTodayBillCount(int branchId) throws SQLException {
        LocalDate today = BusinessDay.todayVn();
        LocalDateTime fromUtc = BusinessDay.vnDayStartUtc(today);
        LocalDateTime toUtc = BusinessDay.vnDayEndExclusiveUtc(today);
        try (Connection c = DBConnection.getConnection()) {
            return billDao.countPaidBetween(c, branchId, fromUtc, toUtc);
        }
    }

    private static String openShiftConflictMessage(CashierShift open) {
        String cashier = open.getCashierName() == null || open.getCashierName().isBlank()
                ? "thu ngân khác"
                : open.getCashierName();
        return "Chi nhánh còn ca thu ngân #" + open.getCashierShiftId() + " của " + cashier
                + " chưa kết. Vui lòng kết ca cũ hoặc nhờ Quản lý xử lý trước khi bắt đầu ca mới.";
    }
}
