package com.cafe.dao.sales;

import com.cafe.model.DiningTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DiningTableDao {

    /** Sơ đồ bàn: trạng thái lưu trên DiningTable và số món thuộc các đơn chưa thanh toán. */
    public List<DiningTable> findFloorMap(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT dt.DiningTableId, dt.BranchId, dt.TableNumber, dt.QrCode, dt.Status, " +
            "       (SELECT COUNT(*) FROM sales.OrderItem oi " +
            "          JOIN sales.SalesOrder o ON o.OrderId=oi.OrderId " +
            "          LEFT JOIN payment.Bill b ON b.BillId=oi.BillId " +
            "        WHERE o.DiningTableId=dt.DiningTableId AND o.BranchId=dt.BranchId " +
            "          AND oi.Status<>'CANCELLED' AND (oi.BillId IS NULL OR b.Status='UNPAID')) AS ItemCount " +
            "FROM sales.DiningTable dt " +
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
                    t.setActiveItemCount(rs.getInt("ItemCount"));
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

    public DiningTable findByIdForUpdate(Connection conn, int id) throws SQLException {
        final String sql = "SELECT DiningTableId, BranchId, TableNumber, QrCode, Status " +
                "FROM sales.DiningTable WITH (UPDLOCK, HOLDLOCK) WHERE DiningTableId=?";
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

    public List<DiningTable> findOccupiedByBranch(Connection conn, int branchId) throws SQLException {
        List<DiningTable> out = new ArrayList<>();
        for (DiningTable table : findFloorMap(conn, branchId)) {
            if ("OCCUPIED".equals(table.getStatus())) out.add(table);
        }
        return out;
    }

    /** Đơn chưa thanh toán: có dòng chưa gắn bill hoặc đang nằm trên bill UNPAID. */
    public boolean hasUnpaidOrders(Connection conn, int tableId, int branchId) throws SQLException {
        final String sql =
                "SELECT TOP(1) 1 FROM sales.SalesOrder o " +
                "JOIN sales.OrderItem oi ON oi.OrderId=o.OrderId " +
                "LEFT JOIN payment.Bill b ON b.BillId=oi.BillId " +
                "WHERE o.DiningTableId=? AND o.BranchId=? AND oi.Status<>'CANCELLED' " +
                "AND (oi.BillId IS NULL OR b.Status='UNPAID')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Chuyển các đơn còn nợ từ bàn nguồn sang bàn đích khi gộp bàn. */
    public int reassignUnpaidOrders(Connection conn, int sourceTableId, int targetTableId,
                                    int branchId) throws SQLException {
        final String sql =
                "UPDATE o SET DiningTableId=? FROM sales.SalesOrder o " +
                "WHERE o.DiningTableId=? AND o.BranchId=? AND EXISTS (" +
                " SELECT 1 FROM sales.OrderItem oi LEFT JOIN payment.Bill b ON b.BillId=oi.BillId " +
                " WHERE oi.OrderId=o.OrderId AND oi.Status<>'CANCELLED' " +
                " AND (oi.BillId IS NULL OR b.Status='UNPAID'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, targetTableId);
            ps.setInt(2, sourceTableId);
            ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }
}
