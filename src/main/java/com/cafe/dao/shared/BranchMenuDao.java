package com.cafe.dao.shared;

import com.cafe.model.BranchMenuItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class BranchMenuDao {

    public List<BranchMenuItem> listForBranch(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT p.ProductId, p.Name, p.BasePrice, " +
            "       bm.IsAvailable, bm.LocalPrice, bm.Is86, " +
            "       CASE WHEN bm.ProductId IS NULL THEN 0 ELSE 1 END AS Published " +
            "FROM catalog.Product p " +
            "LEFT JOIN catalog.BranchMenu bm ON bm.ProductId = p.ProductId AND bm.BranchId = ? " +
            "WHERE p.IsActive = 1 ORDER BY p.Name";
        List<BranchMenuItem> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BranchMenuItem m = new BranchMenuItem();
                    m.setBranchId(branchId);
                    m.setProductId(rs.getInt("ProductId"));
                    m.setProductName(rs.getString("Name"));
                    m.setBasePrice(rs.getBigDecimal("BasePrice"));
                    boolean published = rs.getInt("Published") == 1;
                    m.setPublished(published);
                    if (published) {
                        m.setAvailable(rs.getBoolean("IsAvailable"));
                        m.setLocalPrice(rs.getBigDecimal("LocalPrice"));
                        m.setIs86(rs.getBoolean("Is86"));
                    }
                    out.add(m);
                }
            }
        }
        return out;
    }

    private boolean exists(Connection conn, int branchId, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM catalog.BranchMenu WHERE BranchId = ? AND ProductId = ?")) {
            ps.setInt(1, branchId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public void upsert(Connection conn, int branchId, int productId,
                       boolean available, BigDecimal localPrice, boolean is86) throws SQLException {
        if (exists(conn, branchId, productId)) {
            final String sql = "UPDATE catalog.BranchMenu SET IsAvailable=?, LocalPrice=?, Is86=? WHERE BranchId=? AND ProductId=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, available);
                if (localPrice == null) ps.setNull(2, Types.DECIMAL); else ps.setBigDecimal(2, localPrice);
                ps.setBoolean(3, is86);
                ps.setInt(4, branchId);
                ps.setInt(5, productId);
                ps.executeUpdate();
            }
        } else {
            final String sql = "INSERT INTO catalog.BranchMenu(BranchId, ProductId, IsAvailable, LocalPrice, Is86) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, branchId);
                ps.setInt(2, productId);
                ps.setBoolean(3, available);
                if (localPrice == null) ps.setNull(4, Types.DECIMAL); else ps.setBigDecimal(4, localPrice);
                ps.setBoolean(5, is86);
                ps.executeUpdate();
            }
        }
    }

    /** Chỉ đổi cờ 86 (+ ETA quay lại) — Barista toggle, giữ nguyên giá/available. */
    public void updateIs86(Connection conn, int branchId, int productId, boolean is86) throws SQLException {
        final String sql = "UPDATE catalog.BranchMenu SET Is86=?, BackInEta=NULL WHERE BranchId=? AND ProductId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, is86);
            ps.setInt(2, branchId);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    public void remove(Connection conn, int branchId, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM catalog.BranchMenu WHERE BranchId = ? AND ProductId = ?")) {
            ps.setInt(1, branchId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }
}
