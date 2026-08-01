package com.cafe.dao.shared;

import com.cafe.model.WasteEvent;
import com.cafe.model.WasteEventItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** DAO inventory.WasteEntry; tên class được giữ để tránh đổi kiến trúc ngoài cụm inventory. */
public class WasteEventItemDao {
    public long insert(Connection conn, WasteEvent event, int ingredientId, BigDecimal quantity,
                       String wasteType, String reason, int loggedBy,
                       BigDecimal unitCostAtLog, String costBasis) throws SQLException {
        final String sql = "INSERT INTO inventory.WasteEntry(BranchId,EventGroupId,EventKind,Source,ProductId,"
                + "OrderItemId,CupQuantity,CauseCode,CauseDetail,ShiftAssignmentId,CreatedBy,CreatedAt,"
                + "IngredientId,Quantity,WasteType,Reason,UnitCostAtLog,CostBasis,LoggedBy) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, event.getBranchId());
            nullableString(ps, 2, event.getEventGroupId(), Types.VARCHAR);
            ps.setString(3, event.getEventKind());
            ps.setString(4, event.getSource());
            nullableInt(ps, 5, event.getProductId());
            nullableInt(ps, 6, event.getOrderItemId());
            nullableInt(ps, 7, event.getCupQuantity());
            ps.setString(8, event.getCauseCode());
            nullableString(ps, 9, event.getCauseDetail(), Types.NVARCHAR);
            nullableInt(ps, 10, event.getShiftAssignmentId());
            ps.setInt(11, event.getCreatedBy());
            ps.setTimestamp(12, Timestamp.valueOf(event.getCreatedAt()));
            ps.setInt(13, ingredientId);
            ps.setBigDecimal(14, quantity);
            ps.setString(15, wasteType);
            nullableString(ps, 16, reason, Types.NVARCHAR);
            if (unitCostAtLog == null) ps.setNull(17, Types.DECIMAL); else ps.setBigDecimal(17, unitCostAtLog);
            ps.setString(18, costBasis == null ? "UNAVAILABLE" : costBasis);
            ps.setInt(19, loggedBy);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getLong(1) : 0L; }
        }
    }

    private static final String SELECT =
        "SELECT w.WasteEntryId,w.BranchId,w.EventGroupId,w.EventKind,w.Source,w.ProductId,w.OrderItemId," +
        "       w.CupQuantity,w.CauseCode,w.CauseDetail,w.ShiftAssignmentId,w.CreatedBy,w.CreatedAt," +
        "       w.IngredientId,w.Quantity,w.WasteType,w.Reason,w.UnitCostAtLog,w.CostBasis,w.Status," +
        "       w.VoidedAt,w.LoggedBy,w.LoggedAt,p.Name AS ProductName," +
        "       i.Name AS IngName,i.Unit AS IngUnit,i.IngredientType,u.FullName AS LoggedByName " +
        "FROM inventory.WasteEntry w " +
        "JOIN catalog.Ingredient i ON i.IngredientId=w.IngredientId " +
        "JOIN iam.UserAccount u ON u.UserId=w.LoggedBy " +
        "LEFT JOIN catalog.Product p ON p.ProductId=w.ProductId ";

    public List<WasteEventItem> findByBranch(Connection conn, int branchId) throws SQLException {
        List<WasteEventItem> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE w.BranchId=? ORDER BY w.LoggedAt DESC")) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public List<WasteEventItem> findByBranchBetween(Connection conn, int branchId,
                                                     LocalDateTime fromUtc, LocalDateTime toUtc) throws SQLException {
        return findByBranchBetween(conn, branchId, fromUtc, toUtc, false);
    }

    public List<WasteEventItem> findByBranchBetween(Connection conn, int branchId,
                                                     LocalDateTime fromUtc, LocalDateTime toUtc,
                                                     boolean excludeRemake) throws SQLException {
        List<WasteEventItem> out = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE w.BranchId=? ");
        if (fromUtc != null) sql.append("AND w.LoggedAt>=? ");
        if (toUtc != null) sql.append("AND w.LoggedAt<? ");
        if (excludeRemake) sql.append(NOT_REMAKE);
        sql.append("ORDER BY w.LoggedAt DESC,w.WasteEntryId DESC");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setInt(index++, branchId);
            if (fromUtc != null) ps.setTimestamp(index++, Timestamp.valueOf(fromUtc));
            if (toUtc != null) ps.setTimestamp(index, Timestamp.valueOf(toUtc));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public List<WasteEventItem> findPageByBranchBetween(Connection conn, int branchId,
                                                         LocalDateTime fromUtc, LocalDateTime toUtc,
                                                         String query, String wasteType, String status,
                                                         int offset, int pageSize) throws SQLException {
        return findPageByBranchBetween(conn, branchId, fromUtc, toUtc, query, wasteType, status,
                false, offset, pageSize);
    }

    public List<WasteEventItem> findPageByBranchBetween(Connection conn, int branchId,
                                                         LocalDateTime fromUtc, LocalDateTime toUtc,
                                                         String query, String wasteType, String status,
                                                         boolean excludeRemake, int offset, int pageSize) throws SQLException {
        String sql = SELECT + filteredWhere(fromUtc, toUtc, query, wasteType, status, excludeRemake)
                + "ORDER BY w.LoggedAt DESC,w.WasteEntryId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<WasteEventItem> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = bindFilters(ps, 1, branchId, fromUtc, toUtc, query, wasteType, status);
            ps.setInt(index++, Math.max(0, offset));
            ps.setInt(index, pageSize);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public int countByBranchBetween(Connection conn, int branchId, LocalDateTime fromUtc, LocalDateTime toUtc,
                                    String query, String wasteType, String status) throws SQLException {
        return countByBranchBetween(conn, branchId, fromUtc, toUtc, query, wasteType, status, false);
    }

    public int countByBranchBetween(Connection conn, int branchId, LocalDateTime fromUtc, LocalDateTime toUtc,
                                    String query, String wasteType, String status,
                                    boolean excludeRemake) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inventory.WasteEntry w "
                + "JOIN catalog.Ingredient i ON i.IngredientId=w.IngredientId "
                + "JOIN iam.UserAccount u ON u.UserId=w.LoggedBy "
                + filteredWhere(fromUtc, toUtc, query, wasteType, status, excludeRemake);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilters(ps, 1, branchId, fromUtc, toUtc, query, wasteType, status);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private static final String NOT_REMAKE = "AND w.WasteType<>'REMAKE' ";

    private static String filteredWhere(LocalDateTime fromUtc, LocalDateTime toUtc,
                                        String query, String wasteType, String status, boolean excludeRemake) {
        StringBuilder where = new StringBuilder("WHERE w.BranchId=? ");
        if (fromUtc != null) where.append("AND w.LoggedAt>=? ");
        if (toUtc != null) where.append("AND w.LoggedAt<? ");
        if (excludeRemake) where.append(NOT_REMAKE);
        if (hasText(wasteType)) where.append("AND w.WasteType=? ");
        if (hasText(status)) where.append("AND w.Status=? ");
        if (hasText(query)) {
            where.append("AND (i.Name LIKE ? ESCAPE '\\' OR w.Reason LIKE ? ESCAPE '\\' OR u.FullName LIKE ? ESCAPE '\\') ");
        }
        return where.toString();
    }

    private static int bindFilters(PreparedStatement ps, int index, int branchId,
                                   LocalDateTime fromUtc, LocalDateTime toUtc,
                                   String query, String wasteType, String status) throws SQLException {
        ps.setInt(index++, branchId);
        if (fromUtc != null) ps.setTimestamp(index++, Timestamp.valueOf(fromUtc));
        if (toUtc != null) ps.setTimestamp(index++, Timestamp.valueOf(toUtc));
        if (hasText(wasteType)) ps.setString(index++, wasteType);
        if (hasText(status)) ps.setString(index++, status);
        if (hasText(query)) {
            String pattern = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            ps.setNString(index++, pattern);
            ps.setNString(index++, pattern);
            ps.setNString(index++, pattern);
        }
        return index;
    }

    public List<WasteEventItem> findActiveRemakeLinesOfLatestEvent(Connection conn, int branchId,
                                                                    int orderItemId) throws SQLException {
        final String sql = SELECT
                + "WHERE w.BranchId=? AND w.Status='ACTIVE' AND w.WasteType='REMAKE' "
                + "AND w.EventGroupId=(SELECT TOP 1 e.EventGroupId FROM inventory.WasteEntry e "
                + "                    WHERE e.BranchId=? AND e.OrderItemId=? AND e.EventKind='REMAKE' "
                + "                    ORDER BY e.CreatedAt DESC,e.WasteEntryId DESC) "
                + "ORDER BY w.WasteEntryId";
        List<WasteEventItem> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, branchId);
            ps.setInt(3, orderItemId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public WasteEventItem findById(Connection conn, long wasteEntryId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE w.WasteEntryId=?")) {
            ps.setLong(1, wasteEntryId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public WasteEventItem findByIdForBranch(Connection conn, long wasteEntryId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                SELECT + "WHERE w.WasteEntryId=? AND w.BranchId=?")) {
            ps.setLong(1, wasteEntryId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int updateForBranch(Connection conn, long wasteEntryId, int branchId, BigDecimal quantity,
                               String wasteType, String reason, BigDecimal expectedQuantity) throws SQLException {
        final String sql = "UPDATE inventory.WasteEntry SET Quantity=?,WasteType=?,Reason=? "
                + "WHERE WasteEntryId=? AND BranchId=? AND Status='ACTIVE' AND Quantity=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, quantity);
            ps.setString(2, wasteType);
            nullableString(ps, 3, reason, Types.NVARCHAR);
            ps.setLong(4, wasteEntryId);
            ps.setInt(5, branchId);
            ps.setBigDecimal(6, expectedQuantity);
            return ps.executeUpdate();
        }
    }

    public int updateStatusForBranch(Connection conn, long wasteEntryId, int branchId, String status) throws SQLException {
        final String sql = "UPDATE inventory.WasteEntry SET Status=?,"
                + "VoidedAt=CASE WHEN ?='VOIDED' THEN SYSUTCDATETIME() ELSE NULL END "
                + "WHERE WasteEntryId=? AND BranchId=? AND Status='ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setLong(3, wasteEntryId);
            ps.setInt(4, branchId);
            return ps.executeUpdate();
        }
    }

    private static WasteEventItem map(ResultSet rs) throws SQLException {
        WasteEventItem item = new WasteEventItem();
        item.setWasteEntryId(rs.getLong("WasteEntryId"));
        item.setBranchId(rs.getInt("BranchId"));
        item.setEventGroupId(rs.getString("EventGroupId"));
        item.setIngredientId(rs.getInt("IngredientId"));
        item.setQuantity(rs.getBigDecimal("Quantity"));
        item.setWasteType(rs.getString("WasteType"));
        item.setReason(rs.getString("Reason"));
        item.setLoggedBy(rs.getInt("LoggedBy"));
        Timestamp loggedAt = rs.getTimestamp("LoggedAt");
        if (loggedAt != null) item.setLoggedAt(loggedAt.toLocalDateTime());
        item.setStatus(rs.getString("Status"));
        Timestamp voidedAt = rs.getTimestamp("VoidedAt");
        if (voidedAt != null) item.setVoidedAt(voidedAt.toLocalDateTime());
        item.setIngredientName(rs.getString("IngName"));
        item.setIngredientUnit(rs.getString("IngUnit"));
        item.setIngredientType(rs.getString("IngredientType"));
        item.setLoggedByName(rs.getString("LoggedByName"));
        item.setUnitCostAtLog(rs.getBigDecimal("UnitCostAtLog"));
        item.setCostBasis(rs.getString("CostBasis"));

        WasteEvent event = new WasteEvent();
        event.setEventGroupId(rs.getString("EventGroupId"));
        event.setBranchId(rs.getInt("BranchId"));
        event.setEventKind(rs.getString("EventKind"));
        event.setSource(rs.getString("Source"));
        event.setProductId(nullableInt(rs, "ProductId"));
        event.setOrderItemId(nullableInt(rs, "OrderItemId"));
        event.setCupQuantity(nullableInt(rs, "CupQuantity"));
        event.setCauseCode(rs.getString("CauseCode"));
        event.setCauseDetail(rs.getString("CauseDetail"));
        event.setShiftAssignmentId(nullableInt(rs, "ShiftAssignmentId"));
        event.setCreatedBy(rs.getInt("CreatedBy"));
        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) event.setCreatedAt(createdAt.toLocalDateTime());
        event.setProductName(rs.getString("ProductName"));
        item.setWasteEvent(event);
        return item;
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
    private static void nullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER); else ps.setInt(index, value);
    }
    private static void nullableString(PreparedStatement ps, int index, String value, int sqlType) throws SQLException {
        if (value == null || value.isBlank()) ps.setNull(index, sqlType); else ps.setString(index, value);
    }
}
