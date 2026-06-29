package com.cafe.dao.shared;

import com.cafe.model.PrepBatch;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PrepBatchDao {

    public int insert(Connection conn, int branchId, int preppedIngredientId, BigDecimal qtyProduced, int madeBy) throws SQLException {
        final String sql = "INSERT INTO inventory.PrepBatch(BranchId, PreppedIngredientId, QuantityProduced, MadeBy) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, preppedIngredientId);
            ps.setBigDecimal(3, qtyProduced);
            ps.setInt(4, madeBy);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    private static final String SELECT =
        "SELECT pb.PrepBatchId, pb.BranchId, pb.PreppedIngredientId, pb.QuantityProduced, pb.MadeBy, pb.MadeAt, pb.ExpiresAt, pb.Status, pb.VoidedAt, " +
        "       i.Name AS IngName, i.Unit AS IngUnit, u.FullName AS MadeByName " +
        "FROM inventory.PrepBatch pb " +
        "JOIN catalog.Ingredient i ON i.IngredientId=pb.PreppedIngredientId " +
        "JOIN iam.[User] u ON u.UserId=pb.MadeBy ";

    public List<PrepBatch> findByBranch(Connection conn, int branchId) throws SQLException {
        List<PrepBatch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE pb.BranchId=? ORDER BY pb.MadeAt DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public PrepBatch findById(Connection conn, int prepBatchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE pb.PrepBatchId=?")) {
            ps.setInt(1, prepBatchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Đánh dấu trạng thái (CANCELLED kèm VoidedAt). KHÔNG hard-delete — tồn hoàn qua txn bù. */
    public void updateStatus(Connection conn, int prepBatchId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.PrepBatch SET Status=?, VoidedAt=CASE WHEN ?='CANCELLED' THEN SYSUTCDATETIME() ELSE NULL END WHERE PrepBatchId=?")) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, prepBatchId);
            ps.executeUpdate();
        }
    }

    public void updateQuantity(Connection conn, int prepBatchId, BigDecimal qtyProduced) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE inventory.PrepBatch SET QuantityProduced=? WHERE PrepBatchId=?")) {
            ps.setBigDecimal(1, qtyProduced);
            ps.setInt(2, prepBatchId);
            ps.executeUpdate();
        }
    }

    private PrepBatch map(ResultSet rs) throws SQLException {
        PrepBatch b = new PrepBatch();
        b.setPrepBatchId(rs.getInt("PrepBatchId"));
        b.setBranchId(rs.getInt("BranchId"));
        b.setPreppedIngredientId(rs.getInt("PreppedIngredientId"));
        b.setQuantityProduced(rs.getBigDecimal("QuantityProduced"));
        b.setMadeBy(rs.getInt("MadeBy"));
        Timestamp ma = rs.getTimestamp("MadeAt");
        if (ma != null) b.setMadeAt(ma.toLocalDateTime());
        Timestamp ea = rs.getTimestamp("ExpiresAt");
        if (ea != null) b.setExpiresAt(ea.toLocalDateTime());
        b.setStatus(rs.getString("Status"));
        Timestamp va = rs.getTimestamp("VoidedAt");
        if (va != null) b.setVoidedAt(va.toLocalDateTime());
        b.setPreppedIngredientName(rs.getString("IngName"));
        b.setPreppedIngredientUnit(rs.getString("IngUnit"));
        b.setMadeByName(rs.getString("MadeByName"));
        return b;
    }
}
