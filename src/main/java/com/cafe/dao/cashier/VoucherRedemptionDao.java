package com.cafe.dao.cashier;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class VoucherRedemptionDao {

    public void insert(Connection conn, int voucherId, int billId, BigDecimal discountApplied) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO payment.VoucherRedemption(VoucherId, BillId, DiscountApplied) VALUES (?,?,?)")) {
            ps.setInt(1, voucherId);
            ps.setInt(2, billId);
            ps.setBigDecimal(3, discountApplied);
            ps.executeUpdate();
        }
    }
}
