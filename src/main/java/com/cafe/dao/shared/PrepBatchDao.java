package com.cafe.dao.shared;

import com.cafe.model.PrepBatch;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PrepBatchDao {

    public int insert(Connection conn, int branchId, int preppedIngredientId, BigDecimal qtyProduced,
                      java.time.LocalDateTime expiresAt, int madeBy) throws SQLException {
        final String sql = "INSERT INTO inventory.PrepBatch(BranchId, PreppedIngredientId, QuantityProduced, ExpiresAt, MadeBy) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, preppedIngredientId);
            ps.setBigDecimal(3, qtyProduced);
            if (expiresAt == null) ps.setNull(4, java.sql.Types.TIMESTAMP);
            else ps.setTimestamp(4, Timestamp.valueOf(expiresAt));
            ps.setInt(5, madeBy);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    private static final String SELECT =
        "SELECT pb.PrepBatchId, pb.BranchId, pb.PreppedIngredientId, pb.QuantityProduced, pb.MadeBy, pb.MadeAt, pb.ExpiresAt, pb.Status, pb.VoidedAt, " +
        "       i.Name AS IngName, i.Unit AS IngUnit, u.FullName AS MadeByName " +
        "FROM inventory.PrepBatch pb " +
        "JOIN catalog.Ingredient i ON i.IngredientId=pb.PreppedIngredientId " +
        "JOIN iam.[User] u ON u.UserId=pb.MadeBy ";

    public List<PrepBatch> findByBranch(Connection conn, int branchId) throws SQLException {
        List<PrepBatch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE pb.BranchId=? ORDER BY pb.MadeAt DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /** Mẻ pha tạo HÔM NAY (theo ngày VN, quy về cửa sổ UTC) — mọi trạng thái, mới nhất trước. */
    public List<PrepBatch> findTodayByBranch(Connection conn, int branchId) throws SQLException {
        Timestamp[] range = todayRange();
        List<PrepBatch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE pb.BranchId=? AND pb.MadeAt>=? AND pb.MadeAt<? ORDER BY pb.MadeAt DESC")) {
            ps.setInt(1, branchId);
            ps.setTimestamp(2, range[0]);
            ps.setTimestamp(3, range[1]);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Lấy mẻ pha hôm nay theo trang; việc tìm/lọc và OFFSET/FETCH đều thực hiện tại database. */
    public List<PrepBatch> findTodayPageByBranch(Connection conn, int branchId, String query, int ingredientId,
                                                  String expiry, String status, int offset, int pageSize) throws SQLException {
        Timestamp[] range = todayRange();
        String sql = SELECT + todayFilteredWhere(query, ingredientId, expiry, status)
                + "ORDER BY pb.MadeAt DESC, pb.PrepBatchId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<PrepBatch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindTodayFilters(ps, 1, branchId, range[0], range[1], query, ingredientId, expiry, status);
            ps.setInt(idx++, Math.max(0, offset));
            ps.setInt(idx, pageSize);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public int countTodayByBranch(Connection conn, int branchId, String query, int ingredientId,
                                  String expiry, String status) throws SQLException {
        Timestamp[] range = todayRange();
        String sql = "SELECT COUNT(*) FROM inventory.PrepBatch pb "
                + "JOIN catalog.Ingredient i ON i.IngredientId=pb.PreppedIngredientId "
                + "JOIN iam.[User] u ON u.UserId=pb.MadeBy "
                + todayFilteredWhere(query, ingredientId, expiry, status);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindTodayFilters(ps, 1, branchId, range[0], range[1], query, ingredientId, expiry, status);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public PrepBatch findById(Connection conn, int prepBatchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE pb.PrepBatchId=?")) {
            ps.setInt(1, prepBatchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Scoped lookup: callers must not load a batch belonging to another branch. */
    public PrepBatch findByIdForBranch(Connection conn, int prepBatchId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE pb.PrepBatchId=? AND pb.BranchId=?")) {
            ps.setInt(1, prepBatchId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Me ACTIVE da qua han, cat theo ExpiresAt thay vi MadeAt de bat ca me pha tu ngay truoc. */
    public List<PrepBatch> findExpiredActive(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT pb.PrepBatchId, pb.BranchId, pb.PreppedIngredientId, pb.QuantityProduced, pb.MadeBy, pb.MadeAt, pb.ExpiresAt, pb.Status, pb.VoidedAt, " +
            "       i.Name AS IngName, i.Unit AS IngUnit, u.FullName AS MadeByName, " +
            "       ISNULL(bi.QuantityOnHand, 0) AS BranchQuantityOnHand " +
            "FROM inventory.PrepBatch pb " +
            "JOIN catalog.Ingredient i ON i.IngredientId=pb.PreppedIngredientId " +
            "JOIN iam.[User] u ON u.UserId=pb.MadeBy " +
            "LEFT JOIN inventory.BranchInventory bi ON bi.BranchId=pb.BranchId AND bi.IngredientId=pb.PreppedIngredientId " +
            "WHERE pb.BranchId=? AND pb.Status='ACTIVE' AND pb.ExpiresAt<SYSUTCDATETIME() " +
            "ORDER BY pb.ExpiresAt ASC, pb.MadeAt ASC, pb.PrepBatchId ASC";
        List<PrepBatch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrepBatch batch = map(rs);
                    batch.setBranchQuantityOnHand(rs.getBigDecimal("BranchQuantityOnHand"));
                    out.add(batch);
                }
            }
        }
        return out;
    }

    /** Đánh dấu trạng thái (CANCELLED kèm VoidedAt). KHÔNG hard-delete — tồn hoàn qua txn bù. */
    public int updateStatus(Connection conn, int prepBatchId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.PrepBatch SET Status=?, VoidedAt=CASE WHEN ?='CANCELLED' THEN SYSUTCDATETIME() ELSE NULL END WHERE PrepBatchId=? AND Status='ACTIVE'")) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, prepBatchId);
            return ps.executeUpdate();
        }
    }

    /** Scoped status update; keeps the write protected even if a caller has a stale object. */
    public int updateStatusForBranch(Connection conn, int prepBatchId, int branchId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.PrepBatch SET Status=?, VoidedAt=CASE WHEN ?='CANCELLED' THEN SYSUTCDATETIME() ELSE NULL END "
                        + "WHERE PrepBatchId=? AND BranchId=? AND Status='ACTIVE'")) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, prepBatchId);
            ps.setInt(4, branchId);
            return ps.executeUpdate();
        }
    }

    public int updateQuantity(Connection conn, int prepBatchId, BigDecimal qtyProduced, BigDecimal expectedQtyProduced) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.PrepBatch SET QuantityProduced=? WHERE PrepBatchId=? AND Status='ACTIVE' AND QuantityProduced=?")) {
            ps.setBigDecimal(1, qtyProduced);
            ps.setInt(2, prepBatchId);
            ps.setBigDecimal(3, expectedQtyProduced);
            return ps.executeUpdate();
        }
    }

    /** Optimistic update scoped to the current branch. */
    public int updateQuantityForBranch(Connection conn, int prepBatchId, int branchId,
                                       BigDecimal qtyProduced, BigDecimal expectedQtyProduced) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.PrepBatch SET QuantityProduced=? "
                        + "WHERE PrepBatchId=? AND BranchId=? AND Status='ACTIVE' AND QuantityProduced=?")) {
            ps.setBigDecimal(1, qtyProduced);
            ps.setInt(2, prepBatchId);
            ps.setInt(3, branchId);
            ps.setBigDecimal(4, expectedQtyProduced);
            return ps.executeUpdate();
        }
    }

    private static Timestamp[] todayRange() {
        java.time.ZoneId vn = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        java.time.LocalDate today = java.time.LocalDate.now(vn);
        Timestamp from = Timestamp.valueOf(today.atStartOfDay(vn).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime());
        Timestamp to = Timestamp.valueOf(today.plusDays(1).atStartOfDay(vn).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime());
        return new Timestamp[]{from, to};
    }

    private static String todayFilteredWhere(String query, int ingredientId, String expiry, String status) {
        StringBuilder where = new StringBuilder("WHERE pb.BranchId=? AND pb.MadeAt>=? AND pb.MadeAt<? ");
        if (ingredientId > 0) where.append("AND pb.PreppedIngredientId=? ");
        if (hasText(status)) where.append("AND pb.Status=? ");
        if ("expired".equals(expiry)) where.append("AND pb.Status='ACTIVE' AND pb.ExpiresAt<SYSUTCDATETIME() ");
        else if ("soon".equals(expiry)) where.append("AND pb.Status='ACTIVE' AND pb.ExpiresAt>=SYSUTCDATETIME() AND pb.ExpiresAt<DATEADD(HOUR, 2, SYSUTCDATETIME()) ");
        else if ("ok".equals(expiry)) where.append("AND pb.Status='ACTIVE' AND pb.ExpiresAt>=DATEADD(HOUR, 2, SYSUTCDATETIME()) ");
        else if ("none".equals(expiry)) where.append("AND (pb.ExpiresAt IS NULL OR pb.Status<>'ACTIVE') ");
        if (hasText(query)) {
            where.append("AND (CAST(pb.PrepBatchId AS NVARCHAR(20)) LIKE ? ESCAPE '\\' "
                    + "OR i.Name LIKE ? ESCAPE '\\' OR u.FullName LIKE ? ESCAPE '\\') ");
        }
        return where.toString();
    }

    private static int bindTodayFilters(PreparedStatement ps, int idx, int branchId, Timestamp from, Timestamp to,
                                        String query, int ingredientId, String expiry, String status) throws SQLException {
        ps.setInt(idx++, branchId);
        ps.setTimestamp(idx++, from);
        ps.setTimestamp(idx++, to);
        if (ingredientId > 0) ps.setInt(idx++, ingredientId);
        if (hasText(status)) ps.setString(idx++, status);
        if (hasText(query)) {
            String pattern = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            ps.setNString(idx++, pattern);
            ps.setNString(idx++, pattern);
            ps.setNString(idx++, pattern);
        }
        return idx;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private PrepBatch map(ResultSet rs) throws SQLException {
        PrepBatch b = new PrepBatch();
        b.setPrepBatchId(rs.getInt("PrepBatchId"));
        b.setBranchId(rs.getInt("BranchId"));
        b.setPreppedIngredientId(rs.getInt("PreppedIngredientId"));
        b.setQuantityProduced(rs.getBigDecimal("QuantityProduced"));
        b.setMadeBy(rs.getInt("MadeBy"));
        Timestamp ma = rs.getTimestamp("MadeAt");
        if (ma != null) b.setMadeAt(ma.toLocalDateTime());
        Timestamp ea = rs.getTimestamp("ExpiresAt");
        if (ea != null) b.setExpiresAt(ea.toLocalDateTime());
        b.setStatus(rs.getString("Status"));
        Timestamp va = rs.getTimestamp("VoidedAt");
        if (va != null) b.setVoidedAt(va.toLocalDateTime());
        b.setPreppedIngredientName(rs.getString("IngName"));
        b.setPreppedIngredientUnit(rs.getString("IngUnit"));
        b.setMadeByName(rs.getString("MadeByName"));
        return b;
    }
}
