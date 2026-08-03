package com.cafe.dao.cashier;

import com.cafe.model.BillLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy cập các sales.OrderItem đã được gắn vào bill.
 */
public class BillLineDao {

    /**
     * Gắn dòng vào bill và chốt tiền trong cùng một UPDATE nguyên tử.
     * Modifier là phụ thu trên mỗi ly, nên phải nhân tổng PriceDelta với Quantity.
     */
    public void insert(Connection conn, int billId, int orderItemId) throws SQLException {
        final String sql =
                "UPDATE oi SET oi.BillId=b.BillId, " +
                "oi.BilledAmount=CAST((oi.UnitPrice + ISNULL((" +
                " SELECT SUM(oim.PriceDelta) FROM sales.OrderItemModifier oim " +
                " WHERE oim.OrderItemId=oi.OrderItemId),0))*oi.Quantity AS DECIMAL(12,2)) " +
                "FROM sales.OrderItem oi " +
                "JOIN payment.Bill b ON b.BillId=? AND b.BranchId=oi.BranchId AND b.Status='UNPAID' " +
                "WHERE oi.OrderItemId=? AND oi.BillId IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            ps.setInt(2, orderItemId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Bill và OrderItem không cùng chi nhánh hoặc dòng đã được chốt bill.");
            }
        }
    }

    /** Chuyển dòng sang bill khác nhưng giữ nguyên BilledAmount đã snapshot. */
    public void reassign(Connection conn, int orderItemId, int newBillId) throws SQLException {
        final String sql =
                "UPDATE oi SET oi.BillId=b.BillId " +
                "FROM sales.OrderItem oi " +
                "JOIN payment.Bill b ON b.BillId=? AND b.BranchId=oi.BranchId AND b.Status='UNPAID' " +
                "JOIN payment.Bill oldBill ON oldBill.BillId=oi.BillId AND oldBill.Status='UNPAID' " +
                "WHERE oi.OrderItemId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newBillId);
            ps.setInt(2, orderItemId);
            if (ps.executeUpdate() != 1) throw new SQLException("Không thể chuyển dòng sang bill đích.");
        }
    }

    /** Void bill phải nhả đồng thời BillId và BilledAmount để giữ check lifecycle. */
    public int deleteByBill(Connection conn, int billId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sales.OrderItem SET BillId=NULL,BilledAmount=NULL WHERE BillId=?")) {
            ps.setInt(1, billId);
            return ps.executeUpdate();
        }
    }

    public boolean existsForOrderItem(Connection conn, int orderItemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM sales.OrderItem WHERE OrderItemId=? AND BillId IS NOT NULL")) {
            ps.setInt(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public List<BillLine> findByBill(Connection conn, int billId) throws SQLException {
        final String sql =
                "SELECT oi.BillId,oi.BranchId,oi.OrderItemId,oi.BilledAmount AS Amount,oi.ProductNameAtOrder AS ProductName," +
                "oi.Quantity,oi.Status,oi.Note,mods.Selections FROM sales.OrderItem oi " +
                "OUTER APPLY (SELECT STRING_AGG(oim.ModifierOptionNameAtOrder, N', ') AS Selections " +
                "FROM sales.OrderItemModifier oim WHERE oim.OrderItemId=oi.OrderItemId) mods " +
                "WHERE oi.BillId=? ORDER BY oi.OrderItemId";
        List<BillLine> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public BillLine findById(Connection conn, int orderItemId) throws SQLException {
        final String sql =
                "SELECT BillId,BranchId,OrderItemId," +
                "BilledAmount AS Amount,NULL AS ProductName,Quantity,Status,NULL AS Note,NULL AS Selections " +
                "FROM sales.OrderItem WHERE OrderItemId=? AND BillId IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int countByBill(Connection conn, int billId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM sales.OrderItem WHERE BillId=?")) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private BillLine map(ResultSet rs) throws SQLException {
        BillLine item = new BillLine();
        item.setBillId(rs.getInt("BillId"));
        item.setBranchId(rs.getInt("BranchId"));
        item.setOrderItemId(rs.getInt("OrderItemId"));
        item.setAmount(rs.getBigDecimal("Amount"));
        item.setProductName(rs.getString("ProductName"));
        item.setQuantity(rs.getInt("Quantity"));
        item.setStatus(rs.getString("Status"));
        item.setSelections(rs.getString("Selections"));
        item.setNote(rs.getString("Note"));
        return item;
    }
}
