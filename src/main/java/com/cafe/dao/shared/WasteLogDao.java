package com.cafe.dao.shared;

import com.cafe.model.WasteLog;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WasteLogDao {

    public int insert(Connection conn, int branchId, int ingredientId, BigDecimal qty,
                      String wasteType, String reason, int loggedBy) throws SQLException {
        final String sql = "INSERT INTO inventory.WasteLog(BranchId, IngredientId, Quantity, WasteType, Reason, LoggedBy) " +
                "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, ingredientId);
            ps.setBigDecimal(3, qty);
            ps.setString(4, wasteType);
            if (reason == null) ps.setNull(5, java.sql.Types.NVARCHAR); else ps.setString(5, reason);
            ps.setInt(6, loggedBy);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    private static final String SELECT =
        "SELECT wl.WasteLogId, wl.BranchId, wl.IngredientId, wl.Quantity, wl.WasteType, wl.Reason, wl.LoggedBy, wl.LoggedAt, wl.Status, wl.VoidedAt, " +
        "       i.Name AS IngName, i.Unit AS IngUnit, i.IngredientType, u.FullName AS LoggedByName " +
        "FROM inventory.WasteLog wl " +
        "JOIN catalog.Ingredient i ON i.IngredientId=wl.IngredientId " +
        "JOIN iam.[User] u ON u.UserId=wl.LoggedBy ";

    public List<WasteLog> findByBranch(Connection conn, int branchId) throws SQLException {
        List<WasteLog> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE wl.BranchId=? ORDER BY wl.LoggedAt DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<WasteLog> findByBranchBetween(Connection conn, int branchId, LocalDateTime fromUtc, LocalDateTime toUtc) throws SQLException {
        List<WasteLog> out = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE wl.BranchId=? ");
        if (fromUtc != null) sql.append("AND wl.LoggedAt>=? ");
        if (toUtc != null) sql.append("AND wl.LoggedAt<? ");
        sql.append("ORDER BY wl.LoggedAt DESC");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, branchId);
            if (fromUtc != null) ps.setTimestamp(idx++, Timestamp.valueOf(fromUtc));
            if (toUtc != null) ps.setTimestamp(idx, Timestamp.valueOf(toUtc));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public WasteLog findById(Connection conn, int wasteLogId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE wl.WasteLogId=?")) {
            ps.setInt(1, wasteLogId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public void update(Connection conn, int wasteLogId, BigDecimal qty, String wasteType, String reason) throws SQLException {
        final String sql = "UPDATE inventory.WasteLog SET Quantity=?, WasteType=?, Reason=? WHERE WasteLogId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, qty);
            ps.setString(2, wasteType);
            if (reason == null) ps.setNull(3, java.sql.Types.NVARCHAR); else ps.setString(3, reason);
            ps.setInt(4, wasteLogId);
            ps.executeUpdate();
        }
    }

    /** Đánh dấu VOIDED (kèm VoidedAt). KHÔNG hard-delete — tồn hoàn qua txn bù. */
    public void updateStatus(Connection conn, int wasteLogId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.WasteLog SET Status=?, VoidedAt=CASE WHEN ?='VOIDED' THEN SYSUTCDATETIME() ELSE NULL END WHERE WasteLogId=?")) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, wasteLogId);
            ps.executeUpdate();
        }
    }

    private WasteLog map(ResultSet rs) throws SQLException {
        WasteLog w = new WasteLog();
        w.setWasteLogId(rs.getInt("WasteLogId"));
        w.setBranchId(rs.getInt("BranchId"));
        w.setIngredientId(rs.getInt("IngredientId"));
        w.setQuantity(rs.getBigDecimal("Quantity"));
        w.setWasteType(rs.getString("WasteType"));
        w.setReason(rs.getString("Reason"));
        w.setLoggedBy(rs.getInt("LoggedBy"));
        Timestamp la = rs.getTimestamp("LoggedAt");
        if (la != null) w.setLoggedAt(la.toLocalDateTime());
        w.setStatus(rs.getString("Status"));
        Timestamp va = rs.getTimestamp("VoidedAt");
        if (va != null) w.setVoidedAt(va.toLocalDateTime());
        w.setIngredientName(rs.getString("IngName"));
        w.setIngredientUnit(rs.getString("IngUnit"));
        w.setIngredientType(rs.getString("IngredientType"));
        w.setLoggedByName(rs.getString("LoggedByName"));
        return w;
    }
}
