package com.cafe.dao.shared;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BranchStatusDao {

    public boolean isActive(Connection conn, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT IsActive FROM org.Branch WHERE BranchId=?")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("IsActive");
            }
        }
    }
}
