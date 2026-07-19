package com.cafe.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Điểm DUY NHẤT ghi domain event vào ops.OutboxEvent (đặc tả mục 3).
 * Gọi trong cùng transaction của nghiệp vụ (nhận Connection từ Service).
 */
public final class EventPublisher {

    private EventPublisher() { }

    public static void publish(Connection conn, EventType type, String aggregateId,
                               Integer branchId, String payloadJson) throws SQLException {
        final String sql =
            "INSERT INTO ops.OutboxEvent(EventType, AggregateId, BranchId, Payload) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type.wire());
            if (aggregateId == null) ps.setNull(2, Types.VARCHAR); else ps.setString(2, aggregateId);
            if (branchId == null)    ps.setNull(3, Types.INTEGER); else ps.setInt(3, branchId);
            if (payloadJson == null) ps.setNull(4, Types.NVARCHAR); else ps.setString(4, payloadJson);
            ps.executeUpdate();
        }
    }
}
