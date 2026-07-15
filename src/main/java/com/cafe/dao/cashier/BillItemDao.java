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
                "INSERT INTO payment.BillItem(BillId, OrderItemId, Amount) VALUES (?,?,?)")) {
            ps.setInt(1, billId);
            ps.setInt(2, orderItemId);
            ps.setBigDecimal(3, amount);
            ps.executeUpdate();
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

    public boolean existsForOrderItem(Connection conn, int orderItemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM payment.BillItem WHERE OrderItemId=?")) {
            ps.setInt(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public List<BillItem> findByBill(Connection conn, int billId) throws SQLException {
        final String sql =
            "SELECT bi.BillItemId, bi.BillId, bi.OrderItemId, bi.Amount, p.Name AS ProductName, oi.Quantity, oi.Status " +
            "FROM payment.BillItem bi " +
            "JOIN sales.OrderItem oi ON oi.OrderItemId=bi.OrderItemId " +
            "JOIN catalog.Product  p ON p.ProductId=oi.ProductId " +
            "WHERE bi.BillId=? ORDER BY bi.BillItemId";
        List<BillItem> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BillItem b = new BillItem();
                    b.setBillItemId(rs.getInt("BillItemId"));
                    b.setBillId(rs.getInt("BillId"));
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
                "SELECT BillItemId, BillId, OrderItemId, Amount FROM payment.BillItem WHERE BillItemId=?")) {
            ps.setInt(1, billItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                BillItem b = new BillItem();
                b.setBillItemId(rs.getInt("BillItemId"));
                b.setBillId(rs.getInt("BillId"));
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
