package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BaristaActionLogDao;
import com.cafe.model.BaristaActionLog;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Read facade and correlation helper for barista audit events. */
public class BaristaAuditService {
    private final BaristaActionLogDao dao = new BaristaActionLogDao();

    public static String correlationId() { return UUID.randomUUID().toString(); }

    public long record(Connection conn, int branchId, String entityType, Long entityId, String actionType,
                       String beforeJson, String afterJson, String reason, Integer performedBy,
                       String correlationId) throws SQLException {
        return dao.insert(conn, branchId, null, entityType, entityId, actionType,
                beforeJson, afterJson, reason, performedBy, correlationId);
    }

    public List<BaristaActionLog> recent(int branchId, int limit) throws SQLException {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime from = today.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh"))
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        try (Connection conn = DBConnection.getConnection()) { return dao.findByBranchSince(conn, branchId, from, limit); }
    }

    public List<BaristaActionLog> forEntity(int branchId, String entityType, long entityId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findByEntity(conn, branchId, entityType, entityId); }
    }

    public List<BaristaActionLog> all(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return dao.findAllByBranch(conn, branchId); }
    }
}
