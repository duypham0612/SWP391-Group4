package com.cafe.dao.barista;

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
        return insert(conn, branchId, preppedIngredientId, qtyProduced, expiresAt, madeBy, null);
    }

    public int insert(Connection conn, int branchId, int preppedIngredientId, BigDecimal qtyProduced,
                      java.time.LocalDateTime expiresAt, int madeBy, String clientRequestId) throws SQLException {
        final String sql = "INSERT INTO inventory.PrepBatch"
                + "(BranchId, PreppedIngredientId, QuantityProduced, ExpiresAt, MadeBy, ClientRequestId) "
                + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, preppedIngredientId);
            ps.setBigDecimal(3, qtyProduced);
            if (expiresAt == null) ps.setNull(4, java.sql.Types.TIMESTAMP);
            else ps.setTimestamp(4, Timestamp.valueOf(expiresAt));
            ps.setInt(5, madeBy);
            if (clientRequestId == null) ps.setNull(6, java.sql.Types.VARCHAR);
            else ps.setString(6, clientRequestId);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    /**
     * Flow with anomaly checking (extended Contract #2): {@code requiresApproval=true} writes
     * Status='PENDING' — PREP_IN is not applied by the caller until the Manager approves it.
     */
    public int insert(Connection conn, int branchId, int preppedIngredientId, BigDecimal qtyProduced,
                      java.time.LocalDateTime expiresAt, int madeBy, String clientRequestId,
                      boolean requiresApproval) throws SQLException {
        final String sql = "INSERT INTO inventory.PrepBatch"
                + "(BranchId, PreppedIngredientId, QuantityProduced, ExpiresAt, MadeBy, ClientRequestId, "
                + "RequiresApproval, Status) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, preppedIngredientId);
            ps.setBigDecimal(3, qtyProduced);
            if (expiresAt == null) ps.setNull(4, java.sql.Types.TIMESTAMP);
            else ps.setTimestamp(4, Timestamp.valueOf(expiresAt));
            ps.setInt(5, madeBy);
            if (clientRequestId == null) ps.setNull(6, java.sql.Types.VARCHAR);
            else ps.setString(6, clientRequestId);
            ps.setBoolean(7, requiresApproval);
            ps.setString(8, requiresApproval ? "PENDING" : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    private static final String COLUMNS =
        "pb.PrepBatchId, pb.BranchId, pb.PreppedIngredientId, pb.QuantityProduced, pb.MadeBy, pb.MadeAt, " +
        "pb.ExpiresAt, pb.Status, pb.VoidedAt, pb.WrittenOffAt, pb.WriteOffWasteEntryId, pb.ClientRequestId, " +
        "pb.RequiresApproval, pb.ReviewedAt, pb.ReviewedBy, " +
        "i.Name AS IngName, i.Unit AS IngUnit, u.FullName AS MadeByName, ru.FullName AS ReviewedByName ";

    /**
     * Public because {@code dao.manager.PrepBatchApprovalDao} reuses it — barista creates the
     * batch, manager approves it, and the two DAOs now live in different role packages so
     * package-private can no longer reach across. Shared SELECT + {@link #map} instead of
     * duplicating it on the other side: duplicating it means adding a column to {@code COLUMNS}
     * would leave the manager's approval screen silently missing that column.
     */
    public static final String SELECT =
        "SELECT " + COLUMNS +
        "FROM inventory.PrepBatch pb " +
        "JOIN catalog.Ingredient i ON i.IngredientId=pb.PreppedIngredientId " +
        "JOIN iam.UserAccount u ON u.UserId=pb.MadeBy " +
        "LEFT JOIN iam.UserAccount ru ON ru.UserId=pb.ReviewedBy ";

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

    public List<PrepBatch> findRecentByBranch(Connection conn, int branchId, int limit) throws SQLException {
        String sql = SELECT + "WHERE pb.BranchId=? ORDER BY pb.MadeAt DESC, pb.PrepBatchId DESC "
                + "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        List<PrepBatch> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, Math.max(1, Math.min(limit, 100)));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public PrepBatch findByClientRequest(Connection conn, int branchId, String clientRequestId) throws SQLException {
        if (clientRequestId == null || clientRequestId.isBlank()) return null;
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE pb.BranchId=? AND pb.ClientRequestId=?")) {
            ps.setInt(1, branchId);
            ps.setString(2, clientRequestId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Prep batches created TODAY (by VN calendar day, converted to a UTC window) — every status, newest first. */
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

    /** Fetches today's prep batches page by page; searching/filtering and OFFSET/FETCH are both done at the database. */
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
                + "JOIN iam.UserAccount u ON u.UserId=pb.MadeBy "
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

    /**
     * ACTIVE batches that are past expiry and NOT yet written off as waste; cuts on ExpiresAt
     * instead of MadeAt to also catch batches prepped the day before.
     * Filters WrittenOffAt IS NULL so already-handled batches stop lingering on the Prep banner
     * and the shift handover banner.
     * The ExpiresAt ASC order feeds the FIFO allocation in ExpiryWasteCalculator - do not change this order.
     */
    public List<PrepBatch> findExpiredActive(Connection conn, int branchId) throws SQLException {
        final String sql =
            "SELECT " + COLUMNS + ", ISNULL(bi.QuantityOnHand, 0) AS BranchQuantityOnHand " +
            "FROM inventory.PrepBatch pb " +
            "JOIN catalog.Ingredient i ON i.IngredientId=pb.PreppedIngredientId " +
            "JOIN iam.UserAccount u ON u.UserId=pb.MadeBy " +
            "LEFT JOIN iam.UserAccount ru ON ru.UserId=pb.ReviewedBy " +
            "LEFT JOIN inventory.BranchInventory bi ON bi.BranchId=pb.BranchId AND bi.IngredientId=pb.PreppedIngredientId " +
            "WHERE pb.BranchId=? AND pb.Status='ACTIVE' AND pb.WrittenOffAt IS NULL AND pb.ExpiresAt<SYSUTCDATETIME() " +
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

    /** Count-only variant for dashboard badges; avoids loading and mapping the full batch list. */
    public int countExpiredActive(Connection conn, int branchId) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM inventory.PrepBatch "
                + "WHERE BranchId=? AND Status='ACTIVE' AND WrittenOffAt IS NULL "
                + "AND ExpiresAt<SYSUTCDATETIME()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Marks the status (CANCELLED comes with VoidedAt). NOT a hard-delete — stock is restored via a compensating txn. */
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

    /**
     * Closes out the lifecycle of an expired batch: attaches the recorded waste entry. The
     * WrittenOffAt IS NULL condition is the atomic gate — a double click or two baristas handling
     * the same batch at once still results in only one stock deduction being recorded.
     */
    public int markWrittenOff(Connection conn, int prepBatchId, int branchId, long wasteEntryId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.PrepBatch SET WrittenOffAt=SYSUTCDATETIME(), WriteOffWasteEntryId=? "
                        + "WHERE PrepBatchId=? AND BranchId=? AND Status='ACTIVE' AND WrittenOffAt IS NULL")) {
            ps.setLong(1, wasteEntryId);
            ps.setInt(2, prepBatchId);
            ps.setInt(3, branchId);
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
        if ("ACTIVE".equals(status)) where.append("AND pb.Status='ACTIVE' AND pb.WrittenOffAt IS NULL ");
        else if ("WRITTEN_OFF".equals(status)) where.append("AND pb.Status='ACTIVE' AND pb.WrittenOffAt IS NOT NULL ");
        else if (hasText(status)) where.append("AND pb.Status=? ");
        if ("expired".equals(expiry)) where.append("AND pb.Status='ACTIVE' AND pb.WrittenOffAt IS NULL AND pb.ExpiresAt<SYSUTCDATETIME() ");
        else if ("soon".equals(expiry)) where.append("AND pb.Status='ACTIVE' AND pb.WrittenOffAt IS NULL AND pb.ExpiresAt>=SYSUTCDATETIME() AND pb.ExpiresAt<DATEADD(HOUR, 2, SYSUTCDATETIME()) ");
        else if ("ok".equals(expiry)) where.append("AND pb.Status='ACTIVE' AND pb.WrittenOffAt IS NULL AND pb.ExpiresAt>=DATEADD(HOUR, 2, SYSUTCDATETIME()) ");
        else if ("none".equals(expiry)) where.append("AND pb.ExpiresAt IS NULL ");
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
        if (hasText(status) && !"ACTIVE".equals(status) && !"WRITTEN_OFF".equals(status))
            ps.setString(idx++, status);
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

    /** Public for the same reason as {@link #SELECT} — shared with the {@code dao.manager} package. */
    public static PrepBatch map(ResultSet rs) throws SQLException {
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
        Timestamp wo = rs.getTimestamp("WrittenOffAt");
        if (wo != null) b.setWrittenOffAt(wo.toLocalDateTime());
        long wasteEntryId = rs.getLong("WriteOffWasteEntryId");
        b.setWriteOffWasteEntryId(rs.wasNull() ? null : wasteEntryId);
        b.setClientRequestId(rs.getString("ClientRequestId"));
        b.setRequiresApproval(rs.getBoolean("RequiresApproval"));
        Timestamp rv = rs.getTimestamp("ReviewedAt");
        if (rv != null) b.setReviewedAt(rv.toLocalDateTime());
        int reviewedBy = rs.getInt("ReviewedBy");
        b.setReviewedBy(rs.wasNull() ? null : reviewedBy);
        b.setPreppedIngredientName(rs.getString("IngName"));
        b.setPreppedIngredientUnit(rs.getString("IngUnit"));
        b.setMadeByName(rs.getString("MadeByName"));
        b.setReviewedByName(rs.getString("ReviewedByName"));
        return b;
    }
}
