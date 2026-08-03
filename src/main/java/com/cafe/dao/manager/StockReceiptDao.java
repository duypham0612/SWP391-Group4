package com.cafe.dao.manager;

import com.cafe.model.StockReceipt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Read/update aggregate phiếu nhập từ các dòng inventory.StockReceiptLine. */
public class StockReceiptDao {

    private static final String SELECT =
        "SELECT r.ReceiptBatchId,r.BranchId,r.SupplierId,r.ReceivedBy,r.DocumentDate,r.CreatedAt,r.Status," +
        "       r.TotalCost,r.Note,s.Name AS SupplierName,u.FullName AS ReceivedByName " +
        "FROM (SELECT ReceiptBatchId,MAX(BranchId) AS BranchId,MAX(SupplierId) AS SupplierId," +
        "             MAX(ReceivedBy) AS ReceivedBy,MAX(DocumentDate) AS DocumentDate," +
        "             MAX(CreatedAt) AS CreatedAt,MAX(Status) AS Status,MAX(Note) AS Note," +
        "             SUM(UnitCost*EnteredQuantity) AS TotalCost " +
        "      FROM inventory.StockReceiptLine GROUP BY ReceiptBatchId) r " +
        "LEFT JOIN inventory.Supplier s ON r.SupplierId=s.SupplierId " +
        "LEFT JOIN iam.UserAccount u ON r.ReceivedBy=u.UserId ";

    public List<StockReceipt> findByBranch(Connection conn, int branchId) throws SQLException {
        List<StockReceipt> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE r.BranchId=? ORDER BY r.CreatedAt DESC,r.ReceiptBatchId DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public StockReceipt findById(Connection conn, String batchId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE r.ReceiptBatchId=? AND r.BranchId=?")) {
            ps.setString(1, batchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Khóa toàn bộ dòng DRAFT của batch đến hết transaction. */
    public StockReceipt findDraftForUpdate(Connection conn, String batchId, int branchId) throws SQLException {
        final String lockSql = "SELECT StockReceiptLineId FROM inventory.StockReceiptLine WITH (UPDLOCK,HOLDLOCK) "
                + "WHERE ReceiptBatchId=? AND BranchId=? AND Status='DRAFT'";
        boolean found = false;
        try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
            ps.setString(1, batchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { found = rs.next(); }
        }
        return found ? findById(conn, batchId, branchId) : null;
    }

    public int confirm(Connection conn, String batchId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.StockReceiptLine SET Status='CONFIRMED' "
                + "WHERE ReceiptBatchId=? AND BranchId=? AND Status='DRAFT'")) {
            ps.setString(1, batchId);
            ps.setInt(2, branchId);
            return ps.executeUpdate();
        }
    }

    public int cancel(Connection conn, String batchId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.StockReceiptLine SET Status='CANCELLED' "
                + "WHERE ReceiptBatchId=? AND BranchId=? AND Status='DRAFT'")) {
            ps.setString(1, batchId);
            ps.setInt(2, branchId);
            return ps.executeUpdate();
        }
    }

    private static StockReceipt map(ResultSet rs) throws SQLException {
        StockReceipt r = new StockReceipt();
        r.setReceiptBatchId(rs.getString("ReceiptBatchId"));
        r.setBranchId(rs.getInt("BranchId"));
        int supplierId = rs.getInt("SupplierId");
        r.setSupplierId(rs.wasNull() ? null : supplierId);
        r.setReceivedBy(rs.getInt("ReceivedBy"));
        java.sql.Date documentDate = rs.getDate("DocumentDate");
        r.setDocumentDate(documentDate == null ? null : documentDate.toLocalDate());
        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        r.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        r.setStatus(rs.getString("Status"));
        r.setTotalCost(rs.getBigDecimal("TotalCost"));
        r.setNote(rs.getString("Note"));
        r.setSupplierName(rs.getString("SupplierName"));
        r.setReceivedByName(rs.getString("ReceivedByName"));
        return r;
    }
}
