package com.cafe.dao.shared;

import com.cafe.model.WasteLog;
import com.cafe.model.WasteEvent;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WasteLogDao {

    public int insert(Connection conn, int branchId, int ingredientId, BigDecimal qty,
                      String wasteType, String reason, int loggedBy) throws SQLException {
        return insert(conn, branchId, ingredientId, qty, wasteType, reason, loggedBy, null, null, "LEGACY_ESTIMATE");
    }

    public int insert(Connection conn, int branchId, int ingredientId, BigDecimal qty,
                      String wasteType, String reason, int loggedBy, Long eventId,
                      BigDecimal unitCostAtLog, String costBasis) throws SQLException {
        final String sql = "INSERT INTO inventory.WasteLog(BranchId, IngredientId, Quantity, WasteType, Reason, LoggedBy,WasteEventId,UnitCostAtLog,CostBasis) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId);
            ps.setInt(2, ingredientId);
            ps.setBigDecimal(3, qty);
            ps.setString(4, wasteType);
            if (reason == null) ps.setNull(5, java.sql.Types.NVARCHAR); else ps.setString(5, reason);
            ps.setInt(6, loggedBy);
            if (eventId == null) ps.setNull(7, java.sql.Types.BIGINT); else ps.setLong(7, eventId);
            if (unitCostAtLog == null) ps.setNull(8, java.sql.Types.DECIMAL); else ps.setBigDecimal(8, unitCostAtLog);
            ps.setString(9, costBasis == null ? "UNAVAILABLE" : costBasis);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    private static final String SELECT =
        "SELECT wl.WasteLogId, wl.BranchId, wl.IngredientId, wl.Quantity, wl.WasteType, wl.Reason, wl.LoggedBy, wl.LoggedAt, wl.Status, wl.VoidedAt,wl.WasteEventId,wl.UnitCostAtLog,wl.CostBasis, " +
        "       we.EventKind,we.Source,we.ProductId,we.OrderItemId,we.CupQuantity,we.CauseCode,we.CauseDetail,wp.Name AS ProductName, " +
        "       i.Name AS IngName, i.Unit AS IngUnit, i.IngredientType, u.FullName AS LoggedByName " +
        "FROM inventory.WasteLog wl " +
        "JOIN catalog.Ingredient i ON i.IngredientId=wl.IngredientId " +
        "JOIN iam.[User] u ON u.UserId=wl.LoggedBy " +
        "LEFT JOIN inventory.WasteEvent we ON we.WasteEventId=wl.WasteEventId " +
        "LEFT JOIN catalog.Product wp ON wp.ProductId=we.ProductId ";

    public List<WasteLog> findByBranch(Connection conn, int branchId) throws SQLException {
        List<WasteLog> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE wl.BranchId=? ORDER BY wl.LoggedAt DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public List<WasteLog> findByBranchBetween(Connection conn, int branchId, LocalDateTime fromUtc, LocalDateTime toUtc) throws SQLException {
        List<WasteLog> out = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE wl.BranchId=? ");
        if (fromUtc != null) sql.append("AND wl.LoggedAt>=? ");
        if (toUtc != null) sql.append("AND wl.LoggedAt<? ");
        sql.append("ORDER BY wl.LoggedAt DESC");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, branchId);
            if (fromUtc != null) ps.setTimestamp(idx++, Timestamp.valueOf(fromUtc));
            if (toUtc != null) ps.setTimestamp(idx, Timestamp.valueOf(toUtc));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /** Lấy một trang nhật ký đã lọc tại DB, tránh tải toàn bộ lịch sử về trình duyệt. */
    public List<WasteLog> findPageByBranchBetween(Connection conn, int branchId, LocalDateTime fromUtc, LocalDateTime toUtc,
                                                   String query, String wasteType, String status,
                                                   int offset, int pageSize) throws SQLException {
        List<WasteLog> out = new ArrayList<>();
        String sql = SELECT + filteredWhere(branchId, fromUtc, toUtc, query, wasteType, status)
                + "ORDER BY wl.LoggedAt DESC, wl.WasteLogId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindFilters(ps, 1, branchId, fromUtc, toUtc, query, wasteType, status);
            ps.setInt(idx++, Math.max(0, offset));
            ps.setInt(idx, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public int countByBranchBetween(Connection conn, int branchId, LocalDateTime fromUtc, LocalDateTime toUtc,
                                    String query, String wasteType, String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inventory.WasteLog wl "
                + "JOIN catalog.Ingredient i ON i.IngredientId=wl.IngredientId "
                + "JOIN iam.[User] u ON u.UserId=wl.LoggedBy "
                + filteredWhere(branchId, fromUtc, toUtc, query, wasteType, status);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilters(ps, 1, branchId, fromUtc, toUtc, query, wasteType, status);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static String filteredWhere(int branchId, LocalDateTime fromUtc, LocalDateTime toUtc,
                                        String query, String wasteType, String status) {
        StringBuilder where = new StringBuilder("WHERE wl.BranchId=? ");
        if (fromUtc != null) where.append("AND wl.LoggedAt>=? ");
        if (toUtc != null) where.append("AND wl.LoggedAt<? ");
        if (hasText(wasteType)) where.append("AND wl.WasteType=? ");
        if (hasText(status)) where.append("AND wl.Status=? ");
        if (hasText(query)) {
            where.append("AND (i.Name LIKE ? ESCAPE '\\' OR wl.Reason LIKE ? ESCAPE '\\' OR u.FullName LIKE ? ESCAPE '\\') ");
        }
        return where.toString();
    }

    private static int bindFilters(PreparedStatement ps, int idx, int branchId, LocalDateTime fromUtc, LocalDateTime toUtc,
                                   String query, String wasteType, String status) throws SQLException {
        ps.setInt(idx++, branchId);
        if (fromUtc != null) ps.setTimestamp(idx++, Timestamp.valueOf(fromUtc));
        if (toUtc != null) ps.setTimestamp(idx++, Timestamp.valueOf(toUtc));
        if (hasText(wasteType)) ps.setString(idx++, wasteType);
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

    public WasteLog findById(Connection conn, int wasteLogId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE wl.WasteLogId=?")) {
            ps.setInt(1, wasteLogId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** Scoped lookup: a barista must never load a waste record from another branch. */
    public WasteLog findByIdForBranch(Connection conn, int wasteLogId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE wl.WasteLogId=? AND wl.BranchId=?")) {
            ps.setInt(1, wasteLogId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int update(Connection conn, int wasteLogId, BigDecimal qty, String wasteType, String reason,
                      BigDecimal expectedQty) throws SQLException {
        final String sql = "UPDATE inventory.WasteLog SET Quantity=?, WasteType=?, Reason=? WHERE WasteLogId=? AND Status='ACTIVE' AND Quantity=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, qty);
            ps.setString(2, wasteType);
            if (reason == null) ps.setNull(3, java.sql.Types.NVARCHAR); else ps.setString(3, reason);
            ps.setInt(4, wasteLogId);
            ps.setBigDecimal(5, expectedQty);
            return ps.executeUpdate();
        }
    }

    /** Optimistic update scoped to the current branch. */
    public int updateForBranch(Connection conn, int wasteLogId, int branchId, BigDecimal qty, String wasteType,
                               String reason, BigDecimal expectedQty) throws SQLException {
        final String sql = "UPDATE inventory.WasteLog SET Quantity=?, WasteType=?, Reason=? "
                + "WHERE WasteLogId=? AND BranchId=? AND Status='ACTIVE' AND Quantity=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, qty);
            ps.setString(2, wasteType);
            if (reason == null) ps.setNull(3, java.sql.Types.NVARCHAR); else ps.setString(3, reason);
            ps.setInt(4, wasteLogId);
            ps.setInt(5, branchId);
            ps.setBigDecimal(6, expectedQty);
            return ps.executeUpdate();
        }
    }

    /** Đánh dấu VOIDED (kèm VoidedAt). KHÔNG hard-delete — tồn hoàn qua txn bù. */
    public int updateStatus(Connection conn, int wasteLogId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.WasteLog SET Status=?, VoidedAt=CASE WHEN ?='VOIDED' THEN SYSUTCDATETIME() ELSE NULL END WHERE WasteLogId=? AND Status='ACTIVE'")) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, wasteLogId);
            return ps.executeUpdate();
        }
    }

    /** Scoped status update for voiding a waste log. */
    public int updateStatusForBranch(Connection conn, int wasteLogId, int branchId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inventory.WasteLog SET Status=?, VoidedAt=CASE WHEN ?='VOIDED' THEN SYSUTCDATETIME() ELSE NULL END "
                        + "WHERE WasteLogId=? AND BranchId=? AND Status='ACTIVE'")) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, wasteLogId);
            ps.setInt(4, branchId);
            return ps.executeUpdate();
        }
    }

    private WasteLog map(ResultSet rs) throws SQLException {
        WasteLog w = new WasteLog();
        w.setWasteLogId(rs.getInt("WasteLogId"));
        w.setBranchId(rs.getInt("BranchId"));
        w.setIngredientId(rs.getInt("IngredientId"));
        w.setQuantity(rs.getBigDecimal("Quantity"));
        w.setWasteType(rs.getString("WasteType"));
        w.setReason(rs.getString("Reason"));
        w.setLoggedBy(rs.getInt("LoggedBy"));
        Timestamp la = rs.getTimestamp("LoggedAt");
        if (la != null) w.setLoggedAt(la.toLocalDateTime());
        w.setStatus(rs.getString("Status"));
        Timestamp va = rs.getTimestamp("VoidedAt");
        if (va != null) w.setVoidedAt(va.toLocalDateTime());
        w.setIngredientName(rs.getString("IngName"));
        w.setIngredientUnit(rs.getString("IngUnit"));
        w.setIngredientType(rs.getString("IngredientType"));
        w.setLoggedByName(rs.getString("LoggedByName"));
        long eventId = rs.getLong("WasteEventId"); if (!rs.wasNull()) w.setWasteEventId(eventId);
        w.setUnitCostAtLog(rs.getBigDecimal("UnitCostAtLog"));
        w.setCostBasis(rs.getString("CostBasis"));
        long eventKey = rs.getLong("WasteEventId");
        if (!rs.wasNull()) {
            WasteEvent event = new WasteEvent(); event.setWasteEventId(eventKey); event.setEventKind(rs.getString("EventKind"));
            event.setSource(rs.getString("Source")); event.setProductId(nullableInt(rs, "ProductId")); event.setOrderItemId(nullableInt(rs, "OrderItemId"));
            event.setCupQuantity(nullableInt(rs, "CupQuantity")); event.setCauseCode(rs.getString("CauseCode")); event.setCauseDetail(rs.getString("CauseDetail")); event.setProductName(rs.getString("ProductName"));
            w.setWasteEvent(event);
        }
        return w;
    }

    private static Integer nullableInt(ResultSet rs, String name) throws SQLException { int value=rs.getInt(name); return rs.wasNull() ? null : value; }
}
