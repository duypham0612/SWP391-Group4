package com.cafe.dao.shared;

import com.cafe.common.ModifierGroupNames;
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
            "SELECT g.ModifierGroupId,g.ProductId,g.Name,g.IsRequired,g.MinSelect,g.MaxSelect,g.SortOrder, " +
            "  (SELECT COUNT(*) FROM catalog.ModifierOption o WHERE o.ModifierGroupId = g.ModifierGroupId) AS OptionCount, " +
            "  1 AS ProductCount " +
            "FROM catalog.ModifierGroup g ORDER BY g.ProductId,g.SortOrder,g.ModifierGroupId";
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
        final String sql = "SELECT ModifierGroupId,ProductId,Name,IsRequired,MinSelect,MaxSelect,SortOrder " +
                "FROM catalog.ModifierGroup WHERE ModifierGroupId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insert(Connection conn, ModifierGroup g) throws SQLException {
        final String sql = "INSERT INTO catalog.ModifierGroup(ProductId,Name,IsRequired,MinSelect,MaxSelect,SortOrder) " +
                "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, g);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : 0; }
        }
    }

    public void update(Connection conn, ModifierGroup g) throws SQLException {
        final String sql = "UPDATE catalog.ModifierGroup SET Name=?,IsRequired=?,MinSelect=?,MaxSelect=?,SortOrder=? " +
                "WHERE ModifierGroupId=? AND ProductId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, g.getName());
            ps.setBoolean(2, g.isRequired());
            ps.setInt(3, g.getMinSelect());
            ps.setInt(4, g.getMaxSelect());
            ps.setInt(5, sortOrder(g));
            ps.setInt(6, g.getModifierGroupId());
            ps.setInt(7, g.getProductId());
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
        ps.setInt(1, g.getProductId());
        ps.setString(2, g.getName());
        ps.setBoolean(3, g.isRequired());
        ps.setInt(4, g.getMinSelect());
        ps.setInt(5, g.getMaxSelect());
        ps.setInt(6, sortOrder(g));
    }

    private ModifierGroup map(ResultSet rs) throws SQLException {
        ModifierGroup g = new ModifierGroup();
        g.setModifierGroupId(rs.getInt("ModifierGroupId"));
        g.setProductId(rs.getInt("ProductId"));
        g.setName(rs.getString("Name"));
        g.setRequired(rs.getBoolean("IsRequired"));
        g.setMinSelect(rs.getInt("MinSelect"));
        g.setMaxSelect(rs.getInt("MaxSelect"));
        g.setSortOrder(rs.getInt("SortOrder"));
        return g;
    }

    private int sortOrder(ModifierGroup group) {
        if (group.getSortOrder() > 0) return group.getSortOrder();
        if (ModifierGroupNames.isSize(group.getName())) return 1;
        if (ModifierGroupNames.SUGAR.equals(group.getName())) return 2;
        if (ModifierGroupNames.ICE.equals(group.getName())) return 3;
        if ("Topping".equalsIgnoreCase(group.getName())) return 4;
        return 5;
    }
}
