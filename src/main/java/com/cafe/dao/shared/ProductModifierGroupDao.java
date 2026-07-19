package com.cafe.dao.shared;

import com.cafe.model.ProductModifierGroup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductModifierGroupDao {

    public List<ProductModifierGroup> findByProduct(Connection conn, int productId) throws SQLException {
        final String sql =
            "SELECT pmg.ProductId, pmg.ModifierGroupId, g.Name AS GroupName " +
            "FROM catalog.ProductModifierGroup pmg JOIN catalog.ModifierGroup g ON pmg.ModifierGroupId = g.ModifierGroupId " +
            "WHERE pmg.ProductId = ? " +
            "ORDER BY CASE g.Name WHEN N'Size' THEN 1 WHEN N'Đường' THEN 2 WHEN N'Đá' THEN 3 WHEN N'Topping' THEN 4 ELSE 5 END, pmg.ModifierGroupId";
        List<ProductModifierGroup> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductModifierGroup p = new ProductModifierGroup();
                    p.setProductId(rs.getInt("ProductId"));
                    p.setModifierGroupId(rs.getInt("ModifierGroupId"));
                    p.setGroupName(rs.getString("GroupName"));
                    out.add(p);
                }
            }
        }
        return out;
    }

    public boolean exists(Connection conn, int productId, int groupId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM catalog.ProductModifierGroup WHERE ProductId = ? AND ModifierGroupId = ?")) {
            ps.setInt(1, productId);
            ps.setInt(2, groupId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public void insert(Connection conn, int productId, int groupId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO catalog.ProductModifierGroup(ProductId, ModifierGroupId) VALUES (?,?)")) {
            ps.setInt(1, productId);
            ps.setInt(2, groupId);
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, int productId, int groupId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM catalog.ProductModifierGroup WHERE ProductId = ? AND ModifierGroupId = ?")) {
            ps.setInt(1, productId);
            ps.setInt(2, groupId);
            ps.executeUpdate();
        }
    }

    public void deleteByGroup(Connection conn, int groupId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM catalog.ProductModifierGroup WHERE ModifierGroupId = ?")) {
            ps.setInt(1, groupId);
            ps.executeUpdate();
        }
    }
}
