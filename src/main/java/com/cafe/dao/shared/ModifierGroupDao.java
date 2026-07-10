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

    /** Kèm số option & số sản phẩm đang dùng — phục vụ màn tổng quan. */
    public List<ModifierGroup> findAll(Connection conn) throws SQLException {
        final String sql =
            "SELECT g.ModifierGroupId, g.Name, g.IsRequired, g.MinSelect, g.MaxSelect, " +
            "  (SELECT COUNT(*) FROM catalog.ModifierOption o WHERE o.ModifierGroupId = g.ModifierGroupId) AS OptionCount, " +
            "  (SELECT COUNT(*) FROM catalog.ProductModifierGroup p WHERE p.ModifierGroupId = g.ModifierGroupId) AS ProductCount " +
            "FROM catalog.ModifierGroup g ORDER BY g.Name";
        List<ModifierGroup> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ModifierGroup g = map(rs);
                g.setOptionCount(rs.getInt("OptionCount"));
                g.setProductCount(rs.getInt("ProductCount"));
                out.add(g);
            }
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

    public void delete(Connection conn, int groupId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM catalog.ModifierGroup WHERE ModifierGroupId=?")) {
            ps.setInt(1, groupId);
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
