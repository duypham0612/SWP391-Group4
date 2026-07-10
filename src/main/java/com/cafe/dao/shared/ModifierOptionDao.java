package com.cafe.dao.shared;

import com.cafe.model.ModifierOption;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ModifierOptionDao {

    public List<ModifierOption> findByGroup(Connection conn, int groupId) throws SQLException {
        final String sql = "SELECT ModifierOptionId, ModifierGroupId, Name, PriceDelta, IsActive FROM catalog.ModifierOption WHERE ModifierGroupId = ? ORDER BY ModifierOptionId";
        List<ModifierOption> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public ModifierOption findById(Connection conn, int id) throws SQLException {
        final String sql = "SELECT ModifierOptionId, ModifierGroupId, Name, PriceDelta, IsActive FROM catalog.ModifierOption WHERE ModifierOptionId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public void insert(Connection conn, ModifierOption o) throws SQLException {
        final String sql = "INSERT INTO catalog.ModifierOption(ModifierGroupId, Name, PriceDelta, IsActive) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, o.getModifierGroupId());
            ps.setString(2, o.getName());
            ps.setBigDecimal(3, o.getPriceDelta());
            ps.setBoolean(4, o.isActive());
            ps.executeUpdate();
        }
    }

    public void update(Connection conn, ModifierOption o) throws SQLException {
        final String sql = "UPDATE catalog.ModifierOption SET Name=?, PriceDelta=?, IsActive=? WHERE ModifierOptionId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getName());
            ps.setBigDecimal(2, o.getPriceDelta());
            ps.setBoolean(3, o.isActive());
            ps.setInt(4, o.getModifierOptionId());
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, int optionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM catalog.ModifierOption WHERE ModifierOptionId = ?")) {
            ps.setInt(1, optionId);
            ps.executeUpdate();
        }
    }

    private ModifierOption map(ResultSet rs) throws SQLException {
        ModifierOption o = new ModifierOption();
        o.setModifierOptionId(rs.getInt("ModifierOptionId"));
        o.setModifierGroupId(rs.getInt("ModifierGroupId"));
        o.setName(rs.getString("Name"));
        o.setPriceDelta(rs.getBigDecimal("PriceDelta"));
        o.setActive(rs.getBoolean("IsActive"));
        return o;
    }
}
