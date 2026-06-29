package com.cafe.dao.cashier;

import com.cafe.model.Bill;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class BillDao {

    private static final String SELECT =
        "SELECT b.BillId, b.BranchId, b.TableSessionId, b.CashierShiftId, b.Subtotal, b.VatAmount, b.DiscountAmount, " +
        "       b.TotalAmount, b.VoucherId, b.PaymentMethod, b.Status, b.PaidAt, b.CreatedAt, " +
        "       dt.TableNumber, v.Code AS VoucherCode " +
        "FROM payment.Bill b " +
        "LEFT JOIN sales.TableSession ts ON ts.TableSessionId=b.TableSessionId " +
        "LEFT JOIN sales.DiningTable  dt ON dt.DiningTableId=ts.DiningTableId " +
        "LEFT JOIN payment.Voucher    v  ON v.VoucherId=b.VoucherId ";

    public int insert(Connection conn, int branchId, Integer sessionId, Integer shiftId) throws SQLException {
        final String sql = "INSERT INTO payment.Bill(BranchId, TableSessionId, CashierShiftId, Status) VALUES (?,?,?,'UNPAID')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            if (sessionId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, sessionId);
            if (shiftId == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, shiftId);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public Bill findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE b.BillId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Các bill của 1 phiên (mọi trạng thái) — để dựng màn checkout. */
    public List<Bill> findBySession(Connection conn, int sessionId) throws SQLException {
        List<Bill> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE b.TableSessionId=? ORDER BY b.BillId")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public List<Bill> findUnpaidBySession(Connection conn, int sessionId) throws SQLException {
        List<Bill> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE b.TableSessionId=? AND b.Status='UNPAID' ORDER BY b.BillId")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Lịch sử bill của chi nhánh (mới nhất trước). */
    public List<Bill> findByBranch(Connection conn, int branchId, int limit) throws SQLException {
        List<Bill> out = new ArrayList<>();
        final String sql = SELECT.replaceFirst("SELECT ", "SELECT TOP " + limit + " ") + "WHERE b.BranchId=? ORDER BY b.CreatedAt DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Doanh thu PAID hôm nay của chi nhánh (M1 dashboard manager). */
    public BigDecimal sumPaidToday(Connection conn, int branchId) throws SQLException {
        final String sql = "SELECT ISNULL(SUM(TotalAmount),0) AS Rev FROM payment.Bill " +
                "WHERE BranchId=? AND Status='PAID' AND CAST(PaidAt AS DATE)=CAST(SYSUTCDATETIME() AS DATE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getBigDecimal("Rev") : BigDecimal.ZERO; }
        }
    }

    /** Lịch sử bill trong 1 ca thu ngân (mới nhất trước) — C6 lọc theo ca. */
    public List<Bill> findByShift(Connection conn, int shiftId, int limit) throws SQLException {
        List<Bill> out = new ArrayList<>();
        final String sql = SELECT.replaceFirst("SELECT ", "SELECT TOP " + limit + " ") + "WHERE b.CashierShiftId=? ORDER BY b.CreatedAt DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shiftId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Cập nhật số tiền + voucher sau khi gắn dòng / áp voucher. */
    public void updateAmounts(Connection conn, int billId, BigDecimal subtotal, BigDecimal discount,
                              BigDecimal vat, BigDecimal total, Integer voucherId) throws SQLException {
        final String sql = "UPDATE payment.Bill SET Subtotal=?, DiscountAmount=?, VatAmount=?, TotalAmount=?, VoucherId=? WHERE BillId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, subtotal);
            ps.setBigDecimal(2, discount);
            ps.setBigDecimal(3, vat);
            ps.setBigDecimal(4, total);
            if (voucherId == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, voucherId);
            ps.setInt(6, billId);
            ps.executeUpdate();
        }
    }

    /** Thanh toán: chỉ chuyển UNPAID→PAID (chống double-pay bằng WHERE Status). Trả số dòng đổi. */
    public int markPaid(Connection conn, int billId, String method) throws SQLException {
        final String sql = "UPDATE payment.Bill SET Status='PAID', PaymentMethod=?, PaidAt=SYSUTCDATETIME() " +
                "WHERE BillId=? AND Status='UNPAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, method);
            ps.setInt(2, billId);
            return ps.executeUpdate();
        }
    }

    public int markVoid(Connection conn, int billId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE payment.Bill SET Status='VOID' WHERE BillId=? AND Status<>'PAID'")) {
            ps.setInt(1, billId);
            return ps.executeUpdate();
        }
    }

    /** Hoàn hoá đơn ĐÃ thanh toán: chỉ PAID→REFUND (chống hoàn 2 lần bằng WHERE Status). */
    public int markRefund(Connection conn, int billId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE payment.Bill SET Status='REFUND' WHERE BillId=? AND Status='PAID'")) {
            ps.setInt(1, billId);
            return ps.executeUpdate();
        }
    }

    private Bill map(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setBillId(rs.getInt("BillId"));
        b.setBranchId(rs.getInt("BranchId"));
        int ts = rs.getInt("TableSessionId"); if (!rs.wasNull()) b.setTableSessionId(ts);
        int sh = rs.getInt("CashierShiftId"); if (!rs.wasNull()) b.setCashierShiftId(sh);
        b.setSubtotal(rs.getBigDecimal("Subtotal"));
        b.setVatAmount(rs.getBigDecimal("VatAmount"));
        b.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
        b.setTotalAmount(rs.getBigDecimal("TotalAmount"));
        int vc = rs.getInt("VoucherId"); if (!rs.wasNull()) b.setVoucherId(vc);
        b.setPaymentMethod(rs.getString("PaymentMethod"));
        b.setStatus(rs.getString("Status"));
        Timestamp pa = rs.getTimestamp("PaidAt"); if (pa != null) b.setPaidAt(pa.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("CreatedAt"); if (ca != null) b.setCreatedAt(ca.toLocalDateTime());
        b.setTableNumber(rs.getString("TableNumber"));
        b.setVoucherCode(rs.getString("VoucherCode"));
        return b;
    }
}
