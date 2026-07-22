package com.cafe.dao.cashier;

import com.cafe.model.DiningTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DiningTableDao {

    private static final String TABLE_SELECT =
        "SELECT dt.DiningTableId,dt.BranchId,dt.TableNumber,dt.QrCode,dt.Status," +
        "dt.Capacity,dt.IsVisible,dt.MergedIntoTableId,parent.TableNumber AS MergedIntoTableNumber " +
        "FROM sales.DiningTable dt " +
        "LEFT JOIN sales.DiningTable parent ON parent.DiningTableId=dt.MergedIntoTableId ";

    /** Floor map with active session resolved through the merge destination. */
    public List<DiningTable> findFloorMap(Connection conn, int branchId, boolean includeHidden) throws SQLException {
        final String sql =
            "SELECT dt.DiningTableId,dt.BranchId,dt.TableNumber,dt.QrCode,dt.Status," +
            "dt.Capacity,dt.IsVisible,dt.MergedIntoTableId,parent.TableNumber AS MergedIntoTableNumber," +
            "dt.Capacity + CASE WHEN dt.MergedIntoTableId IS NULL THEN ISNULL((" +
            "  SELECT SUM(child.Capacity) FROM sales.DiningTable child " +
            "  WHERE child.MergedIntoTableId=dt.DiningTableId AND child.IsVisible=1),0) ELSE 0 END AS EffectiveCapacity," +
            "ts.TableSessionId AS ActiveSessionId," +
            "(SELECT COUNT(*) FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId " +
            " WHERE o.TableSessionId=ts.TableSessionId AND oi.Status<>'CANCELLED') AS ItemCount " +
            "FROM sales.DiningTable dt " +
            "LEFT JOIN sales.DiningTable parent ON parent.DiningTableId=dt.MergedIntoTableId " +
            "LEFT JOIN sales.TableSession ts ON ts.DiningTableId=COALESCE(dt.MergedIntoTableId,dt.DiningTableId) AND ts.Status='OPEN' " +
            "WHERE dt.BranchId=? AND (?=1 OR dt.IsVisible=1) ORDER BY dt.TableNumber";
        List<DiningTable> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setBoolean(2, includeHidden);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapFloor(rs));
            }
        }
        return out;
    }

    public List<DiningTable> findFloorMap(Connection conn, int branchId) throws SQLException {
        return findFloorMap(conn, branchId, false);
    }

    public DiningTable findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(TABLE_SELECT + "WHERE dt.DiningTableId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Hidden tables are deliberately invalid QR entry points. */
    public DiningTable findByQrCode(Connection conn, String qrCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(TABLE_SELECT + "WHERE dt.QrCode=? AND dt.IsVisible=1")) {
            ps.setString(1, qrCode);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insert(Connection conn, int branchId, String tableNumber, int capacity, String qrCode) throws SQLException {
        final String sql = "INSERT INTO sales.DiningTable(BranchId,TableNumber,QrCode,Status,Capacity,IsVisible) " +
                "VALUES (?,?,?,'EMPTY',?,1)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setString(2, tableNumber);
            ps.setString(3, qrCode);
            ps.setInt(4, capacity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : 0; }
        }
    }

    public int updateDetails(Connection conn, int tableId, int branchId, String tableNumber, int capacity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sales.DiningTable SET TableNumber=?,Capacity=? WHERE DiningTableId=? AND BranchId=?")) {
            ps.setString(1, tableNumber);
            ps.setInt(2, capacity);
            ps.setInt(3, tableId);
            ps.setInt(4, branchId);
            return ps.executeUpdate();
        }
    }

    public int updateVisibility(Connection conn, int tableId, int branchId, boolean visible) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sales.DiningTable SET IsVisible=? WHERE DiningTableId=? AND BranchId=?")) {
            ps.setBoolean(1, visible);
            ps.setInt(2, tableId);
            ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }

    public void updateStatus(Connection conn, int tableId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sales.DiningTable SET Status=? WHERE DiningTableId=?")) {
            ps.setString(1, status);
            ps.setInt(2, tableId);
            ps.executeUpdate();
        }
    }

    public void setMergedInto(Connection conn, int sourceTableId, Integer destinationTableId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sales.DiningTable SET MergedIntoTableId=?,Status=? WHERE DiningTableId=?")) {
            if (destinationTableId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, destinationTableId);
            ps.setString(2, status);
            ps.setInt(3, sourceTableId);
            ps.executeUpdate();
        }
    }

    public void releaseMergedChildren(Connection conn, int destinationTableId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sales.DiningTable SET MergedIntoTableId=NULL,Status='EMPTY' WHERE MergedIntoTableId=?")) {
            ps.setInt(1, destinationTableId);
            ps.executeUpdate();
        }
    }

    public boolean hasMergedChildren(Connection conn, int tableId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 WHERE EXISTS(SELECT 1 FROM sales.DiningTable WHERE MergedIntoTableId=?)")) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private DiningTable mapFloor(ResultSet rs) throws SQLException {
        DiningTable table = map(rs);
        table.setEffectiveCapacity(rs.getInt("EffectiveCapacity"));
        int sessionId = rs.getInt("ActiveSessionId");
        if (!rs.wasNull()) {
            table.setActiveSessionId(sessionId);
            table.setActiveItemCount(rs.getInt("ItemCount"));
        }
        return table;
    }

    private DiningTable map(ResultSet rs) throws SQLException {
        DiningTable table = new DiningTable();
        table.setDiningTableId(rs.getInt("DiningTableId"));
        table.setBranchId(rs.getInt("BranchId"));
        table.setTableNumber(rs.getString("TableNumber"));
        table.setQrCode(rs.getString("QrCode"));
        table.setStatus(rs.getString("Status"));
        table.setCapacity(rs.getInt("Capacity"));
        table.setVisible(rs.getBoolean("IsVisible"));
        int mergedInto = rs.getInt("MergedIntoTableId");
        if (!rs.wasNull()) table.setMergedIntoTableId(mergedInto);
        table.setMergedIntoTableNumber(rs.getString("MergedIntoTableNumber"));
        return table;
    }
}
