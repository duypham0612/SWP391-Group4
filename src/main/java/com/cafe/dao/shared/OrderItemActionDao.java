package com.cafe.dao.shared;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/** Append-only audit cho mọi transition nghiệp vụ của một dòng món. */
public class OrderItemActionDao {
    public void insert(Connection conn, int orderItemId, int branchId, String action,
                       String fromStatus, String toStatus, String reason, Integer performedBy)
            throws SQLException {
        final String sql = "INSERT INTO ops.OrderItemActionLog"
                + "(OrderItemId,BranchId,ActionType,FromStatus,ToStatus,Reason,PerformedBy) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId);
            ps.setInt(2, branchId);
            ps.setString(3, action);
            if (fromStatus == null) ps.setNull(4, Types.VARCHAR); else ps.setString(4, fromStatus);
            if (toStatus == null) ps.setNull(5, Types.VARCHAR); else ps.setString(5, toStatus);
            if (reason == null || reason.isBlank()) ps.setNull(6, Types.NVARCHAR); else ps.setString(6, reason);
            if (performedBy == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, performedBy);
            ps.executeUpdate();
        }
    }
}
