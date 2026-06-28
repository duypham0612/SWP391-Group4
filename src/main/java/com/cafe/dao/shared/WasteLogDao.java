package com.cafe.dao.shared;

import com.cafe.model.WasteLog;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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

    public List<WasteLog> findByBranch(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT wl.WasteLogId, wl.BranchId, wl.IngredientId, wl.Quantity, wl.WasteType, wl.Reason, wl.LoggedBy, wl.LoggedAt, " +
            "       i.Name AS IngName, i.Unit AS IngUnit, u.FullName AS LoggedByName " +
            "FROM inventory.WasteLog wl " +
            "JOIN catalog.Ingredient i ON i.IngredientId=wl.IngredientId " +
            "JOIN iam.[User] u ON u.UserId=wl.LoggedBy " +
            "WHERE wl.BranchId=? ORDER BY wl.LoggedAt DESC";
        List<WasteLog> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
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
                    w.setIngredientName(rs.getString("IngName"));
                    w.setIngredientUnit(rs.getString("IngUnit"));
                    w.setLoggedByName(rs.getString("LoggedByName"));
                    out.add(w);
                }
            }
        }
        return out;
    }
}
