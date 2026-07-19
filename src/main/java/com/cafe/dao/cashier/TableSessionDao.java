package com.cafe.dao.cashier;

import com.cafe.model.TableSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TableSessionDao {

    private static final String SELECT =
        "SELECT ts.TableSessionId, ts.BranchId, ts.DiningTableId, ts.OpenedBy, ts.OpenedAt, ts.ClosedAt, ts.Status, " +
        "       dt.TableNumber " +
        "FROM sales.TableSession ts JOIN sales.DiningTable dt ON dt.DiningTableId=ts.DiningTableId ";

    public TableSession findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE ts.TableSessionId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Phiên OPEN của 1 bàn (nếu có). */
    public TableSession findOpenByTable(Connection conn, int tableId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE ts.DiningTableId=? AND ts.Status='OPEN'")) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<TableSession> findOpenByBranch(Connection conn, int branchId) throws SQLException {
        List<TableSession> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE ts.BranchId=? AND ts.Status='OPEN' ORDER BY dt.TableNumber")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public int insertOpen(Connection conn, int branchId, int tableId, Integer openedBy) throws SQLException {
        final String sql = "INSERT INTO sales.TableSession(BranchId, DiningTableId, OpenedBy, Status) VALUES (?,?,?,'OPEN')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, tableId);
            if (openedBy == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, openedBy);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public void updateStatus(Connection conn, int sessionId, String status, boolean setClosed) throws SQLException {
        final String sql = setClosed
            ? "UPDATE sales.TableSession SET Status=?, ClosedAt=SYSUTCDATETIME() WHERE TableSessionId=?"
            : "UPDATE sales.TableSession SET Status=? WHERE TableSessionId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        }
    }

    /** Đổi mọi đơn của phiên nguồn sang phiên đích (gộp bill). */
    public void reassignOrders(Connection conn, int srcSessionId, int dstSessionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sales.Orders SET TableSessionId=? WHERE TableSessionId=?")) {
            ps.setInt(1, dstSessionId);
            ps.setInt(2, srcSessionId);
            ps.executeUpdate();
        }
    }

    private TableSession map(ResultSet rs) throws SQLException {
        TableSession t = new TableSession();
        t.setTableSessionId(rs.getInt("TableSessionId"));
        t.setBranchId(rs.getInt("BranchId"));
        t.setDiningTableId(rs.getInt("DiningTableId"));
        int ob = rs.getInt("OpenedBy");
        if (!rs.wasNull()) t.setOpenedBy(ob);
        Timestamp oa = rs.getTimestamp("OpenedAt");
        if (oa != null) t.setOpenedAt(oa.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("ClosedAt");
        if (ca != null) t.setClosedAt(ca.toLocalDateTime());
        t.setStatus(rs.getString("Status"));
        t.setTableNumber(rs.getString("TableNumber"));
        return t;
    }
}
