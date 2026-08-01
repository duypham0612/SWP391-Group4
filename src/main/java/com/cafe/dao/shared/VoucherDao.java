package com.cafe.dao.shared;

import com.cafe.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class VoucherDao {

    private static final String SELECT =
        "SELECT v.VoucherId, v.Code, v.DiscountType, v.DiscountValue, v.MinOrderAmount, v.Scope, v.BranchId, " +
        "       v.StartAtUtc, v.EndAtUtc, v.UsageLimit, v.UsedCount, v.IsActive, b.Name AS BranchName " +
        "FROM payment.Voucher v LEFT JOIN org.Branch b ON v.BranchId = b.BranchId ";

    public List<Voucher> findAll(Connection conn) throws SQLException {
        List<Voucher> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY v.VoucherId DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public Voucher findByCode(Connection conn, String code) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE v.Code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Voucher findByCodeForUpdate(Connection conn, String code) throws SQLException {
        String lockedSelect = SELECT.replace(
                "FROM payment.Voucher v ",
                "FROM payment.Voucher v WITH (UPDLOCK, HOLDLOCK) ");
        try (PreparedStatement ps = conn.prepareStatement(lockedSelect + "WHERE v.Code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean existsByCode(Connection conn, String code, int excludeVoucherId) throws SQLException {
        String sql = "SELECT 1 FROM payment.Voucher WHERE UPPER(Code) = UPPER(?) AND VoucherId <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, excludeVoucherId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Tăng lượt dùng voucher, KHÔNG bao giờ vượt {@code UsageLimit}.
     *
     * <p>Điều kiện {@code UsedCount < UsageLimit} nằm ngay trong câu UPDATE nên việc
     * kiểm tra và tăng là một thao tác nguyên tử — hai Cashier thanh toán đồng thời
     * không thể cùng vượt hạn mức (kiểm ở {@code VoucherService} rồi mới tăng thì vẫn
     * hở giữa hai bước).
     *
     * @return true nếu đã tăng; false nếu voucher vừa hết lượt (không có dòng nào khớp).
     */
    public boolean incrementUsed(Connection conn, int voucherId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE payment.Voucher SET UsedCount = UsedCount + 1 " +
                "WHERE VoucherId = ? AND (UsageLimit IS NULL OR UsedCount < UsageLimit)")) {
            ps.setInt(1, voucherId);
            return ps.executeUpdate() > 0;
        }
    }

    public Voucher findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE v.VoucherId = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Voucher findByIdForUpdate(Connection conn, int id) throws SQLException {
        String lockedSelect = SELECT.replace(
                "FROM payment.Voucher v ",
                "FROM payment.Voucher v WITH (UPDLOCK, HOLDLOCK) ");
        try (PreparedStatement ps = conn.prepareStatement(lockedSelect + "WHERE v.VoucherId = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Voucher v) throws SQLException {
        final String sql = "INSERT INTO payment.Voucher(Code, DiscountType, DiscountValue, MinOrderAmount, Scope, BranchId, " +
                "StartAtUtc, EndAtUtc, UsageLimit, IsActive) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, v);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(Connection conn, Voucher v) throws SQLException {
        final String sql = "UPDATE payment.Voucher SET Code=?, DiscountType=?, DiscountValue=?, MinOrderAmount=?, Scope=?, " +
                "BranchId=?, StartAtUtc=?, EndAtUtc=?, UsageLimit=?, IsActive=? WHERE VoucherId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = fill(ps, v);
            ps.setInt(i, v.getVoucherId());
            ps.executeUpdate();
        }
    }

    public void updateActive(Connection conn, int id, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE payment.Voucher SET IsActive=? WHERE VoucherId=?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Set 10 cột chung (insert/update), trả về index kế tiếp. */
    private int fill(PreparedStatement ps, Voucher v) throws SQLException {
        ps.setString(1, v.getCode());
        ps.setString(2, v.getDiscountType());
        ps.setBigDecimal(3, v.getDiscountValue());
        ps.setBigDecimal(4, v.getMinOrderAmount());
        ps.setString(5, v.getScope());
        if (v.getBranchId() == null) ps.setNull(6, Types.INTEGER); else ps.setInt(6, v.getBranchId());
        if (v.getStartAtUtc() == null) ps.setNull(7, Types.TIMESTAMP); else ps.setTimestamp(7, Timestamp.valueOf(v.getStartAtUtc()));
        if (v.getEndAtUtc() == null) ps.setNull(8, Types.TIMESTAMP); else ps.setTimestamp(8, Timestamp.valueOf(v.getEndAtUtc()));
        if (v.getUsageLimit() == null) ps.setNull(9, Types.INTEGER); else ps.setInt(9, v.getUsageLimit());
        ps.setBoolean(10, v.isActive());
        return 11;
    }

    private Voucher map(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setVoucherId(rs.getInt("VoucherId"));
        v.setCode(rs.getString("Code"));
        v.setDiscountType(rs.getString("DiscountType"));
        v.setDiscountValue(rs.getBigDecimal("DiscountValue"));
        v.setMinOrderAmount(rs.getBigDecimal("MinOrderAmount"));
        v.setScope(rs.getString("Scope"));
        int b = rs.getInt("BranchId");
        v.setBranchId(rs.wasNull() ? null : b);
        Timestamp s = rs.getTimestamp("StartAtUtc");
        v.setStartAtUtc(s == null ? null : s.toLocalDateTime());
        Timestamp e = rs.getTimestamp("EndAtUtc");
        v.setEndAtUtc(e == null ? null : e.toLocalDateTime());
        int ul = rs.getInt("UsageLimit");
        v.setUsageLimit(rs.wasNull() ? null : ul);
        v.setUsedCount(rs.getInt("UsedCount"));
        v.setActive(rs.getBoolean("IsActive"));
        v.setBranchName(rs.getString("BranchName"));
        return v;
    }
}
