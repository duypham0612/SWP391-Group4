package com.cafe.dao.shared;

import com.cafe.model.StockCount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Read DAO phiên kiểm kê đã được gộp vào inventory.StockAdjustment.
 */
public class StockCountDao {

    /**
     * Danh sách biên bản của chi nhánh, kèm số dòng và tổng chênh lệch.
     *
     * <p>Hai con số này tính từ chi tiết chứ KHÔNG lưu ở header: cache đếm sẵn là đúng thứ
     * gây lệch dữ liệu mà bản rà soát database đang đi dọn.
     */
    public List<StockCount> findByBranch(Connection conn, int branchId, int limit) throws SQLException {
        final String sql =
            "SELECT TOP (?) a.CountBatchId, a.BranchId, MAX(a.CountedBy) AS CountedBy, " +
            "       MAX(a.CountedAt) AS CountedAt, MAX(a.CountNote) AS CountNote, " +
            "       MAX(u.FullName) AS CountedByName, COUNT(*) AS LineCount, " +
            "       ISNULL(SUM(a.DiffQty), 0) AS TotalDiffQty " +
            "FROM inventory.StockAdjustment a " +
            "LEFT JOIN iam.UserAccount u ON u.UserId = a.CountedBy " +
            "WHERE a.BranchId = ? AND a.CountBatchId IS NOT NULL " +
            "GROUP BY a.CountBatchId, a.BranchId " +
            "ORDER BY MAX(a.CountedAt) DESC, a.CountBatchId DESC";
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
        s.setCountBatchId(rs.getString("CountBatchId"));
        s.setBranchId(rs.getInt("BranchId"));
        s.setCountedBy(rs.getInt("CountedBy"));
        Timestamp ts = rs.getTimestamp("CountedAt");
        s.setCountedAt(ts == null ? null : ts.toLocalDateTime());
        s.setNote(rs.getString("CountNote"));
        s.setCountedByName(rs.getString("CountedByName"));
        s.setLineCount(rs.getInt("LineCount"));
        s.setTotalDiffQty(rs.getBigDecimal("TotalDiffQty"));
        return s;
    }
}
