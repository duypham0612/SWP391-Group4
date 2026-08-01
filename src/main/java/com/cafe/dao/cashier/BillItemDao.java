package com.cafe.dao.cashier;

import com.cafe.model.BillItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BillItemDao {

    public void insert(Connection conn, int billId, int orderItemId, BigDecimal amount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO payment.BillItem(BillId,BranchId,OrderItemId,Amount) " +
                "SELECT b.BillId,b.BranchId,oi.OrderItemId,? FROM payment.Bill b " +
                "JOIN sales.OrderItem oi ON oi.OrderItemId=? AND oi.BranchId=b.BranchId " +
                "WHERE b.BillId=?")) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, orderItemId);
            ps.setInt(3, billId);
            if (ps.executeUpdate() != 1) throw new SQLException("Bill và OrderItem không cùng chi nhánh.");
        }
    }

    /** Đổi dòng sang bill khác (tách/gộp). Giữ UNIQUE(OrderItemId). */
    public void reassign(Connection conn, int billItemId, int newBillId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE payment.BillItem SET BillId=? WHERE BillItemId=?")) {
            ps.setInt(1, newBillId);
            ps.setInt(2, billItemId);
            ps.executeUpdate();
        }
    }

    /**
     * Nhả các dòng của một bill bị huỷ.
     *
     * <p>Bắt buộc khi void bill: {@code UQ_BillItem_OrderItem} giữ "1 dòng đơn chỉ thuộc
     * 1 bill", nên nếu chỉ đổi {@code Bill.Status='VOID'} mà để lại BillItem thì các món
     * đó không bao giờ lên được bill mới — phiên bàn không đóng được. SQL Server không cho
     * filtered index tham chiếu {@code Bill.Status} nên phải nhả ở tầng ứng dụng.
     */
    public int deleteByBill(Connection conn, int billId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM payment.BillItem WHERE BillId=?")) {
            ps.setInt(1, billId);
            return ps.executeUpdate();
        }
    }

    public boolean existsForOrderItem(Connection conn, int orderItemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM payment.BillItem WHERE OrderItemId=?")) {
            ps.setInt(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public List<BillItem> findByBill(Connection conn, int billId) throws SQLException {
        final String sql =
            "SELECT bi.BillItemId,bi.BillId,bi.BranchId,bi.OrderItemId,bi.Amount,oi.ProductNameAtOrder AS ProductName,oi.Quantity,oi.Status " +
            "FROM payment.BillItem bi " +
            "JOIN sales.OrderItem oi ON oi.OrderItemId=bi.OrderItemId " +
            "WHERE bi.BillId=? ORDER BY bi.BillItemId";
        List<BillItem> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BillItem b = new BillItem();
                    b.setBillItemId(rs.getInt("BillItemId"));
                    b.setBillId(rs.getInt("BillId"));
                    b.setBranchId(rs.getInt("BranchId"));
                    b.setOrderItemId(rs.getInt("OrderItemId"));
                    b.setAmount(rs.getBigDecimal("Amount"));
                    b.setProductName(rs.getString("ProductName"));
                    b.setQuantity(rs.getInt("Quantity"));
                    b.setStatus(rs.getString("Status"));
                    out.add(b);
                }
            }
        }
        return out;
    }

    public BillItem findById(Connection conn, int billItemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT BillItemId,BillId,BranchId,OrderItemId,Amount FROM payment.BillItem WHERE BillItemId=?")) {
            ps.setInt(1, billItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                BillItem b = new BillItem();
                b.setBillItemId(rs.getInt("BillItemId"));
                b.setBillId(rs.getInt("BillId"));
                b.setBranchId(rs.getInt("BranchId"));
                b.setOrderItemId(rs.getInt("OrderItemId"));
                b.setAmount(rs.getBigDecimal("Amount"));
                return b;
            }
        }
    }

    public int countByBill(Connection conn, int billId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM payment.BillItem WHERE BillId=?")) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }
}
