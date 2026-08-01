package com.cafe.dao.shared;

import com.cafe.model.StockCount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * inventory.StockCount — biên bản kiểm kê (header của inventory.StockAdjustment).
 *
 * <p>Một lượt kiểm kê ở màn Đối soát tick nhiều nguyên liệu rồi submit một lần; header này
 * là thứ cho phép nhóm N dòng chênh lệch đó lại thành một biên bản. Điều chỉnh lẻ (Barista
 * báo hết nguyên liệu tại màn pha chế) KHÔNG tạo header — dòng đó để {@code StockCountId} NULL.
 */
public class StockCountDao {

    public int insert(Connection conn, int branchId, int countedBy, String note) throws SQLException {
        final String sql = "INSERT INTO inventory.StockCount(BranchId, CountedBy, Note) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, countedBy);
            if (note == null || note.isBlank()) ps.setNull(3, Types.NVARCHAR); else ps.setString(3, note);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    /**
     * Danh sách biên bản của chi nhánh, kèm số dòng và tổng chênh lệch.
     *
     * <p>Hai con số này tính từ chi tiết chứ KHÔNG lưu ở header: cache đếm sẵn là đúng thứ
     * gây lệch dữ liệu mà bản rà soát database đang đi dọn.
     */
    public List<StockCount> findByBranch(Connection conn, int branchId, int limit) throws SQLException {
        final String sql =
            "SELECT TOP (?) sc.StockCountId, sc.BranchId, sc.CountedBy, sc.CountedAt, sc.Note, " +
            "       u.FullName AS CountedByName, " +
            "       (SELECT COUNT(*) FROM inventory.StockAdjustment a WHERE a.StockCountId = sc.StockCountId) AS LineCount, " +
            "       (SELECT ISNULL(SUM(a.DiffQty), 0) FROM inventory.StockAdjustment a WHERE a.StockCountId = sc.StockCountId) AS TotalDiffQty " +
            "FROM inventory.StockCount sc " +
            "LEFT JOIN iam.UserAccount u ON u.UserId = sc.CountedBy " +
            "WHERE sc.BranchId = ? " +
            "ORDER BY sc.CountedAt DESC, sc.StockCountId DESC";
        List<StockCount> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit <= 0 ? 50 : limit);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private static StockCount map(ResultSet rs) throws SQLException {
        StockCount s = new StockCount();
        s.setStockCountId(rs.getInt("StockCountId"));
        s.setBranchId(rs.getInt("BranchId"));
        s.setCountedBy(rs.getInt("CountedBy"));
        Timestamp ts = rs.getTimestamp("CountedAt");
        s.setCountedAt(ts == null ? null : ts.toLocalDateTime());
        s.setNote(rs.getString("Note"));
        s.setCountedByName(rs.getString("CountedByName"));
        s.setLineCount(rs.getInt("LineCount"));
        s.setTotalDiffQty(rs.getBigDecimal("TotalDiffQty"));
        return s;
    }
}
