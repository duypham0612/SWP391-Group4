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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BillDao {

    private static final String SELECT =
        "SELECT b.BillId, b.BranchId, b.CashierShiftId, b.Subtotal, b.VatAmount, b.DiscountAmount, " +
        "       b.TotalAmount, b.RoundingAdjustment, b.PaidAmount, b.CashTendered, b.CashChange, " +
        "       b.PaymentMethod, b.Status, b.PaidAt, b.CreatedAt, " +
        "       rel.DiningTableId, dt.TableNumber " +
        "FROM payment.Bill b " +
        "OUTER APPLY (SELECT TOP(1) o.DiningTableId FROM sales.OrderItem oi " +
        " JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId " +
        " WHERE oi.BillId=b.BillId AND o.DiningTableId IS NOT NULL " +
        " ORDER BY oi.OrderItemId) rel " +
        "LEFT JOIN sales.DiningTable dt ON dt.DiningTableId=rel.DiningTableId ";

    public int insert(Connection conn, int branchId, Integer shiftId) throws SQLException {
        final String sql = "INSERT INTO payment.Bill(BranchId,CashierShiftId,Status) VALUES (?,?,'UNPAID')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            if (shiftId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, shiftId);
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

    /** Khóa bill đến hết transaction thanh toán để số tiền không đổi giữa chừng. */
    public Bill findByIdForUpdate(Connection conn, int id) throws SQLException {
        String lockedSelect = SELECT.replace(
                "FROM payment.Bill b ",
                "FROM payment.Bill b WITH (UPDLOCK, HOLDLOCK) ");
        try (PreparedStatement ps = conn.prepareStatement(lockedSelect + "WHERE b.BillId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Các bill chứa dòng của các đơn tại một bàn. */
    public List<Bill> findByTable(Connection conn, int tableId) throws SQLException {
        List<Bill> out = new ArrayList<>();
        final String sql = SELECT + "WHERE b.Status='UNPAID' AND EXISTS (SELECT 1 FROM sales.OrderItem oi " +
                "JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId " +
                "WHERE oi.BillId=b.BillId AND o.DiningTableId=?) ORDER BY b.BillId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public List<Bill> findUnpaidByTable(Connection conn, int tableId) throws SQLException {
        List<Bill> out = new ArrayList<>();
        final String sql = SELECT + "WHERE b.Status='UNPAID' AND EXISTS (" +
                "SELECT 1 FROM sales.OrderItem oi JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId " +
                "WHERE oi.BillId=b.BillId AND o.DiningTableId=?) ORDER BY b.BillId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các bill chứa món của một đơn. */
    public List<Bill> findByOrder(Connection conn, int orderId) throws SQLException {
        List<Bill> out = new ArrayList<>();
        final String sql = SELECT +
                "WHERE EXISTS (SELECT 1 FROM sales.OrderItem oi " +
                "WHERE oi.BillId=b.BillId AND oi.OrderId=?) ORDER BY b.BillId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
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
    public BigDecimal sumPaidToday(Connection conn, int branchId,
                                   LocalDateTime fromUtc, LocalDateTime toUtc) throws SQLException {
        final String sql = "SELECT ISNULL(SUM(TotalAmount),0) AS Rev FROM payment.Bill " +
                "WHERE BranchId=? AND Status='PAID' AND PaidAt>=? AND PaidAt<?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setTimestamp(2, Timestamp.valueOf(fromUtc));
            ps.setTimestamp(3, Timestamp.valueOf(toUtc));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getBigDecimal("Rev") : BigDecimal.ZERO; }
        }
    }

    /** Doanh thu hóa đơn trong khoảng UTC nửa mở [from, to), dùng cho ngày lịch Việt Nam. */
    public BigDecimal sumPaidBetween(Connection conn, int branchId,
                                     LocalDateTime fromUtc, LocalDateTime toUtc) throws SQLException {
        final String sql = "SELECT ISNULL(SUM(TotalAmount),0) AS Rev FROM payment.Bill " +
                "WHERE BranchId=? AND Status='PAID' AND PaidAt>=? AND PaidAt<?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setTimestamp(2, Timestamp.valueOf(fromUtc));
            ps.setTimestamp(3, Timestamp.valueOf(toUtc));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("Rev") : BigDecimal.ZERO;
            }
        }
    }

    /** Số hóa đơn đã thu trong khoảng UTC nửa mở [from, to). */
    public int countPaidBetween(Connection conn, int branchId,
                                LocalDateTime fromUtc, LocalDateTime toUtc) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM payment.Bill " +
                "WHERE BranchId=? AND Status='PAID' AND PaidAt>=? AND PaidAt<?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setTimestamp(2, Timestamp.valueOf(fromUtc));
            ps.setTimestamp(3, Timestamp.valueOf(toUtc));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Status bill gắn trực tiếp với các món của đơn mang đi. */
    public List<String> findStatusesByOrder(Connection conn, int orderId) throws SQLException {
        List<String> out = new ArrayList<>();
        final String sql = "SELECT DISTINCT b.Status FROM payment.Bill b " +
                "JOIN sales.OrderItem oi ON oi.BillId=b.BillId WHERE oi.OrderId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getString(1)); }
        }
        return out;
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

    /** Cập nhật số tiền sau khi gắn/chuyển dòng hoặc nhập giảm giá thủ công. */
    public int updateAmounts(Connection conn, int billId, BigDecimal subtotal, BigDecimal discount,
                             BigDecimal vat, BigDecimal total) throws SQLException {
        final String sql = "UPDATE payment.Bill SET Subtotal=?,DiscountAmount=?,VatAmount=?,TotalAmount=? " +
                "WHERE BillId=? AND Status='UNPAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, subtotal);
            ps.setBigDecimal(2, discount);
            ps.setBigDecimal(3, vat);
            ps.setBigDecimal(4, total);
            ps.setInt(5, billId);
            return ps.executeUpdate();
        }
    }

    /** Thanh toán: chỉ chuyển UNPAID→PAID (chống double-pay bằng WHERE Status). Trả số dòng đổi. */
    public int markPaid(Connection conn, int billId, String method, int shiftId,
                        BigDecimal roundingAdjustment, BigDecimal paidAmount,
                        BigDecimal cashTendered, BigDecimal cashChange) throws SQLException {
        final String sql = "UPDATE b SET Status='PAID', PaymentMethod=?, " +
                "RoundingAdjustment=?, PaidAmount=?, CashTendered=?, CashChange=?, " +
                "CashierShiftId=cs.CashierShiftId, PaidAt=SYSUTCDATETIME() " +
                "FROM payment.Bill b " +
                "JOIN payment.CashierShift cs WITH (UPDLOCK, HOLDLOCK) " +
                "ON cs.CashierShiftId=? AND cs.BranchId=b.BranchId AND cs.ClosedAt IS NULL " +
                "WHERE b.BillId=? AND b.Status='UNPAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, method);
            ps.setBigDecimal(2, roundingAdjustment);
            ps.setBigDecimal(3, paidAmount);
            if (cashTendered == null) ps.setNull(4, Types.DECIMAL); else ps.setBigDecimal(4, cashTendered);
            if (cashChange == null) ps.setNull(5, Types.DECIMAL); else ps.setBigDecimal(5, cashChange);
            ps.setInt(6, shiftId);
            ps.setInt(7, billId);
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

    private Bill map(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setBillId(rs.getInt("BillId"));
        b.setBranchId(rs.getInt("BranchId"));
        int tableId = rs.getInt("DiningTableId"); if (!rs.wasNull()) b.setDiningTableId(tableId);
        int sh = rs.getInt("CashierShiftId"); if (!rs.wasNull()) b.setCashierShiftId(sh);
        b.setSubtotal(rs.getBigDecimal("Subtotal"));
        b.setVatAmount(rs.getBigDecimal("VatAmount"));
        b.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
        b.setTotalAmount(rs.getBigDecimal("TotalAmount"));
        b.setRoundingAdjustment(rs.getBigDecimal("RoundingAdjustment"));
        b.setPaidAmount(rs.getBigDecimal("PaidAmount"));
        b.setCashTendered(rs.getBigDecimal("CashTendered"));
        b.setCashChange(rs.getBigDecimal("CashChange"));
        b.setPaymentMethod(rs.getString("PaymentMethod"));
        b.setStatus(rs.getString("Status"));
        Timestamp pa = rs.getTimestamp("PaidAt"); if (pa != null) b.setPaidAt(pa.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("CreatedAt"); if (ca != null) b.setCreatedAt(ca.toLocalDateTime());
        b.setTableNumber(rs.getString("TableNumber"));
        return b;
    }
}
