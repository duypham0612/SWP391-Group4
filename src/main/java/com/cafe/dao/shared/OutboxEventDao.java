package com.cafe.dao.shared;

import com.cafe.common.EventType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Truy cập ops.OutboxEvent. Mọi câu SQL của outbox được giữ tại DAO này;
 * Service truyền Connection hiện tại để event tham gia cùng transaction nghiệp vụ.
 */
public class OutboxEventDao {

    public void insert(Connection conn, EventType type, String aggregateId,
                       Integer branchId, String payloadJson) throws SQLException {
        final String sql =
                "INSERT INTO ops.OutboxEvent(EventType, AggregateId, BranchId, Payload) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.wire());
            if (aggregateId == null) ps.setNull(2, Types.VARCHAR);
            else ps.setString(2, aggregateId);
            if (branchId == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, branchId);
            if (payloadJson == null) ps.setNull(4, Types.NVARCHAR);
            else ps.setString(4, payloadJson);
            ps.executeUpdate();
        }
    }

    /**
     * Bàn đang có yêu cầu mở (chưa xử lý) của chi nhánh → thời điểm yêu cầu SỚM NHẤT.
     * Khách bấm nhiều lần chỉ tính là một bàn đang chờ; giữ mốc sớm nhất để quầy thấy chờ bao lâu.
     */
    public Map<Integer, LocalDateTime> findPendingOpenRequests(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT AggregateId, MIN(CreatedAt) AS FirstAt FROM ops.OutboxEvent " +
            "WHERE EventType=? AND BranchId=? AND ProcessedAt IS NULL " +
            "GROUP BY AggregateId ORDER BY MIN(CreatedAt)";
        Map<Integer, LocalDateTime> out = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, EventType.TABLE_OPEN_REQUESTED.wire());
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer tableId = parseTableId(rs.getString("AggregateId"));
                    if (tableId == null) continue;
                    Timestamp at = rs.getTimestamp("FirstAt");
                    out.put(tableId, at == null ? null : at.toLocalDateTime());
                }
            }
        }
        return out;
    }

    /** Có yêu cầu mở đang chờ cho bàn này không (dùng để tránh ghi trùng khi khách bấm lại). */
    public boolean hasPendingOpenRequest(Connection conn, int tableId) throws SQLException {
        final String sql = "SELECT TOP(1) 1 FROM ops.OutboxEvent " +
                           "WHERE EventType=? AND AggregateId=? AND ProcessedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, EventType.TABLE_OPEN_REQUESTED.wire());
            ps.setString(2, String.valueOf(tableId));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Đóng mọi yêu cầu mở còn treo của bàn — gọi trong cùng tx với lúc thu ngân mở bàn. */
    public int markOpenRequestsProcessed(Connection conn, int tableId) throws SQLException {
        final String sql = "UPDATE ops.OutboxEvent SET ProcessedAt=SYSUTCDATETIME() " +
                           "WHERE EventType=? AND AggregateId=? AND ProcessedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, EventType.TABLE_OPEN_REQUESTED.wire());
            ps.setString(2, String.valueOf(tableId));
            return ps.executeUpdate();
        }
    }

    /**
     * Tín hiệu khách chưa được tiếp nhận theo bàn. AggregateId của hai event là tableId;
     * ưu tiên "xin thanh toán" nếu một bàn đồng thời có cả hai tín hiệu.
     */
    public Map<Integer, String> findPendingSignals(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT oe.AggregateId,oe.EventType,dt.DiningTableId " +
            "FROM ops.OutboxEvent oe " +
            "JOIN sales.DiningTable dt ON dt.DiningTableId=TRY_CONVERT(INT,oe.AggregateId) " +
            "WHERE oe.EventType IN (?,?) AND oe.ProcessedAt IS NULL " +
            "  AND oe.BranchId=? AND dt.BranchId=? AND dt.Status='OCCUPIED' " +
            "  AND EXISTS (SELECT 1 FROM sales.SalesOrder o " +
            "      JOIN sales.OrderItem oi ON oi.OrderId=o.OrderId " +
            "      LEFT JOIN payment.Bill b ON b.BillId=oi.BillId " +
            "      WHERE o.DiningTableId=dt.DiningTableId AND o.BranchId=dt.BranchId " +
            "        AND oi.Status<>'CANCELLED' AND (oi.BillId IS NULL OR b.Status='UNPAID')) " +
            "ORDER BY CASE WHEN oe.EventType=? THEN 0 ELSE 1 END, oe.CreatedAt";
        Map<Integer, String> out = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, EventType.SERVICE_CALL.wire());
            ps.setString(2, EventType.BILL_REQUESTED.wire());
            ps.setInt(3, branchId);
            ps.setInt(4, branchId);
            ps.setString(5, EventType.BILL_REQUESTED.wire());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int tableId = rs.getInt("DiningTableId");
                    out.putIfAbsent(tableId, rs.getString("EventType"));
                }
            }
        }
        return out;
    }

    /** Đánh dấu cả gọi nhân viên và xin thanh toán đã được thu ngân tiếp nhận. */
    public int markSignalsProcessed(Connection conn, int tableId) throws SQLException {
        final String sql =
            "UPDATE ops.OutboxEvent SET ProcessedAt=SYSUTCDATETIME() " +
            "WHERE EventType IN (?,?) AND AggregateId=? AND ProcessedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, EventType.SERVICE_CALL.wire());
            ps.setString(2, EventType.BILL_REQUESTED.wire());
            ps.setString(3, String.valueOf(tableId));
            return ps.executeUpdate();
        }
    }

    /** Thanh toán xong tự hạ riêng tín hiệu xin thanh toán của bàn, cùng transaction payBill. */
    public int markBillRequestProcessed(Connection conn, int tableId) throws SQLException {
        final String sql =
            "UPDATE ops.OutboxEvent SET ProcessedAt=SYSUTCDATETIME() " +
            "WHERE EventType=? AND AggregateId=? AND ProcessedAt IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, EventType.BILL_REQUESTED.wire());
            ps.setString(2, String.valueOf(tableId));
            return ps.executeUpdate();
        }
    }

    /** AggregateId là VARCHAR dùng chung cho mọi loại event — bỏ qua bản ghi không phải id bàn. */
    private static Integer parseTableId(String aggregateId) {
        if (aggregateId == null) return null;
        try { return Integer.valueOf(aggregateId.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
