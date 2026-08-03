package com.cafe.dao.cashier;

import com.cafe.model.Voucher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/** Atomic persistence for promotion vouchers applied to bills. */
public final class VoucherRedemptionDao {
    public Voucher findByCodeForUpdate(Connection conn, String code) throws SQLException {
        String sql = "SELECT VoucherId,Code,Name,DiscountType,DiscountValue,MaxDiscountAmount,MinOrderAmount,UsageLimit,UsedCount,StartsAt,EndsAt,IsActive "
                + "FROM promotion.Voucher WITH (UPDLOCK,HOLDLOCK) WHERE CodeKey=UPPER(LTRIM(RTRIM(?)))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public String findCodeByBill(Connection conn, int billId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT CodeAtRedemption FROM promotion.VoucherRedemption WHERE BillId=?")) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    public void replaceRedemption(Connection conn, Voucher voucher, int billId, BigDecimal discount, Integer userId)
            throws SQLException {
        releaseForBill(conn, billId);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT promotion.VoucherRedemption(VoucherId,BillId,CodeAtRedemption,DiscountAmount,RedeemedBy) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, voucher.getVoucherId());
            ps.setInt(2, billId);
            ps.setString(3, voucher.getCode());
            ps.setBigDecimal(4, discount);
            if (userId == null) ps.setNull(5, java.sql.Types.INTEGER); else ps.setInt(5, userId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE promotion.Voucher SET UsedCount=UsedCount+1 WHERE VoucherId=? AND (UsageLimit IS NULL OR UsedCount<UsageLimit)")) {
            ps.setInt(1, voucher.getVoucherId());
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Voucher đã hết lượt sử dụng.");
        }
    }

    /** Remove the bill's voucher and return one usage slot, if any. */
    public void releaseForBill(Connection conn, int billId) throws SQLException {
        Integer voucherId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT VoucherId FROM promotion.VoucherRedemption WITH (UPDLOCK,HOLDLOCK) WHERE BillId=?")) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) voucherId = rs.getInt(1); }
        }
        if (voucherId == null) return;
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM promotion.VoucherRedemption WHERE BillId=?")) {
            ps.setInt(1, billId); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE promotion.Voucher SET UsedCount=CASE WHEN UsedCount>0 THEN UsedCount-1 ELSE 0 END WHERE VoucherId=?")) {
            ps.setInt(1, voucherId); ps.executeUpdate();
        }
    }

    private Voucher map(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setVoucherId(rs.getInt("VoucherId")); v.setCode(rs.getString("Code")); v.setName(rs.getString("Name"));
        v.setDiscountType(rs.getString("DiscountType")); v.setDiscountValue(rs.getBigDecimal("DiscountValue"));
        v.setMaxDiscountAmount(rs.getBigDecimal("MaxDiscountAmount")); v.setMinOrderAmount(rs.getBigDecimal("MinOrderAmount"));
        int limit = rs.getInt("UsageLimit"); v.setUsageLimit(rs.wasNull() ? null : limit); v.setUsedCount(rs.getInt("UsedCount"));
        Timestamp starts = rs.getTimestamp("StartsAt"), ends = rs.getTimestamp("EndsAt");
        v.setStartsAt(starts == null ? null : starts.toLocalDateTime()); v.setEndsAt(ends == null ? null : ends.toLocalDateTime());
        v.setActive(rs.getBoolean("IsActive"));
        return v;
    }
}
