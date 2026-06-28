package com.cafe.dao.admin;

import com.cafe.model.Branch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BranchDao {

    private static final String SELECT =
        "SELECT BranchId, Code, Name, Address, Phone, IsActive FROM org.Branch ";

    public List<Branch> findAll(Connection conn) throws SQLException {
        List<Branch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY Code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<Branch> findAllActive(Connection conn) throws SQLException {
        List<Branch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE IsActive = 1 ORDER BY Code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public Branch findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE BranchId = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Connection conn, Branch b) throws SQLException {
        final String sql = "INSERT INTO org.Branch(Code, Name, Address, Phone, IsActive) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getCode());
            ps.setString(2, b.getName());
            ps.setString(3, b.getAddress());
            ps.setString(4, b.getPhone());
            ps.setBoolean(5, b.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public void update(Connection conn, Branch b) throws SQLException {
        final String sql = "UPDATE org.Branch SET Code=?, Name=?, Address=?, Phone=?, IsActive=? WHERE BranchId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getCode());
            ps.setString(2, b.getName());
            ps.setString(3, b.getAddress());
            ps.setString(4, b.getPhone());
            ps.setBoolean(5, b.isActive());
            ps.setInt(6, b.getBranchId());
            ps.executeUpdate();
        }
    }

    public void updateActive(Connection conn, int id, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE org.Branch SET IsActive=? WHERE BranchId=?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateManager(Connection conn, int branchId, Integer userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE org.Branch SET ManagerUserId=? WHERE BranchId=?")) {
            if (userId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, userId);
            ps.setInt(2, branchId);
            ps.executeUpdate();
        }
    }

    private Branch map(ResultSet rs) throws SQLException {
        Branch b = new Branch();
        b.setBranchId(rs.getInt("BranchId"));
        b.setCode(rs.getString("Code"));
        b.setName(rs.getString("Name"));
        b.setAddress(rs.getString("Address"));
        b.setPhone(rs.getString("Phone"));
        b.setActive(rs.getBoolean("IsActive"));
        return b;
    }
}
