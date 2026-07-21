package com.cafe.dao.shared;

import com.cafe.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDao {

    private static final String SELECT =
        "SELECT oi.OrderItemId, oi.OrderId, oi.ProductId, oi.Quantity, oi.UnitPrice, oi.Size, oi.IceLevel, oi.SugarLevel, oi.Note, oi.Status, " +
        "       oi.StartedAt, oi.DoneAt, oi.ServedAt, oi.BaristaId, oi.PreparedBy, " +
        "       oi.HasIssue, oi.IssueReason, oi.IssueReportedBy, oi.IssueReportedAt, " +
        "       oi.RemakeCount, oi.RemakeInventoryReserved, oi.HandoverLocation, oi.PickedUpBy, oi.PickedUpAt, " +
        "       DATEDIFF(SECOND, o.CreatedAt, SYSUTCDATETIME()) AS WaitedSeconds, " +
        "       CASE WHEN oi.StartedAt IS NULL THEN NULL " +
        "            ELSE DATEDIFF(SECOND, oi.StartedAt, SYSUTCDATETIME()) END AS MakingSeconds, " +
        "       CASE WHEN oi.DoneAt IS NULL THEN NULL " +
        "            ELSE DATEDIFF(SECOND, oi.DoneAt, SYSUTCDATETIME()) END AS ServeWaitSeconds, " +
        "       p.Name AS ProductName, p.PrepSeconds, c.Name AS CategoryName, o.BranchId AS OrderBranchId, " +
        "       o.OrderType, o.CreatedAt AS OrderCreatedAt, o.PickupCode, o.TableSessionId, dt.TableNumber, ts.Status AS SessionStatus, " +
        "       bu.FullName AS BaristaName, cu.FullName AS PreparedByName " +
        "FROM sales.OrderItem oi " +
        "JOIN catalog.Product p ON p.ProductId=oi.ProductId " +
        "JOIN catalog.Category c ON c.CategoryId=p.CategoryId " +
        "JOIN sales.Orders o    ON o.OrderId=oi.OrderId " +
        "LEFT JOIN sales.TableSession ts ON ts.TableSessionId=o.TableSessionId " +
        "LEFT JOIN sales.DiningTable  dt ON dt.DiningTableId=ts.DiningTableId " +
        "LEFT JOIN iam.[User] bu ON bu.UserId=oi.BaristaId " +
        "LEFT JOIN iam.[User] cu ON cu.UserId=oi.PreparedBy ";

    public int insert(Connection conn, OrderItem it) throws SQLException {
        final String sql = "INSERT INTO sales.OrderItem(OrderId, ProductId, Quantity, UnitPrice, Size, IceLevel, SugarLevel, Note, Status) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, it.getOrderId());
            ps.setInt(2, it.getProductId());
            ps.setInt(3, it.getQuantity());
            ps.setBigDecimal(4, it.getUnitPrice());
            ps.setString(5, it.getSize());
            ps.setString(6, it.getIceLevel());
            ps.setString(7, it.getSugarLevel());
            if (it.getNote() == null) ps.setNull(8, java.sql.Types.NVARCHAR); else ps.setString(8, it.getNote());
            ps.setString(9, it.getStatus() == null ? "WAITING" : it.getStatus());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public OrderItem findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE oi.OrderItemId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<OrderItem> findByOrder(Connection conn, int orderId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE oi.OrderId=? ORDER BY oi.OrderItemId")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Hàng chờ/đang pha, dùng cho dashboard cũ. */
    public List<OrderItem> findKdsQueue(Connection conn, int branchId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status IN ('WAITING','MAKING') " +
            "ORDER BY oi.Priority DESC, o.CreatedAt ASC, oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Quầy pha chế. Mỗi phần tử là đúng một dòng món, không gom theo đơn.
     * BLOCKED nằm trong danh sách để món bị chặn vẫn hiện ở khu "Cần xử lý" —
     * bỏ ra thì món biến mất khỏi mọi màn barista và khách chờ mãi không ai biết.
     */
    public List<OrderItem> findBaristaWorkbench(Connection conn, int branchId) throws SQLException {
        return findBaristaWorkbench(conn, branchId, null);
    }

    /**
     * @param businessDayStartUtc mốc đầu ngày kinh doanh; món tạo TRƯỚC mốc này bị loại khỏi
     *        hàng chờ hiện tại (xem {@link #findStaleItems}). Truyền null để lấy tất cả.
     */
    public List<OrderItem> findBaristaWorkbench(Connection conn, int branchId,
                                                java.time.LocalDateTime businessDayStartUtc) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status IN ('WAITING','MAKING','READY','BLOCKED') " +
            (businessDayStartUtc == null ? "" : "AND o.CreatedAt >= ? ") +
            "ORDER BY o.CreatedAt, oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            if (businessDayStartUtc != null) ps.setTimestamp(2, Timestamp.valueOf(businessDayStartUtc));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Món còn dang dở nhưng thuộc ngày kinh doanh TRƯỚC — khu "Đơn treo cần xử lý" cho quản lý.
     * Tách ra để hàng chờ hiện tại không bị rác cũ làm đỏ toàn bộ và làm lệch thống kê trễ giờ.
     */
    public List<OrderItem> findStaleItems(Connection conn, int branchId,
                                          java.time.LocalDateTime businessDayStartUtc) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status IN ('WAITING','MAKING','READY','BLOCKED') " +
            "AND o.CreatedAt < ? ORDER BY o.CreatedAt, oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setTimestamp(2, Timestamp.valueOf(businessDayStartUtc));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * Lịch sử các món đã pha xong trong một khoảng thời gian theo giờ UTC.
     * Chỉ lấy các món đã pha xong thuộc đúng chi nhánh; {@code DoneAt} là mốc hoàn tất
     * để món đã giao vẫn xuất hiện trong bàn giao ca.
     */
    public List<OrderItem> findBrewedToday(Connection conn, int branchId,
                                            java.time.LocalDateTime fromUtc,
                                            java.time.LocalDateTime toUtc) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE o.BranchId=? AND oi.Status IN ('READY','PICKED_UP','SERVED') " +
            "AND oi.DoneAt >= ? AND oi.DoneAt < ? " +
            "ORDER BY oi.DoneAt DESC, oi.OrderItemId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setTimestamp(2, Timestamp.valueOf(fromUtc));
            ps.setTimestamp(3, Timestamp.valueOf(toUtc));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Lịch sử món đã pha xong hôm nay theo trang; tìm kiếm/lọc/paging chạy ở database. */
    public List<OrderItem> findBrewedTodayPage(Connection conn, int branchId,
                                               java.time.LocalDateTime fromUtc,
                                               java.time.LocalDateTime toUtc,
                                               String query, String status, String orderType,
                                               int offset, int pageSize) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT + brewedTodayWhere(query, status, orderType) +
            "ORDER BY oi.DoneAt DESC, oi.OrderItemId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = bindBrewedTodayFilters(ps, 1, branchId, fromUtc, toUtc, query, status, orderType);
            ps.setInt(idx++, Math.max(0, offset));
            ps.setInt(idx, pageSize);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public int countBrewedToday(Connection conn, int branchId,
                                java.time.LocalDateTime fromUtc,
                                java.time.LocalDateTime toUtc,
                                String query, String status, String orderType) throws SQLException {
        final String sql =
            "SELECT COUNT(*) FROM sales.OrderItem oi " +
            "JOIN catalog.Product p ON p.ProductId=oi.ProductId " +
            "JOIN catalog.Category c ON c.CategoryId=p.CategoryId " +
            "JOIN sales.Orders o ON o.OrderId=oi.OrderId " +
            "LEFT JOIN sales.TableSession ts ON ts.TableSessionId=o.TableSessionId " +
            "LEFT JOIN sales.DiningTable dt ON dt.DiningTableId=ts.DiningTableId " +
            "LEFT JOIN iam.[User] cu ON cu.UserId=oi.PreparedBy " +
            brewedTodayWhere(query, status, orderType);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindBrewedTodayFilters(ps, 1, branchId, fromUtc, toUtc, query, status, orderType);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        }
        return 0;
    }

    /** Bảng lấy món: READY của chi nhánh, đơn còn ACTIVE. Cũ nhất trước (tie-break theo Id cho ổn định). */
    public List<OrderItem> findReady(Connection conn, int branchId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='READY' " +
            "ORDER BY oi.DoneAt, oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** B2 · Món vừa giao gần đây (SERVED, ServedAt trong {@code minutes} phút) để hoàn tác giao nhầm. Mới giao trước. */
    public List<OrderItem> findRecentlyServed(Connection conn, int branchId, int minutes) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT +
            "WHERE o.BranchId=? AND oi.Status='SERVED' AND oi.ServedAt IS NOT NULL " +
            "  AND oi.ServedAt >= DATEADD(MINUTE, ?, SYSUTCDATETIME()) " +
            "ORDER BY oi.ServedAt DESC, oi.OrderItemId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, -Math.abs(minutes));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Món nhân viên đã nhận khỏi quầy, đang mang giao khách. */
    public List<OrderItem> findPickedUp(Connection conn, int branchId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT + "WHERE o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='PICKED_UP' "
                + "ORDER BY oi.PickedUpAt,oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các món đang mở (chưa SERVED/CANCELLED) của một danh sách đơn — gom 1 query cho màn Pickup (tránh N+1). */
    public List<OrderItem> findByOrders(Connection conn, List<Integer> orderIds) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        if (orderIds == null || orderIds.isEmpty()) return out;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < orderIds.size(); i++) in.append(i == 0 ? "?" : ",?");
        final String sql = SELECT + "WHERE oi.OrderId IN (" + in + ") ORDER BY oi.OrderId, oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Integer id : orderIds) ps.setInt(idx++, id);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** Các món của 1 phiên bàn (cho khách theo dõi). */
    public List<OrderItem> findBySession(Connection conn, int sessionId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        final String sql = SELECT + "WHERE o.TableSessionId=? ORDER BY oi.OrderItemId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /**
     * KPI bàn giao ca (B7) trong khoảng [fromUtc, toUtc) tại chi nhánh (mốc do Service tính theo giờ VN):
     *  - Cups   = số ly PHA XONG (DoneAt trong khoảng) — KHÔNG đòi StartedAt, nên đếm cả món pha nhanh
     *             bấm READY thẳng từ WAITING (throughput không bị thiếu).
     *  - AvgSec = lead-time TB = AVG(DoneAt − StartedAt), CHỈ trên món có cả StartedAt & DoneAt
     *             (món chưa từng "bắt đầu pha" không làm loãng tốc độ pha thực).
     * Trả về {avgLeadSeconds (null → -1), cupCount}.
     */
    public long[] leadTimeStats(Connection conn, int branchId,
                                java.time.LocalDateTime fromUtc, java.time.LocalDateTime toUtc) throws SQLException {
        return leadTimeStats(conn, branchId, fromUtc, toUtc, null);
    }

    public long[] leadTimeStats(Connection conn, int branchId,
                                java.time.LocalDateTime fromUtc, java.time.LocalDateTime toUtc,
                                Integer userId) throws SQLException {
        boolean filterUser = userId != null && userId > 0;
        String userWhere = filterUser ? " AND oi.PreparedBy=? " : " ";
        final String sql =
            "SELECT " +
            // SUM(Quantity) chứ không COUNT(*): một dòng món 3 ly phải tính là 3.
            // userWhere giữ khả năng lọc theo người pha cho thẻ KPI cá nhân.
            "  (SELECT ISNULL(SUM(oi.Quantity),0) FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId " +
            "     WHERE o.BranchId=? AND oi.DoneAt >= ? AND oi.DoneAt < ?" + userWhere + ") AS Cups, " +
            "  (SELECT AVG(CAST(DATEDIFF(SECOND, oi.StartedAt, oi.DoneAt) AS BIGINT)) " +
            "     FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId " +
            "     WHERE o.BranchId=? AND oi.StartedAt IS NOT NULL AND oi.DoneAt IS NOT NULL " +
            "       AND oi.DoneAt >= ? AND oi.DoneAt < ?" + userWhere + ") AS AvgSec";
        Timestamp from = Timestamp.valueOf(fromUtc);
        Timestamp to = Timestamp.valueOf(toUtc);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, branchId); ps.setTimestamp(idx++, from); ps.setTimestamp(idx++, to);
            if (filterUser) ps.setInt(idx++, userId);
            ps.setInt(idx++, branchId); ps.setTimestamp(idx++, from); ps.setTimestamp(idx++, to);
            if (filterUser) ps.setInt(idx++, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long cups = rs.getLong("Cups");
                    long avg = rs.getLong("AvgSec");
                    boolean avgNull = rs.wasNull();
                    return new long[]{ avgNull ? -1L : avg, cups };
                }
            }
        }
        return new long[]{ -1L, 0L };
    }

    /** Bump (B1): đẩy món lên đầu hàng chờ — Priority = max hiện tại CỦA CHI NHÁNH + 1 (không nhảy chéo chi nhánh). */
    public void bump(Connection conn, int orderItemId, int branchId) throws SQLException {
        final String sql =
            "UPDATE oi SET oi.Priority = (" +
            "    SELECT ISNULL(MAX(oi2.Priority),0)+1 FROM sales.OrderItem oi2 " +
            "    JOIN sales.Orders o2 ON o2.OrderId=oi2.OrderId WHERE o2.BranchId=?) " +
            "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId " +
            "WHERE oi.OrderItemId=? AND o.BranchId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, orderItemId);
            ps.setInt(3, branchId);
            ps.executeUpdate();
        }
    }

    /** WAITING → MAKING, lưu chủ sở hữu trong cùng câu UPDATE để khóa claim. */
    public int claim(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='MAKING',oi.BaristaId=?,oi.StartedAt=SYSUTCDATETIME() "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='WAITING'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, baristaId); ps.setInt(2, orderItemId); ps.setInt(3, branchId);
            return ps.executeUpdate();
        }
    }

    /** Chỉ người đã nhận món mới được hoàn thành. */
    public int completeClaimed(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        return completeClaimed(conn, orderItemId, branchId, baristaId, null);
    }

    /** Hoàn thành + ghi vị trí đặt món (nơi thu ngân ra lấy). handoverLocation null = không đổi ghi NULL. */
    public int completeClaimed(Connection conn, int orderItemId, int branchId, int baristaId,
                               String handoverLocation) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='READY',oi.DoneAt=SYSUTCDATETIME(),oi.PreparedBy=?,"
                + "oi.HasIssue=0,oi.IssueReason=NULL,oi.RemakeInventoryReserved=0,oi.HandoverLocation=? "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, baristaId);
            if (handoverLocation == null) ps.setNull(2, java.sql.Types.NVARCHAR); else ps.setString(2, handoverLocation);
            ps.setInt(3, orderItemId); ps.setInt(4, branchId); ps.setInt(5, baristaId);
            return ps.executeUpdate();
        }
    }

    public int returnToQueue(Connection conn, int orderItemId, int branchId, int baristaId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.BaristaId=NULL,oi.StartedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='MAKING' AND oi.BaristaId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); ps.setInt(3, baristaId);
            return ps.executeUpdate();
        }
    }

    /** Gắn cờ sự cố nhưng giữ trạng thái để card không biến mất khỏi người đang xử lý. */
    public int reportIssue(Connection conn, int orderItemId, int branchId, int userId, String reason) throws SQLException {
        final String sql = "UPDATE oi SET oi.HasIssue=1,oi.IssueReason=?,oi.IssueReportedBy=?,oi.IssueReportedAt=SYSUTCDATETIME() "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status IN ('WAITING','MAKING') "
                + "AND (oi.Status='WAITING' OR oi.BaristaId=?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason); ps.setInt(2, userId); ps.setInt(3, orderItemId);
            ps.setInt(4, branchId); ps.setInt(5, userId);
            return ps.executeUpdate();
        }
    }

    /**
     * WAITING/MAKING → BLOCKED: món không pha được (hết nguyên liệu, hỏng máy, ngừng bán).
     * Nhả luôn người nhận + mốc bắt đầu vì món đã rời khỏi luồng pha; giữ lý do để hiện ở khu "Cần xử lý".
     * Guard giống reportIssue: món đang pha thì chỉ chính chủ được chặn.
     */
    public int blockItem(Connection conn, int orderItemId, int branchId, int userId, String reason) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='BLOCKED',oi.HasIssue=1,oi.IssueReason=?,"
                + "oi.IssueReportedBy=?,oi.IssueReportedAt=SYSUTCDATETIME(),oi.BaristaId=NULL,oi.StartedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' "
                + "AND oi.Status IN ('WAITING','MAKING') AND (oi.Status='WAITING' OR oi.BaristaId=?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason); ps.setInt(2, userId); ps.setInt(3, orderItemId);
            ps.setInt(4, branchId); ps.setInt(5, userId);
            return ps.executeUpdate();
        }
    }

    /** BLOCKED → WAITING: nguyên liệu/máy đã có lại, trả món về hàng chờ và xoá sạch cờ sự cố. */
    public int unblockItem(Connection conn, int orderItemId, int branchId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.HasIssue=0,oi.IssueReason=NULL,"
                + "oi.IssueReportedBy=NULL,oi.IssueReportedAt=NULL "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND o.Status='ACTIVE' AND oi.Status='BLOCKED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId);
            return ps.executeUpdate();
        }
    }

    /** Đếm các dòng món BLOCKED còn lại trong chi nhánh có dùng một trong các nguyên liệu vừa kiểm kê. */
    public int countBlockedUsingIngredients(Connection conn, int branchId,
                                            java.util.Collection<Integer> ingredientIds) throws SQLException {
        if (ingredientIds == null || ingredientIds.isEmpty()) return 0;
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < ingredientIds.size(); i++) in.append(i == 0 ? "?" : ",?");
        final String sql =
            "SELECT COUNT(DISTINCT oi.OrderItemId) " +
            "FROM sales.OrderItem oi " +
            "JOIN sales.Orders o ON o.OrderId = oi.OrderId " +
            "JOIN catalog.ProductRecipe pr ON pr.ProductId = oi.ProductId " +
            "WHERE o.BranchId = ? AND o.Status = 'ACTIVE' AND oi.Status = 'BLOCKED' " +
            "AND pr.IngredientId IN (" + in + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, branchId);
            for (Integer ingredientId : ingredientIds) ps.setInt(idx++, ingredientId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    /** READY → REMAKE là claim chuyển tiếp, chống hai người tạo remake trùng. */
    public int beginRemake(Connection conn, int orderItemId, int branchId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='REMAKE' FROM sales.OrderItem oi "
                + "JOIN sales.Orders o ON o.OrderId=oi.OrderId WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='READY'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderItemId); ps.setInt(2, branchId); return ps.executeUpdate();
        }
    }

    public void finishRemake(Connection conn, int orderItemId, int branchId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='WAITING',oi.Priority=(SELECT ISNULL(MAX(x.Priority),0)+1 "
                + "FROM sales.OrderItem x JOIN sales.Orders xo ON xo.OrderId=x.OrderId WHERE xo.BranchId=?),"
                + "oi.RemakeCount=oi.RemakeCount+1,oi.RemakeInventoryReserved=1,oi.BaristaId=NULL,oi.PreparedBy=NULL,"
                + "oi.StartedAt=NULL,oi.DoneAt=NULL,oi.HasIssue=0,oi.IssueReason=NULL "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='REMAKE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setInt(2, orderItemId); ps.setInt(3, branchId); ps.executeUpdate();
        }
    }

    public int pickUp(Connection conn, int orderItemId, int branchId, int userId) throws SQLException {
        final String sql = "UPDATE oi SET oi.Status='PICKED_UP',oi.PickedUpBy=?,oi.PickedUpAt=SYSUTCDATETIME() "
                + "FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId "
                + "WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status='READY'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, orderItemId); ps.setInt(3, branchId); return ps.executeUpdate();
        }
    }

    /** Đổi trạng thái + đóng dấu thời gian theo trạng thái mới. */
    public void updateStatus(Connection conn, int orderItemId, String status, boolean stampStarted, boolean stampDone) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE sales.OrderItem SET Status=?");
        if (stampStarted) sql.append(", StartedAt=SYSUTCDATETIME()");
        if (stampDone)    sql.append(", DoneAt=SYSUTCDATETIME()");
        sql.append(" WHERE OrderItemId=?");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, status);
            ps.setInt(2, orderItemId);
            ps.executeUpdate();
        }
    }

    /**
     * ★ Đổi trạng thái CÓ ĐIỀU KIỆN + scope chi nhánh, NGUYÊN TỬ (một câu UPDATE ở DB).
     * Chỉ đổi nếu món đang ở một trong {@code expectedStatuses} VÀ thuộc {@code branchId}.
     * Trả số dòng đổi (0 hoặc 1). Dùng làm hàng rào chống double-deduct khi 2 barista thao tác
     * song song: chỉ request thắng cuộc "claim" được món (rows==1) mới trừ kho.
     */
    public int updateStatusIf(Connection conn, int orderItemId, String newStatus,
                              String[] expectedStatuses, int branchId,
                              boolean stampStarted, boolean stampDone) throws SQLException {
        return updateStatusIf(conn, orderItemId, newStatus, expectedStatuses, branchId,
                stampStarted, stampDone, false, false, null);
    }

    /** Overload ghi nhận người pha (thẻ KPI cá nhân). */
    public int updateStatusIf(Connection conn, int orderItemId, String newStatus,
                              String[] expectedStatuses, int branchId,
                              boolean stampStarted, boolean stampDone,
                              Integer preparedBy) throws SQLException {
        return updateStatusIf(conn, orderItemId, newStatus, expectedStatuses, branchId,
                stampStarted, stampDone, false, false, preparedBy);
    }

    /** Overload đóng/gỡ dấu ServedAt (giao khách & hoàn tác giao nhầm). */
    public int updateStatusIf(Connection conn, int orderItemId, String newStatus,
                              String[] expectedStatuses, int branchId,
                              boolean stampStarted, boolean stampDone,
                              boolean stampServed, boolean clearServed) throws SQLException {
        return updateStatusIf(conn, orderItemId, newStatus, expectedStatuses, branchId,
                stampStarted, stampDone, stampServed, clearServed, null);
    }

    public int updateStatusIf(Connection conn, int orderItemId, String newStatus,
                              String[] expectedStatuses, int branchId,
                              boolean stampStarted, boolean stampDone,
                              boolean stampServed, boolean clearServed,
                              Integer preparedBy) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE oi SET oi.Status=?");
        if (stampStarted) sql.append(", oi.StartedAt=SYSUTCDATETIME()");
        if (stampDone)    sql.append(", oi.DoneAt=SYSUTCDATETIME()");
        if (stampDone && preparedBy != null && preparedBy > 0) sql.append(", oi.PreparedBy=?");
        if (stampServed)  sql.append(", oi.ServedAt=SYSUTCDATETIME()");
        if (clearServed)  sql.append(", oi.ServedAt=NULL");
        sql.append(" FROM sales.OrderItem oi JOIN sales.Orders o ON o.OrderId=oi.OrderId ")
           .append("WHERE oi.OrderItemId=? AND o.BranchId=? AND oi.Status IN (");
        for (int i = 0; i < expectedStatuses.length; i++) sql.append(i == 0 ? "?" : ",?");
        sql.append(")");
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setString(idx++, newStatus);
            if (stampDone && preparedBy != null && preparedBy > 0) ps.setInt(idx++, preparedBy);
            ps.setInt(idx++, orderItemId);
            ps.setInt(idx++, branchId);
            for (String s : expectedStatuses) ps.setString(idx++, s);
            return ps.executeUpdate();
        }
    }

    private static String brewedTodayWhere(String query, String status, String orderType) {
        StringBuilder where = new StringBuilder(
            "WHERE o.BranchId=? AND oi.Status IN ('READY','PICKED_UP','SERVED') " +
            "AND oi.DoneAt >= ? AND oi.DoneAt < ? ");
        if (hasText(status)) where.append("AND oi.Status=? ");
        if (hasText(orderType)) where.append("AND o.OrderType=? ");
        if (hasText(query)) {
            where.append("AND (CAST(oi.OrderItemId AS NVARCHAR(20)) LIKE ? ESCAPE '\\' " +
                    "OR CAST(oi.OrderId AS NVARCHAR(20)) LIKE ? ESCAPE '\\' " +
                    "OR p.Name LIKE ? ESCAPE '\\' " +
                    "OR c.Name LIKE ? ESCAPE '\\' " +
                    "OR cu.FullName LIKE ? ESCAPE '\\' " +
                    "OR oi.HandoverLocation LIKE ? ESCAPE '\\' " +
                    "OR o.PickupCode LIKE ? ESCAPE '\\' " +
                    "OR dt.TableNumber LIKE ? ESCAPE '\\') ");
        }
        return where.toString();
    }

    private static int bindBrewedTodayFilters(PreparedStatement ps, int idx, int branchId,
                                              java.time.LocalDateTime fromUtc,
                                              java.time.LocalDateTime toUtc,
                                              String query, String status, String orderType) throws SQLException {
        ps.setInt(idx++, branchId);
        ps.setTimestamp(idx++, Timestamp.valueOf(fromUtc));
        ps.setTimestamp(idx++, Timestamp.valueOf(toUtc));
        if (hasText(status)) ps.setString(idx++, status);
        if (hasText(orderType)) ps.setString(idx++, orderType);
        if (hasText(query)) {
            String pattern = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            for (int i = 0; i < 8; i++) ps.setNString(idx++, pattern);
        }
        return idx;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private OrderItem map(ResultSet rs) throws SQLException {
        OrderItem it = new OrderItem();
        it.setOrderItemId(rs.getInt("OrderItemId"));
        it.setOrderId(rs.getInt("OrderId"));
        it.setProductId(rs.getInt("ProductId"));
        it.setQuantity(rs.getInt("Quantity"));
        it.setUnitPrice(rs.getBigDecimal("UnitPrice"));
        it.setSize(rs.getString("Size"));
        it.setIceLevel(rs.getString("IceLevel"));
        it.setSugarLevel(rs.getString("SugarLevel"));
        it.setNote(rs.getString("Note"));
        it.setStatus(rs.getString("Status"));
        Timestamp sa = rs.getTimestamp("StartedAt");
        if (sa != null) it.setStartedAt(sa.toLocalDateTime());
        Timestamp da = rs.getTimestamp("DoneAt");
        if (da != null) it.setDoneAt(da.toLocalDateTime());
        Timestamp se = rs.getTimestamp("ServedAt");
        if (se != null) it.setServedAt(se.toLocalDateTime());
        Timestamp oc = rs.getTimestamp("OrderCreatedAt");
        if (oc != null) it.setOrderCreatedAt(oc.toLocalDateTime());
        Timestamp ir = rs.getTimestamp("IssueReportedAt");
        if (ir != null) it.setIssueReportedAt(ir.toLocalDateTime());
        Timestamp pu = rs.getTimestamp("PickedUpAt");
        if (pu != null) it.setPickedUpAt(pu.toLocalDateTime());
        int barista = rs.getInt("BaristaId"); if (!rs.wasNull()) it.setBaristaId(barista);
        int completed = rs.getInt("PreparedBy"); if (!rs.wasNull()) it.setPreparedBy(completed);
        int issueBy = rs.getInt("IssueReportedBy"); if (!rs.wasNull()) it.setIssueReportedBy(issueBy);
        int pickedBy = rs.getInt("PickedUpBy"); if (!rs.wasNull()) it.setPickedUpBy(pickedBy);
        it.setHasIssue(rs.getBoolean("HasIssue"));
        it.setIssueReason(rs.getString("IssueReason"));
        it.setRemakeCount(rs.getInt("RemakeCount"));
        it.setRemakeInventoryReserved(rs.getBoolean("RemakeInventoryReserved"));
        it.setHandoverLocation(rs.getString("HandoverLocation"));
        it.setWaitedSeconds(rs.getInt("WaitedSeconds"));
        int makingSeconds = rs.getInt("MakingSeconds");
        it.setMakingSeconds(rs.wasNull() ? null : makingSeconds);
        int serveWaitSeconds = rs.getInt("ServeWaitSeconds");
        it.setServeWaitSeconds(rs.wasNull() ? null : serveWaitSeconds);
        it.setProductName(rs.getString("ProductName"));
        it.setPrepSeconds(rs.getInt("PrepSeconds"));
        it.setCategoryName(rs.getString("CategoryName"));
        it.setOrderType(rs.getString("OrderType"));
        it.setBaristaName(rs.getString("BaristaName"));
        it.setPreparedByName(rs.getString("PreparedByName"));
        it.setOrderBranchId(rs.getInt("OrderBranchId"));
        int tableSessionId = rs.getInt("TableSessionId");
        it.setTableSessionId(rs.wasNull() ? null : tableSessionId);
        it.setTableNumber(rs.getString("TableNumber"));
        it.setPickupCode(rs.getString("PickupCode"));
        it.setSessionStatus(rs.getString("SessionStatus"));
        return it;
    }
}
