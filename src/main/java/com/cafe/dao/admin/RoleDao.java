package com.cafe.dao.admin;

import com.cafe.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoleDao {

    public List<Role> findAll(Connection conn) throws SQLException {
        final String sql = "SELECT RoleId, Code, Name FROM iam.Role ORDER BY RoleId";
        List<Role> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Role r = new Role();
                r.setRoleId(rs.getInt("RoleId"));
                r.setCode(rs.getString("Code"));
                r.setName(rs.getString("Name"));
                out.add(r);
            }
        }
        return out;
    }
}
