package com.mycoffee.dao;

import com.mycoffee.context.DBContext;
import com.mycoffee.model.Voucher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VoucherDAO {

    public List<Voucher> getValidVouchersForOrder(int orderId) {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT DISTINCT v.VoucherCode, v.DiscountValue, v.IsPercentage, v.MaxDiscount, v.UsageLimit, v.UsedCount " +
                "FROM Vouchers v " +
                "WHERE v.IsActive = 1 " +
                "  AND v.UsedCount < v.UsageLimit " +
                "  AND GETDATE() BETWEEN v.StartDate AND v.EndDate " +
                "  AND v.MinOrderValue <= (SELECT COALESCE(TotalAmount, 0) FROM Orders WHERE OrderID = ?) " +
                "  AND (v.ProductID IS NULL OR EXISTS ( " +
                "      SELECT 1 FROM OrderDetails od WHERE od.OrderID = ? AND od.ProductID = v.ProductID " +
                "  ))";

        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Voucher v = new Voucher();
                    v.setVoucherCode(rs.getString("VoucherCode"));
                    v.setDiscountValue(rs.getDouble("DiscountValue"));
                    v.setIsPercentage(rs.getBoolean("IsPercentage"));
                    Object maxDiscObj = rs.getObject("MaxDiscount");
                    v.setMaxDiscount(maxDiscObj != null ? ((Number) maxDiscObj).doubleValue() : null);
                    v.setUsageLimit(rs.getInt("UsageLimit"));
                    v.setUsedCount(rs.getInt("UsedCount"));
                    list.add(v);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public Voucher getVoucherByCode(String code) {
        String sql = "SELECT TOP 1 DiscountValue, IsPercentage, MaxDiscount FROM Vouchers WHERE VoucherCode = ? AND IsActive = 1 AND UsedCount < UsageLimit";
        try (Connection conn = new DBContext().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Voucher v = new Voucher();
                    v.setDiscountValue(rs.getDouble("DiscountValue"));
                    v.setIsPercentage(rs.getBoolean("IsPercentage"));
                    Object maxDiscObj = rs.getObject("MaxDiscount");
                    v.setMaxDiscount(maxDiscObj != null ? ((Number) maxDiscObj).doubleValue() : null);
                    return v;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }
}