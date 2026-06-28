package com.cafe.dao.cashier;

import com.cafe.model.DiningTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DiningTableDao {

    /** Sơ đồ bàn: mỗi bàn kèm phiên OPEN hiện tại (nếu có) + số món đang phục vụ. */
    public List<DiningTable> findFloorMap(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT dt.DiningTableId, dt.BranchId, dt.TableNumber, dt.QrCode, dt.Status, " +
            "       ts.TableSessionId AS ActiveSessionId, " +
            "       (SELECT COUNT(*) FROM sales.OrderItem oi " +
            "          JOIN sales.Orders o ON o.OrderId=oi.OrderId " +
            "        WHERE o.TableSessionId=ts.TableSessionId AND oi.Status<>'CANCELLED') AS ItemCount " +
            "FROM sales.DiningTable dt " +
            "LEFT JOIN sales.TableSession ts ON ts.DiningTableId=dt.DiningTableId AND ts.Status='OPEN' " +
            "WHERE dt.BranchId=? ORDER BY dt.TableNumber";
        List<DiningTable> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DiningTable t = new DiningTable();
                    t.setDiningTableId(rs.getInt("DiningTableId"));
                    t.setBranchId(rs.getInt("BranchId"));
                    t.setTableNumber(rs.getString("TableNumber"));
                    t.setQrCode(rs.getString("QrCode"));
                    t.setStatus(rs.getString("Status"));
                    int sid = rs.getInt("ActiveSessionId");
                    if (!rs.wasNull()) { t.setActiveSessionId(sid); t.setActiveItemCount(rs.getInt("ItemCount")); }
                    out.add(t);
                }
            }
        }
        return out;
    }

    public DiningTable findById(Connection conn, int id) throws SQLException {
        final String sql = "SELECT DiningTableId, BranchId, TableNumber, QrCode, Status FROM sales.DiningTable WHERE DiningTableId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                DiningTable t = new DiningTable();
                t.setDiningTableId(rs.getInt("DiningTableId"));
                t.setBranchId(rs.getInt("BranchId"));
                t.setTableNumber(rs.getString("TableNumber"));
                t.setQrCode(rs.getString("QrCode"));
                t.setStatus(rs.getString("Status"));
                return t;
            }
        }
    }

    /** Tìm bàn theo mã QR (khách quét) — Phase 6. */
    public DiningTable findByQrCode(Connection conn, String qrCode) throws SQLException {
        final String sql = "SELECT DiningTableId, BranchId, TableNumber, QrCode, Status FROM sales.DiningTable WHERE QrCode=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, qrCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                DiningTable t = new DiningTable();
                t.setDiningTableId(rs.getInt("DiningTableId"));
                t.setBranchId(rs.getInt("BranchId"));
                t.setTableNumber(rs.getString("TableNumber"));
                t.setQrCode(rs.getString("QrCode"));
                t.setStatus(rs.getString("Status"));
                return t;
            }
        }
    }

    public void updateStatus(Connection conn, int tableId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sales.DiningTable SET Status=? WHERE DiningTableId=?")) {
            ps.setString(1, status);
            ps.setInt(2, tableId);
            ps.executeUpdate();
        }
    }
}
