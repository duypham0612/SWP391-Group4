package com.cafe.dao.barista;

import com.cafe.model.BaristaOpsSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Query read-only cho dashboard Barista; không dùng cho đánh giá nhân sự hay payroll. */
public class BaristaMetricsDao {

    public BaristaOpsSnapshot load(Connection conn, int branchId, int userId,
                                   LocalDateTime businessDayStartUtc) throws SQLException {
        BaristaOpsSnapshot out = new BaristaOpsSnapshot();
        Timestamp from = Timestamp.valueOf(businessDayStartUtc);
        loadMyOrders(conn, out, branchId, userId, from);
        loadMyEvents(conn, out, branchId, userId, from);
        loadBranchBoard(conn, out, branchId, from);
        loadExpiredPrep(conn, out, branchId);
        return out;
    }

    private void loadMyOrders(Connection conn, BaristaOpsSnapshot out, int branchId, int userId, Timestamp from)
            throws SQLException {
        final String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN oi.Status='MAKING' AND oi.BaristaId=? THEN oi.Quantity ELSE 0 END),0) AS MakingCups, "
                + "COALESCE(SUM(CASE WHEN oi.PreparedBy=? AND oi.DoneAt>=? THEN oi.Quantity ELSE 0 END),0) AS CompletedCups, "
                + "COALESCE(AVG(CASE WHEN oi.PreparedBy=? AND oi.DoneAt>=? AND oi.StartedAt IS NOT NULL "
                + " THEN CONVERT(BIGINT,DATEDIFF(SECOND, oi.StartedAt, oi.DoneAt)) END),0) AS AvgPrepSeconds "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE o.BranchId=? AND o.CreatedAt>=? AND oi.Status IN ('WAITING','MAKING','READY','PICKED_UP','SERVED','BLOCKED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId); ps.setTimestamp(3, from);
            ps.setInt(4, userId); ps.setTimestamp(5, from);
            ps.setInt(6, branchId); ps.setTimestamp(7, from);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out.setMyMakingCups(rs.getInt("MakingCups"));
                    out.setMyCompletedCups(rs.getInt("CompletedCups"));
                    out.setMyAveragePreparationSeconds(rs.getLong("AvgPrepSeconds"));
                }
            }
        }
    }

    private void loadMyEvents(Connection conn, BaristaOpsSnapshot out, int branchId, int userId, Timestamp from)
            throws SQLException {
        final String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN EventKind='REMAKE' THEN 1 ELSE 0 END),0) AS Remakes, "
                + "COALESCE(SUM(CASE WHEN EventKind='INGREDIENT_WASTE' THEN 1 ELSE 0 END),0) AS Wastes "
                + "FROM inventory.WasteEvent WHERE BranchId=? AND CreatedBy=? AND CreatedAt>=? "
                + "AND EventKind IN ('REMAKE','INGREDIENT_WASTE')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setInt(2, userId); ps.setTimestamp(3, from);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out.setMyRemakeCount(rs.getInt("Remakes"));
                    out.setMyWasteCount(rs.getInt("Wastes"));
                }
            }
        }
    }

    private void loadBranchBoard(Connection conn, BaristaOpsSnapshot out, int branchId, Timestamp from)
            throws SQLException {
        final String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN oi.Status='WAITING' THEN oi.Quantity ELSE 0 END),0) AS WaitingCups, "
                + "COALESCE(SUM(CASE WHEN oi.Status='MAKING' THEN oi.Quantity ELSE 0 END),0) AS MakingCups, "
                + "COALESCE(SUM(CASE WHEN oi.Status='READY' THEN oi.Quantity ELSE 0 END),0) AS ReadyCups, "
                + "COALESCE(SUM(CASE WHEN oi.Status='BLOCKED' THEN oi.Quantity ELSE 0 END),0) AS BlockedCups "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE o.BranchId=? AND o.CreatedAt>=? AND o.Status='ACTIVE' "
                + "AND oi.Status IN ('WAITING','MAKING','READY','BLOCKED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setTimestamp(2, from);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out.setBranchWaitingCups(rs.getInt("WaitingCups"));
                    out.setBranchMakingCups(rs.getInt("MakingCups"));
                    out.setBranchReadyCups(rs.getInt("ReadyCups"));
                    out.setBranchBlockedCups(rs.getInt("BlockedCups"));
                }
            }
        }
        final String remakeSql = "SELECT COUNT(*) FROM inventory.WasteEvent "
                + "WHERE BranchId=? AND EventKind='REMAKE' AND CreatedAt>=?";
        try (PreparedStatement ps = conn.prepareStatement(remakeSql)) {
            ps.setInt(1, branchId); ps.setTimestamp(2, from);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) out.setBranchRemakeCount(rs.getInt(1)); }
        }
    }

    private void loadExpiredPrep(Connection conn, BaristaOpsSnapshot out, int branchId) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM inventory.PrepBatch "
                + "WHERE BranchId=? AND Status='ACTIVE' AND WrittenOffAt IS NULL AND ExpiresAt<SYSUTCDATETIME()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) out.setExpiredPrepBatchCount(rs.getInt(1)); }
        }
    }
}
