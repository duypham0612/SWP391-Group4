package com.cafe.dao.manager;

import com.cafe.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SupplierDao {

    private static final String SELECT = "SELECT SupplierId, Name, Phone, Address, IsActive FROM inventory.Supplier ";

    public List<Supplier> findAll(Connection conn) throws SQLException {
        List<Supplier> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "ORDER BY Name");
             ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        return out;
    }

    public List<Supplier> findAllActive(Connection conn) throws SQLException {
        List<Supplier> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE IsActive=1 ORDER BY Name");
             ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        return out;
    }

    public Supplier findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE SupplierId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insert(Connection conn, Supplier s) throws SQLException {
        final String sql = "INSERT INTO inventory.Supplier(Name, Phone, Address, IsActive) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getAddress());
            ps.setBoolean(4, s.isActive());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public void update(Connection conn, Supplier s) throws SQLException {
        final String sql = "UPDATE inventory.Supplier SET Name=?, Phone=?, Address=?, IsActive=? WHERE SupplierId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getPhone());
            ps.setString(3, s.getAddress());
            ps.setBoolean(4, s.isActive());
            ps.setInt(5, s.getSupplierId());
            ps.executeUpdate();
        }
    }

    public void updateActive(Connection conn, int id, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE inventory.Supplier SET IsActive=? WHERE SupplierId=?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setSupplierId(rs.getInt("SupplierId"));
        s.setName(rs.getString("Name"));
        s.setPhone(rs.getString("Phone"));
        s.setAddress(rs.getString("Address"));
        s.setActive(rs.getBoolean("IsActive"));
        return s;
    }
}
