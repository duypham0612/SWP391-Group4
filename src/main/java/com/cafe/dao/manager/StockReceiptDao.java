package com.cafe.dao.manager;

import com.cafe.model.StockReceipt;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class StockReceiptDao {

    private static final String SELECT =
        "SELECT r.StockReceiptId, r.BranchId, r.SupplierId, r.ReceivedBy, r.ReceiptDate, r.Status, r.TotalCost, r.Note, " +
        "       s.Name AS SupplierName, u.FullName AS ReceivedByName " +
        "FROM inventory.StockReceipt r " +
        "LEFT JOIN inventory.Supplier s ON r.SupplierId = s.SupplierId " +
        "LEFT JOIN iam.[User] u ON r.ReceivedBy = u.UserId ";

    public List<StockReceipt> findByBranch(Connection conn, int branchId) throws SQLException {
        List<StockReceipt> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE r.BranchId=? ORDER BY r.StockReceiptId DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public StockReceipt findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE r.StockReceiptId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insertDraft(Connection conn, StockReceipt r) throws SQLException {
        final String sql = "INSERT INTO inventory.StockReceipt(BranchId, SupplierId, ReceivedBy, Status, Note) VALUES (?,?,?,'DRAFT',?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getBranchId());
            if (r.getSupplierId() == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, r.getSupplierId());
            ps.setInt(3, r.getReceivedBy());
            ps.setString(4, r.getNote());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public void confirm(Connection conn, int id, BigDecimal totalCost) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.StockReceipt SET Status='CONFIRMED', TotalCost=? WHERE StockReceiptId=? AND Status='DRAFT'")) {
            ps.setBigDecimal(1, totalCost);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void cancel(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.StockReceipt SET Status='CANCELLED' WHERE StockReceiptId=? AND Status='DRAFT'")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private StockReceipt map(ResultSet rs) throws SQLException {
        StockReceipt r = new StockReceipt();
        r.setStockReceiptId(rs.getInt("StockReceiptId"));
        r.setBranchId(rs.getInt("BranchId"));
        int sid = rs.getInt("SupplierId");
        r.setSupplierId(rs.wasNull() ? null : sid);
        r.setReceivedBy(rs.getInt("ReceivedBy"));
        Timestamp d = rs.getTimestamp("ReceiptDate");
        r.setReceiptDate(d == null ? null : d.toLocalDateTime());
        r.setStatus(rs.getString("Status"));
        r.setTotalCost(rs.getBigDecimal("TotalCost"));
        r.setNote(rs.getString("Note"));
        r.setSupplierName(rs.getString("SupplierName"));
        r.setReceivedByName(rs.getString("ReceivedByName"));
        return r;
    }
}
