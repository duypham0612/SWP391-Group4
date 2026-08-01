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
        "SELECT r.StockReceiptId, r.BranchId, r.SupplierId, r.ReceivedBy, r.DocumentDate, r.CreatedAt, r.Status, " +
        "       CASE WHEN costs.LineCount > 0 THEN costs.DetailTotal ELSE r.TotalCost END AS TotalCost, r.Note, " +
        "       s.Name AS SupplierName, u.FullName AS ReceivedByName " +
        "FROM inventory.StockReceipt r " +
        "LEFT JOIN inventory.Supplier s ON r.SupplierId = s.SupplierId " +
        "LEFT JOIN iam.UserAccount u ON r.ReceivedBy = u.UserId " +
        "OUTER APPLY (SELECT COUNT(*) AS LineCount, COALESCE(SUM(d.EnteredQuantity*d.UnitCost),0) AS DetailTotal " +
        "             FROM inventory.StockReceiptDetail d WHERE d.StockReceiptId=r.StockReceiptId) costs ";

    public List<StockReceipt> findByBranch(Connection conn, int branchId) throws SQLException {
        List<StockReceipt> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE r.BranchId=? ORDER BY r.StockReceiptId DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public StockReceipt findById(Connection conn, int id, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE r.StockReceiptId=? AND r.BranchId=?")) {
            ps.setInt(1, id);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Khóa phiếu nháp đến hết transaction trước mọi thao tác sửa/chốt/hủy. */
    public StockReceipt findDraftForUpdate(Connection conn, int id, int branchId) throws SQLException {
        String lockedSelect = SELECT.replace(
                "FROM inventory.StockReceipt r ",
                "FROM inventory.StockReceipt r WITH (UPDLOCK, HOLDLOCK) ");
        try (PreparedStatement ps = conn.prepareStatement(
                lockedSelect + "WHERE r.StockReceiptId=? AND r.BranchId=? AND r.Status='DRAFT'")) {
            ps.setInt(1, id);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insertDraft(Connection conn, StockReceipt r) throws SQLException {
        final String sql = "INSERT INTO inventory.StockReceipt(BranchId, SupplierId, ReceivedBy, Status, Note) "
                + "SELECT ?,?,?, 'DRAFT',? FROM iam.UserAccount u "
                + "WHERE u.UserId=? AND u.BranchId=? AND u.Status='ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getBranchId());
            if (r.getSupplierId() == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, r.getSupplierId());
            ps.setInt(3, r.getReceivedBy());
            ps.setString(4, r.getNote());
            ps.setInt(5, r.getReceivedBy());
            ps.setInt(6, r.getBranchId());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public int confirm(Connection conn, int id, int branchId, BigDecimal totalCost) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.StockReceipt SET Status='CONFIRMED', TotalCost=? " +
                "WHERE StockReceiptId=? AND BranchId=? AND Status='DRAFT'")) {
            ps.setBigDecimal(1, totalCost);
            ps.setInt(2, id);
            ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }

    public int cancel(Connection conn, int id, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.StockReceipt SET Status='CANCELLED' " +
                "WHERE StockReceiptId=? AND BranchId=? AND Status='DRAFT'")) {
            ps.setInt(1, id);
            ps.setInt(2, branchId);
            return ps.executeUpdate();
        }
    }

    private StockReceipt map(ResultSet rs) throws SQLException {
        StockReceipt r = new StockReceipt();
        r.setStockReceiptId(rs.getInt("StockReceiptId"));
        r.setBranchId(rs.getInt("BranchId"));
        int sid = rs.getInt("SupplierId");
        r.setSupplierId(rs.wasNull() ? null : sid);
        r.setReceivedBy(rs.getInt("ReceivedBy"));
        java.sql.Date d = rs.getDate("DocumentDate");
        r.setDocumentDate(d == null ? null : d.toLocalDate());
        Timestamp created = rs.getTimestamp("CreatedAt");
        r.setCreatedAt(created == null ? null : created.toLocalDateTime());
        r.setStatus(rs.getString("Status"));
        r.setTotalCost(rs.getBigDecimal("TotalCost"));
        r.setNote(rs.getString("Note"));
        r.setSupplierName(rs.getString("SupplierName"));
        r.setReceivedByName(rs.getString("ReceivedByName"));
        return r;
    }
}
