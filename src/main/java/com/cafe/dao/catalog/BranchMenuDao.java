package com.cafe.dao.catalog;

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

    private static final String AVAILABILITY_SELECT =
            "SELECT p.ProductId, p.Name, p.BasePrice, p.ImageUrl, " +
            "       bm.IsListed, bm.LocalPrice, bm.IsTemporarilyUnavailable, bm.BackInEta, " +
            "       1 AS Published " +
            "FROM catalog.Product p " +
            "JOIN catalog.BranchMenu bm ON bm.ProductId = p.ProductId AND bm.BranchId = ? " +
            "WHERE p.IsActive = 1 ";

    public List<BranchMenuItem> listForBranch(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT p.ProductId, p.Name, p.BasePrice, p.ImageUrl, " +
            "       bm.IsListed, bm.LocalPrice, bm.IsTemporarilyUnavailable, bm.BackInEta, " +
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
                    m.setImageUrl(rs.getString("ImageUrl"));
                    boolean published = rs.getInt("Published") == 1;
                    m.setPublished(published);
                    if (published) {
                        m.setListed(rs.getBoolean("IsListed"));
                        m.setLocalPrice(rs.getBigDecimal("LocalPrice"));
                        m.setTemporarilyUnavailable(rs.getBoolean("IsTemporarilyUnavailable"));
                        java.sql.Timestamp eta = rs.getTimestamp("BackInEta");
                        if (eta != null) m.setBackInEta(eta.toLocalDateTime());
                    }
                    out.add(m);
                }
            }
        }
        return out;
    }

    /** Published menu rows for the barista 86 board, filtered and paged in SQL Server. */
    public List<BranchMenuItem> findAvailabilityPage(Connection conn, int branchId, String query,
                                                      String state, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(AVAILABILITY_SELECT);
        appendAvailabilityFilters(sql, query, state);
        sql.append("ORDER BY p.Name, p.ProductId OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        List<BranchMenuItem> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = bindAvailabilityFilters(ps, 1, branchId, query);
            ps.setInt(index++, Math.max(0, offset));
            ps.setInt(index, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs, branchId));
            }
        }
        return out;
    }

    /** Count uses exactly the same predicates as {@link #findAvailabilityPage}. */
    public int countAvailability(Connection conn, int branchId, String query, String state) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM catalog.Product p " +
                "JOIN catalog.BranchMenu bm ON bm.ProductId = p.ProductId AND bm.BranchId = ? " +
                "WHERE p.IsActive = 1 ");
        appendAvailabilityFilters(sql, query, state);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindAvailabilityFilters(ps, 1, branchId, query);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void appendAvailabilityFilters(StringBuilder sql, String query, String state) {
        if (query != null && !query.isBlank()) {
            sql.append("AND p.Name COLLATE SQL_Latin1_General_CP1_CI_AI " +
                    "LIKE ? COLLATE SQL_Latin1_General_CP1_CI_AI ESCAPE '\\' ");
        }
        if ("out".equals(state)) sql.append("AND bm.IsTemporarilyUnavailable = 1 ");
        else if ("available".equals(state)) sql.append("AND bm.IsTemporarilyUnavailable = 0 ");
    }

    private int bindAvailabilityFilters(PreparedStatement ps, int startIndex, int branchId,
                                        String query) throws SQLException {
        int index = startIndex;
        ps.setInt(index++, branchId);
        if (query != null && !query.isBlank()) {
            ps.setString(index++, "%" + escapeLike(query.trim()) + "%");
        }
        return index;
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("[", "\\[");
    }

    private BranchMenuItem map(ResultSet rs, int branchId) throws SQLException {
        BranchMenuItem m = new BranchMenuItem();
        m.setBranchId(branchId);
        m.setProductId(rs.getInt("ProductId"));
        m.setProductName(rs.getString("Name"));
        m.setBasePrice(rs.getBigDecimal("BasePrice"));
        m.setImageUrl(rs.getString("ImageUrl"));
        boolean published = rs.getInt("Published") == 1;
        m.setPublished(published);
        if (published) {
            m.setListed(rs.getBoolean("IsListed"));
            m.setLocalPrice(rs.getBigDecimal("LocalPrice"));
            m.setTemporarilyUnavailable(rs.getBoolean("IsTemporarilyUnavailable"));
            java.sql.Timestamp eta = rs.getTimestamp("BackInEta");
            if (eta != null) m.setBackInEta(eta.toLocalDateTime());
        }
        return m;
    }

    private boolean exists(Connection conn, int branchId, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM catalog.BranchMenu WHERE BranchId = ? AND ProductId = ?")) {
            ps.setInt(1, branchId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * Cấu hình menu chi nhánh: bật/tắt bán + giá riêng. CỐ Ý không đụng {@code IsTemporarilyUnavailable}/{@code BackInEta} —
     * cờ hết món chỉ do luồng báo hết ghi ({@code request86}/{@code reopen86}), vì nó phải khớp với
     * yêu cầu còn mở trong các cột {@code BranchMenu.Block*}. Ghi từ hai đường sẽ làm lệch hai bên:
     * món mở bán lại mà yêu cầu vẫn treo thì barista vướng unique index, không báo hết lại được.
     */
    public void upsert(Connection conn, int branchId, int productId,
                       boolean available, BigDecimal localPrice) throws SQLException {
        if (exists(conn, branchId, productId)) {
            final String sql = "UPDATE catalog.BranchMenu SET IsListed=?, LocalPrice=? WHERE BranchId=? AND ProductId=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, available);
                if (localPrice == null) ps.setNull(2, Types.DECIMAL); else ps.setBigDecimal(2, localPrice);
                ps.setInt(3, branchId);
                ps.setInt(4, productId);
                ps.executeUpdate();
            }
        } else {
            // Món vừa lên menu thì chưa hết — để cột IsTemporarilyUnavailable dùng DEFAULT 0 của schema.
            final String sql = "INSERT INTO catalog.BranchMenu(BranchId, ProductId, IsListed, LocalPrice) VALUES (?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, branchId);
                ps.setInt(2, productId);
                ps.setBoolean(3, available);
                if (localPrice == null) ps.setNull(4, Types.DECIMAL); else ps.setBigDecimal(4, localPrice);
                ps.executeUpdate();
            }
        }
    }

    /** Chỉ đổi cờ 86 (+ ETA quay lại) — Barista toggle, giữ nguyên giá/available.
     *  Mở bán lại (is86=false) luôn xoá ETA; báo hết (is86=true) ghi ETA nếu có. */
    public int updateIs86(Connection conn, int branchId, int productId, boolean is86,
                           java.sql.Timestamp backInEta) throws SQLException {
        final String sql = "UPDATE catalog.BranchMenu SET IsTemporarilyUnavailable=?, BackInEta=? WHERE BranchId=? AND ProductId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, is86);
            if (!is86 || backInEta == null) ps.setNull(2, Types.TIMESTAMP);
            else ps.setTimestamp(2, backInEta);
            ps.setInt(3, branchId);
            ps.setInt(4, productId);
            return ps.executeUpdate();
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
