package com.cafe.dao.shared;

import com.cafe.model.ModifierGroup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ModifierGroupDao {

    public List<ModifierGroup> findAll(Connection conn) throws SQLException {
        final String sql = "SELECT ModifierGroupId, Name, IsRequired, MinSelect, MaxSelect FROM catalog.ModifierGroup ORDER BY Name";
        List<ModifierGroup> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public ModifierGroup findById(Connection conn, int id) throws SQLException {
        final String sql = "SELECT ModifierGroupId, Name, IsRequired, MinSelect, MaxSelect FROM catalog.ModifierGroup WHERE ModifierGroupId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insert(Connection conn, ModifierGroup g) throws SQLException {
        final String sql = "INSERT INTO catalog.ModifierGroup(Name, IsRequired, MinSelect, MaxSelect) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, g);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : 0; }
        }
    }

    public void update(Connection conn, ModifierGroup g) throws SQLException {
        final String sql = "UPDATE catalog.ModifierGroup SET Name=?, IsRequired=?, MinSelect=?, MaxSelect=? WHERE ModifierGroupId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, g);
            ps.setInt(5, g.getModifierGroupId());
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, ModifierGroup g) throws SQLException {
        ps.setString(1, g.getName());
        ps.setBoolean(2, g.isRequired());
        ps.setInt(3, g.getMinSelect());
        ps.setInt(4, g.getMaxSelect());
    }

    private ModifierGroup map(ResultSet rs) throws SQLException {
        ModifierGroup g = new ModifierGroup();
        g.setModifierGroupId(rs.getInt("ModifierGroupId"));
        g.setName(rs.getString("Name"));
        g.setRequired(rs.getBoolean("IsRequired"));
        g.setMinSelect(rs.getInt("MinSelect"));
        g.setMaxSelect(rs.getInt("MaxSelect"));
        return g;
    }
}
