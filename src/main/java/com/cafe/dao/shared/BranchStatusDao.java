package com.cafe.dao.shared;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BranchStatusDao {

    public record AccessStatus(boolean active, boolean managerAssigned) {}

    public AccessStatus findAccessStatus(Connection conn, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT IsActive, ManagerUserId FROM org.Branch WHERE BranchId=?")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new AccessStatus(false, false);
                return new AccessStatus(
                        rs.getBoolean("IsActive"),
                        rs.getObject("ManagerUserId") != null);
            }
        }
    }

    public boolean isActive(Connection conn, int branchId) throws SQLException {
        return findAccessStatus(conn, branchId).active();
    }
}
