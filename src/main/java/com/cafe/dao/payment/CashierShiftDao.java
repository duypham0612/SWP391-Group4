package com.cafe.dao.payment;

import com.cafe.model.CashierShift;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CashierShiftDao {

    private static final String SELECT =
        "SELECT cs.CashierShiftId, cs.BranchId, cs.CashierId, cs.OpeningCash, cs.ClosingCash, cs.OpenedAt, cs.ClosedAt, " +
        "       u.FullName AS CashierName " +
        "FROM payment.CashierShift cs JOIN iam.UserAccount u ON u.UserId=cs.CashierId ";

    public int insertOpen(Connection conn, int branchId, int cashierId, BigDecimal openingCash) throws SQLException {
        final String sql = "INSERT INTO payment.CashierShift(BranchId, CashierId, OpeningCash) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, cashierId);
            ps.setBigDecimal(3, openingCash == null ? BigDecimal.ZERO : openingCash);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public int close(Connection conn, int shiftId, BigDecimal closingCash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE payment.CashierShift SET ClosingCash=?, ClosedAt=SYSUTCDATETIME() " +
                        "WHERE CashierShiftId=? AND ClosedAt IS NULL")) {
            ps.setBigDecimal(1, closingCash == null ? BigDecimal.ZERO : closingCash);
            ps.setInt(2, shiftId);
            return ps.executeUpdate();
        }
    }

    /** Ca đang mở của 1 thu ngân tại đúng chi nhánh đang thao tác. */
    public CashierShift findOpenByCashier(Connection conn, int cashierId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE cs.CashierId=? AND cs.BranchId=? AND cs.ClosedAt IS NULL")) {
            ps.setInt(1, cashierId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Khóa ca trong transaction kết ca để chống hai request đóng cùng lúc. */
    public CashierShift findOpenByCashierForUpdate(Connection conn, int cashierId, int branchId)
            throws SQLException {
        String lockedSelect = SELECT.replace(
                "FROM payment.CashierShift cs ",
                "FROM payment.CashierShift cs WITH (UPDLOCK, HOLDLOCK) ");
        try (PreparedStatement ps = conn.prepareStatement(
                lockedSelect + "WHERE cs.CashierId=? AND cs.BranchId=? AND cs.ClosedAt IS NULL")) {
            ps.setInt(1, cashierId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Khóa đúng ca theo id trước khi thanh toán; thống nhất thứ tự khóa ca → bill với kết ca. */
    public CashierShift findOpenByIdForUpdate(Connection conn, int shiftId) throws SQLException {
        String lockedSelect = SELECT.replace(
                "FROM payment.CashierShift cs ",
                "FROM payment.CashierShift cs WITH (UPDLOCK, HOLDLOCK) ");
        try (PreparedStatement ps = conn.prepareStatement(
                lockedSelect + "WHERE cs.CashierShiftId=? AND cs.ClosedAt IS NULL")) {
            ps.setInt(1, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Khóa dòng chi nhánh đến hết transaction để serialize mọi thao tác mở/đóng két của branch. */
    public void acquireBranchOpenLock(Connection conn, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT BranchId FROM org.Branch WITH (UPDLOCK, HOLDLOCK) WHERE BranchId=?")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Không tìm thấy chi nhánh để khóa thao tác mở ca: " + branchId);
                }
            }
        }
    }

    /** Tất cả ca đang mở của chi nhánh, được khóa tới hết transaction mở/đóng cưỡng chế. */
    public List<CashierShift> findOpenByBranchForUpdate(Connection conn, int branchId) throws SQLException {
        List<CashierShift> out = new ArrayList<>();
        String lockedSelect = SELECT.replace(
                "FROM payment.CashierShift cs ",
                "FROM payment.CashierShift cs WITH (UPDLOCK, HOLDLOCK) ");
        try (PreparedStatement ps = conn.prepareStatement(
                lockedSelect + "WHERE cs.BranchId=? AND cs.ClosedAt IS NULL "
                        + "ORDER BY cs.OpenedAt, cs.CashierShiftId")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /** Tổng bill tiền mặt đã thu trong ca; khóa tập bill tới khi transaction kết ca hoàn tất. */
    public BigDecimal sumPaidCashForClose(Connection conn, int shiftId) throws SQLException {
        String sql = "SELECT ISNULL(SUM(COALESCE(PaidAmount, TotalAmount)),0) AS CashTotal " +
                "FROM payment.Bill WITH (UPDLOCK, HOLDLOCK) " +
                "WHERE CashierShiftId=? AND Status='PAID' AND PaymentMethod='CASH'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shiftId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("CashTotal") : BigDecimal.ZERO;
            }
        }
    }

    public CashierShift findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE cs.CashierShiftId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public CashierShift findByIdAndBranch(Connection conn, int id, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE cs.CashierShiftId=? AND cs.BranchId=?")) {
            ps.setInt(1, id);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Báo cáo ca: số bill PAID + riêng tổng tiền mặt đã thu. */
    public void fillReport(Connection conn, CashierShift shift) throws SQLException {
        final String sql = "SELECT COUNT(*) AS Cnt, " +
                "ISNULL(SUM(CASE WHEN PaymentMethod='CASH' " +
                "THEN COALESCE(PaidAmount, TotalAmount) ELSE 0 END),0) AS CashTotal " +
                "FROM payment.Bill WHERE CashierShiftId=? AND Status='PAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shift.getCashierShiftId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    shift.setBillCount(rs.getInt("Cnt"));
                    shift.setCashCollected(rs.getBigDecimal("CashTotal"));
                }
            }
        }
    }

    /** Đơn chưa thu phải được Cashier xác nhận bàn giao trước khi đóng ca. */
    public void fillPendingHandover(Connection conn, CashierShift shift) throws SQLException {
        PendingHandover pending = pendingHandover(conn, shift.getBranchId(), false);
        shift.setPendingReadyOrderCount(pending.readyOrderCount());
        shift.setPendingInProgressOrderCount(pending.inProgressOrderCount());
    }

    /** Đọc và khóa tập đơn chưa thu trong transaction kết ca. */
    public PendingHandover pendingHandoverForClose(Connection conn, int branchId) throws SQLException {
        return pendingHandover(conn, branchId, true);
    }

    private PendingHandover pendingHandover(Connection conn, int branchId, boolean lock)
            throws SQLException {
        String orderTable = lock ? "sales.SalesOrder o WITH (UPDLOCK, HOLDLOCK) " : "sales.SalesOrder o ";
        final String sql = "SELECT " +
                "ISNULL(SUM(CASE WHEN o.Status='COMPLETED' THEN 1 ELSE 0 END),0) AS ReadyCount, " +
                "ISNULL(SUM(CASE WHEN o.Status='ACTIVE' THEN 1 ELSE 0 END),0) AS ActiveCount " +
                "FROM " + orderTable +
                "WHERE o.BranchId=? AND o.Status IN ('ACTIVE','COMPLETED') AND EXISTS (" +
                " SELECT 1 FROM sales.OrderItem oi " +
                " LEFT JOIN payment.Bill b ON b.BillId=oi.BillId " +
                " WHERE oi.OrderId=o.OrderId AND oi.Status<>'CANCELLED' " +
                " AND (oi.BillId IS NULL OR b.Status<>'PAID'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new PendingHandover(0, 0);
                return new PendingHandover(rs.getInt("ReadyCount"), rs.getInt("ActiveCount"));
            }
        }
    }

    public List<CashierShift> findByBranch(Connection conn, int branchId) throws SQLException {
        List<CashierShift> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE cs.BranchId=? ORDER BY cs.OpenedAt DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    private CashierShift map(ResultSet rs) throws SQLException {
        CashierShift s = new CashierShift();
        s.setCashierShiftId(rs.getInt("CashierShiftId"));
        s.setBranchId(rs.getInt("BranchId"));
        s.setCashierId(rs.getInt("CashierId"));
        s.setOpeningCash(rs.getBigDecimal("OpeningCash"));
        s.setClosingCash(rs.getBigDecimal("ClosingCash"));
        Timestamp oa = rs.getTimestamp("OpenedAt");
        if (oa != null) s.setOpenedAt(oa.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("ClosedAt");
        if (ca != null) s.setClosedAt(ca.toLocalDateTime());
        s.setCashierName(rs.getString("CashierName"));
        return s;
    }

    public record PendingHandover(int readyOrderCount, int inProgressOrderCount) {
        public int totalOrderCount() {
            return readyOrderCount + inProgressOrderCount;
        }
    }
}
