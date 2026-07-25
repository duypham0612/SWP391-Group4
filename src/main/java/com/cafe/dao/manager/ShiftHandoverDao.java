package com.cafe.dao.manager;

import com.cafe.model.ShiftAssignment;
import com.cafe.model.ShiftHandover;
import com.cafe.model.ShiftHandoverRecipient;
import com.cafe.model.ShiftHandoverTask;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** DAO bàn giao có người nhận và đầu việc theo ca. */
public class ShiftHandoverDao {
    private static final String HANDOVER_COLUMNS =
        "sh.ShiftHandoverId, sh.BranchId, sh.Note, sh.CreatedBy, sh.SourceShiftAssignmentId, sh.OverallStatus, sh.CreatedAt, " +
        "u.FullName AS CreatedByName, st.Name AS SourceTemplateName, st.StartTime AS SourceStartTime, st.EndTime AS SourceEndTime ";
    private static final String HANDOVER_FROM =
        "FROM hr.ShiftHandover sh JOIN iam.[User] u ON u.UserId=sh.CreatedBy " +
        "LEFT JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=sh.SourceShiftAssignmentId " +
        "LEFT JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId ";

    public ShiftAssignment findOpenSourceAssignment(Connection conn, int userId, int branchId) throws SQLException {
        final String sql = "SELECT TOP 1 sa.ShiftAssignmentId, sa.ShiftTemplateId, sa.UserId, sa.WorkDate, " +
            "st.Name AS TemplateName, st.StartTime, st.EndTime, u.FullName AS UserName, r.Code AS RoleCode " +
            "FROM hr.Attendance a JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=a.ShiftAssignmentId " +
            "JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId JOIN iam.[User] u ON u.UserId=sa.UserId " +
            "JOIN iam.Role r ON r.RoleId=u.RoleId WHERE sa.UserId=? AND st.BranchId=? " +
            "AND a.CheckInAt IS NOT NULL AND a.CheckOutAt IS NULL ORDER BY a.AttendanceId DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ShiftAssignment a = new ShiftAssignment();
                a.setShiftAssignmentId(rs.getInt("ShiftAssignmentId"));
                a.setShiftTemplateId(rs.getInt("ShiftTemplateId")); a.setUserId(rs.getInt("UserId"));
                Date workDate = rs.getDate("WorkDate"); if (workDate != null) a.setWorkDate(workDate.toLocalDate());
                a.setTemplateName(rs.getString("TemplateName"));
                java.sql.Time start = rs.getTime("StartTime"), end = rs.getTime("EndTime");
                if (start != null) a.setStartTime(start.toLocalTime()); if (end != null) a.setEndTime(end.toLocalTime());
                a.setUserName(rs.getString("UserName")); a.setRoleCode(rs.getString("RoleCode"));
                return a;
            }
        }
    }

    /** Bàn giao có thuộc chi nhánh đang thao tác không — chặn sửa dữ liệu chi nhánh khác qua POST giả mạo. */
    public boolean existsInBranch(Connection conn, int handoverId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM hr.ShiftHandover WHERE ShiftHandoverId=? AND BranchId=?")) {
            ps.setInt(1, handoverId); ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Ca này đã lập bàn giao chưa — dùng để chặn tan ca khi chưa bàn giao. */
    public boolean existsForSourceAssignment(Connection conn, int sourceAssignmentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM hr.ShiftHandover WHERE SourceShiftAssignmentId=?")) {
            ps.setInt(1, sourceAssignmentId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int insert(Connection conn, int branchId, String note, int createdBy, int sourceAssignmentId) throws SQLException {
        final String sql = "INSERT INTO hr.ShiftHandover(BranchId, Note, CreatedBy, SourceShiftAssignmentId, OverallStatus) VALUES (?,?,?,?, 'WAITING_RECEIPT')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, branchId); ps.setString(2, note); ps.setInt(3, createdBy); ps.setInt(4, sourceAssignmentId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getInt(1) : 0; }
        }
    }

    public void insertRecipient(Connection conn, int handoverId, int userId, Integer assignmentId, String type) throws SQLException {
        final String sql = "INSERT INTO hr.ShiftHandoverRecipient(ShiftHandoverId,RecipientUserId,RecipientShiftAssignmentId,RecipientType) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, handoverId); ps.setInt(2, userId);
            if (assignmentId == null) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, assignmentId);
            ps.setString(4, type); ps.executeUpdate();
        }
    }

    public void insertTask(Connection conn, int handoverId, String content) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO hr.ShiftHandoverTask(ShiftHandoverId,Content) VALUES (?,?)")) {
            ps.setInt(1, handoverId); ps.setString(2, content); ps.executeUpdate();
        }
    }

    /**
     * Mệnh đề lọc dùng chung cho đếm và lấy trang — hai câu phải khớp nhau, lệch một điều kiện là
     * tổng số trang không còn đúng với dữ liệu đang hiện.
     * scope: MINE = bàn giao gửi cho tôi, SENT = tôi gửi đi, rỗng = cả chi nhánh.
     */
    private static String filterSql(String scope, String status, String query) {
        StringBuilder sql = new StringBuilder(" WHERE sh.BranchId=?");
        if ("MINE".equals(scope)) sql.append(" AND EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient rf WHERE rf.ShiftHandoverId=sh.ShiftHandoverId AND rf.RecipientUserId=?)");
        else if ("SENT".equals(scope)) sql.append(" AND sh.CreatedBy=?");
        if (status != null && !status.isEmpty()) sql.append(" AND sh.OverallStatus=?");
        if (query != null && !query.isEmpty())
            sql.append(" AND (u.FullName LIKE ? OR sh.Note LIKE ? OR EXISTS (SELECT 1 FROM hr.ShiftHandoverTask tf WHERE tf.ShiftHandoverId=sh.ShiftHandoverId AND tf.Content LIKE ?))");
        return sql.toString();
    }

    /** Gán tham số theo đúng thứ tự {@link #filterSql} sinh ra; trả về vị trí tham số kế tiếp. */
    private static int bindFilter(PreparedStatement ps, int index, int branchId, int userId, String scope, String status, String query) throws SQLException {
        ps.setInt(index++, branchId);
        if ("MINE".equals(scope) || "SENT".equals(scope)) ps.setInt(index++, userId);
        if (status != null && !status.isEmpty()) ps.setString(index++, status);
        if (query != null && !query.isEmpty()) { String like = "%" + query + "%"; ps.setString(index++, like); ps.setString(index++, like); ps.setString(index++, like); }
        return index;
    }

    public int countByFilter(Connection conn, int branchId, int userId, String scope, String status, String query) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM hr.ShiftHandover sh JOIN iam.[User] u ON u.UserId=sh.CreatedBy" + filterSql(scope, status, query);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindFilter(ps, 1, branchId, userId, scope, status, query);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    /**
     * Bàn giao đang chờ chính tôi xác nhận luôn nằm đầu trang — việc cần làm không bị đẩy xuống dưới lịch sử.
     * Cờ ưu tiên nằm ở SELECT rồi ORDER BY theo alias, nên tham số userId là tham số ĐẦU TIÊN của câu lệnh.
     */
    public List<ShiftHandover> findPage(Connection conn, int branchId, int userId, String scope, String status, String query, int offset, int limit) throws SQLException {
        final String sql = "SELECT " + HANDOVER_COLUMNS
            + ", CASE WHEN EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient ro WHERE ro.ShiftHandoverId=sh.ShiftHandoverId AND ro.RecipientUserId=? AND ro.AcknowledgedAt IS NULL) THEN 0 ELSE 1 END AS NeedsMyAck "
            + HANDOVER_FROM + filterSql(scope, status, query)
            + " ORDER BY NeedsMyAck, sh.CreatedAt DESC, sh.ShiftHandoverId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<ShiftHandover> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int index = bindFilter(ps, 2, branchId, userId, scope, status, query);
            ps.setInt(index++, offset); ps.setInt(index, limit);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(mapHandover(rs)); }
        }
        hydrateAll(conn, result, userId);
        return result;
    }

    /**
     * Bàn giao gửi tới quản lý (dự phòng khi không có ca barista kế tiếp), chỉ những cái CHƯA xong.
     * Lấy cả cái đã hoàn tất thì cảnh báo trên dashboard quản lý không bao giờ tắt và số đếm cứ tăng mãi.
     */
    public List<ShiftHandover> findManagerFallbacks(Connection conn, int branchId, int managerUserId, int limit) throws SQLException {
        final String sql = "SELECT TOP (?) " + HANDOVER_COLUMNS + HANDOVER_FROM
            + " WHERE sh.BranchId=? AND sh.OverallStatus <> 'COMPLETED'"
            + " AND EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient rm WHERE rm.ShiftHandoverId=sh.ShiftHandoverId AND rm.RecipientUserId=?)"
            + " ORDER BY sh.CreatedAt DESC, sh.ShiftHandoverId DESC";
        List<ShiftHandover> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit); ps.setInt(2, branchId); ps.setInt(3, managerUserId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(mapHandover(rs)); }
        }
        hydrateAll(conn, result, managerUserId);
        return result;
    }

    /**
     * Ba con số đầu màn trong một lượt đi DB: bàn giao chờ tôi xác nhận, việc tôi đã nhận mà chưa xong,
     * bàn giao cả chi nhánh chưa ai nhận.
     */
    public int[] summaryCounts(Connection conn, int branchId, int userId) throws SQLException {
        final String sql =
            "SELECT (SELECT COUNT(*) FROM hr.ShiftHandoverRecipient r JOIN hr.ShiftHandover h ON h.ShiftHandoverId=r.ShiftHandoverId " +
            "        WHERE h.BranchId=? AND r.RecipientUserId=? AND r.AcknowledgedAt IS NULL AND h.OverallStatus <> 'LEGACY'), " +
            "       (SELECT COUNT(*) FROM hr.ShiftHandoverTask t JOIN hr.ShiftHandover h ON h.ShiftHandoverId=t.ShiftHandoverId " +
            "        JOIN hr.ShiftHandoverRecipient r ON r.ShiftHandoverId=h.ShiftHandoverId AND r.RecipientUserId=? AND r.AcknowledgedAt IS NOT NULL " +
            "        WHERE h.BranchId=? AND t.Status <> 'DONE'), " +
            "       (SELECT COUNT(*) FROM hr.ShiftHandover h WHERE h.BranchId=? AND h.OverallStatus='WAITING_RECEIPT')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setInt(2, userId); ps.setInt(3, userId); ps.setInt(4, branchId); ps.setInt(5, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)} : new int[]{0, 0, 0}; }
        }
    }

    /** Việc chưa xong của các bàn giao tôi đã tiếp nhận — nguồn để chuyển tiếp sang ca sau. */
    public List<ShiftHandoverTask> findOpenTasksForUser(Connection conn, int branchId, int userId, int limit) throws SQLException {
        final String sql = "SELECT TOP (?) t.ShiftHandoverTaskId, t.Content, t.Status, u.FullName AS CreatedByName " +
            "FROM hr.ShiftHandoverTask t JOIN hr.ShiftHandover h ON h.ShiftHandoverId=t.ShiftHandoverId " +
            "JOIN hr.ShiftHandoverRecipient r ON r.ShiftHandoverId=h.ShiftHandoverId AND r.RecipientUserId=? AND r.AcknowledgedAt IS NOT NULL " +
            "JOIN iam.[User] u ON u.UserId=h.CreatedBy WHERE h.BranchId=? AND t.Status <> 'DONE' " +
            "ORDER BY h.CreatedAt DESC, t.ShiftHandoverTaskId";
        List<ShiftHandoverTask> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit); ps.setInt(2, userId); ps.setInt(3, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {
                ShiftHandoverTask t = new ShiftHandoverTask();
                t.setShiftHandoverTaskId(rs.getInt(1)); t.setContent(rs.getString(2)); t.setStatus(rs.getString(3));
                t.setSourceLabel(rs.getString(4)); result.add(t);
            }}
        }
        return result;
    }

    /**
     * Đếm đúng các bàn giao mà người đang xem có thể nhận thay. Điều kiện phải khớp với
     * {@link #claimStale} và ShiftHandover.applyViewer để số cảnh báo, nút và quyền ghi không lệch.
     */
    public int countClaimableInBranch(Connection conn, int branchId, int userId, int staleAfterHours)
            throws SQLException {
        final String sql = "SELECT COUNT(*) FROM hr.ShiftHandover sh WHERE sh.BranchId=? AND sh.CreatedBy<>? " +
            "AND sh.OverallStatus='WAITING_RECEIPT' " +
            "AND NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient a WHERE a.ShiftHandoverId=sh.ShiftHandoverId AND a.AcknowledgedAt IS NOT NULL) " +
            "AND NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient m WHERE m.ShiftHandoverId=sh.ShiftHandoverId AND m.RecipientUserId=?) " +
            "AND (NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient r WHERE r.ShiftHandoverId=sh.ShiftHandoverId) " +
            "     OR sh.CreatedAt < DATEADD(HOUR, -?, SYSUTCDATETIME()))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setInt(2, userId); ps.setInt(3, userId);
            ps.setInt(4, staleAfterHours);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    /**
     * Ca sau tự đứng ra nhận một bàn giao mồ côi hoặc quá hạn chưa ai xác nhận: chèn thẳng dòng
     * người nhận đã xác nhận.
     *
     * <p>Toàn bộ điều kiện nằm trong mệnh đề WHERE của INSERT…SELECT chứ không kiểm ở Java, nên hai
     * barista bấm cùng lúc thì người sau chèn 0 dòng và nhận báo xung đột thay vì cả hai cùng nhận.
     * {@code UPDLOCK} khoá dòng bàn giao cha để hai giao dịch không cùng thấy "chưa ai xác nhận".
     * Điều kiện phải khớp với {@link #countClaimableInBranch} và ShiftHandover.applyViewer.
     */
    public boolean claimStale(Connection conn, int handoverId, int branchId, int userId,
                              Integer assignmentId, int staleAfterHours) throws SQLException {
        final String sql = "INSERT INTO hr.ShiftHandoverRecipient(ShiftHandoverId,RecipientUserId,RecipientShiftAssignmentId,RecipientType,AcknowledgedAt) " +
            "SELECT sh.ShiftHandoverId, ?, ?, 'NEXT_SHIFT', SYSUTCDATETIME() " +
            "FROM hr.ShiftHandover sh WITH (UPDLOCK, HOLDLOCK) " +
            "WHERE sh.ShiftHandoverId=? AND sh.BranchId=? AND sh.CreatedBy<>? AND sh.OverallStatus='WAITING_RECEIPT' " +
            "AND NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient a WHERE a.ShiftHandoverId=sh.ShiftHandoverId AND a.AcknowledgedAt IS NOT NULL) " +
            "AND NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient m WHERE m.ShiftHandoverId=sh.ShiftHandoverId AND m.RecipientUserId=?) " +
            "AND (NOT EXISTS (SELECT 1 FROM hr.ShiftHandoverRecipient r WHERE r.ShiftHandoverId=sh.ShiftHandoverId) " +
            "     OR sh.CreatedAt < DATEADD(HOUR, -?, SYSUTCDATETIME()))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (assignmentId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, assignmentId);
            ps.setInt(3, handoverId); ps.setInt(4, branchId); ps.setInt(5, userId);
            ps.setInt(6, userId); ps.setInt(7, staleAfterHours);
            return ps.executeUpdate() == 1;
        }
    }

    public int countUnacknowledgedForUser(Connection conn, int branchId, int userId) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM hr.ShiftHandoverRecipient r JOIN hr.ShiftHandover h ON h.ShiftHandoverId=r.ShiftHandoverId " +
            "WHERE h.BranchId=? AND r.RecipientUserId=? AND r.AcknowledgedAt IS NULL AND h.OverallStatus <> 'LEGACY'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public boolean acknowledge(Connection conn, int handoverId, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE hr.ShiftHandoverRecipient SET AcknowledgedAt=SYSUTCDATETIME() WHERE ShiftHandoverId=? AND RecipientUserId=? AND AcknowledgedAt IS NULL")) {
            ps.setInt(1, handoverId); ps.setInt(2, userId); return ps.executeUpdate() == 1;
        }
    }

    public boolean isAcknowledgedRecipient(Connection conn, int handoverId, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM hr.ShiftHandoverRecipient WHERE ShiftHandoverId=? AND RecipientUserId=? AND AcknowledgedAt IS NOT NULL")) {
            ps.setInt(1, handoverId); ps.setInt(2, userId); try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean updateTaskStatus(Connection conn, int taskId, int handoverId, String status, int userId) throws SQLException {
        final String sql = "UPDATE hr.ShiftHandoverTask SET Status=?, UpdatedBy=?, UpdatedAt=SYSUTCDATETIME() WHERE ShiftHandoverTaskId=? AND ShiftHandoverId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status); ps.setInt(2, userId); ps.setInt(3, taskId); ps.setInt(4, handoverId); return ps.executeUpdate() == 1;
        }
    }

    public void refreshOverallStatus(Connection conn, int handoverId) throws SQLException {
        int acknowledged = count(conn, "SELECT COUNT(*) FROM hr.ShiftHandoverRecipient WHERE ShiftHandoverId=? AND AcknowledgedAt IS NOT NULL", handoverId);
        int tasks = count(conn, "SELECT COUNT(*) FROM hr.ShiftHandoverTask WHERE ShiftHandoverId=?", handoverId);
        int done = count(conn, "SELECT COUNT(*) FROM hr.ShiftHandoverTask WHERE ShiftHandoverId=? AND Status='DONE'", handoverId);
        String status = overallStatus(acknowledged, tasks, done);
        try (PreparedStatement ps = conn.prepareStatement("UPDATE hr.ShiftHandover SET OverallStatus=? WHERE ShiftHandoverId=?")) {
            ps.setString(1, status); ps.setInt(2, handoverId); ps.executeUpdate();
        }
    }

    /**
     * Bàn giao hoàn tất khi đã có ít nhất một người thực sự tiếp nhận và toàn bộ đầu việc đã xong.
     * Không đòi mọi người được chỉ định cùng xác nhận: ca nhiều người hoặc người nhận thay quá hạn
     * vẫn phải đóng được bàn giao khi công việc thực tế đã hoàn tất.
     */
    static String overallStatus(int acknowledged, int tasks, int done) {
        return acknowledged > 0 && tasks > 0 && tasks == done ? "COMPLETED"
            : (acknowledged > 0 ? "IN_PROGRESS" : "WAITING_RECEIPT");
    }

    private int count(Connection conn, String sql, int handoverId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.setInt(1, handoverId); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; } }
    }

    /**
     * Nạp người nhận + đầu việc cho cả trang bằng 2 truy vấn thay vì 2 truy vấn mỗi bàn giao.
     * Danh sách bàn giao chỉ dài thêm theo thời gian nên N+1 ở đây là nợ chắc chắn phải trả.
     */
    private void hydrateAll(Connection conn, List<ShiftHandover> handovers, int currentUserId) throws SQLException {
        if (handovers.isEmpty()) return;
        Map<Integer, ShiftHandover> byId = new LinkedHashMap<>();
        for (ShiftHandover h : handovers) { h.setRecipients(new ArrayList<>()); h.setTasks(new ArrayList<>()); byId.put(h.getShiftHandoverId(), h); }
        String in = placeholders(byId.size());

        final String receiverSql = "SELECT r.ShiftHandoverId,r.ShiftHandoverRecipientId,r.RecipientUserId,r.RecipientShiftAssignmentId,r.RecipientType,r.AcknowledgedAt,u.FullName, " +
            "st.Name AS TemplateName,st.StartTime,st.EndTime FROM hr.ShiftHandoverRecipient r JOIN iam.[User] u ON u.UserId=r.RecipientUserId " +
            "LEFT JOIN hr.ShiftAssignment sa ON sa.ShiftAssignmentId=r.RecipientShiftAssignmentId LEFT JOIN hr.ShiftTemplate st ON st.ShiftTemplateId=sa.ShiftTemplateId " +
            "WHERE r.ShiftHandoverId IN (" + in + ") ORDER BY u.FullName";
        try (PreparedStatement ps = conn.prepareStatement(receiverSql)) {
            bindIds(ps, byId.keySet());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {
                ShiftHandover h = byId.get(rs.getInt(1)); if (h == null) continue;
                ShiftHandoverRecipient r = new ShiftHandoverRecipient(); r.setShiftHandoverRecipientId(rs.getInt(2)); r.setRecipientUserId(rs.getInt(3));
                int assignment = rs.getInt(4); if (!rs.wasNull()) r.setRecipientShiftAssignmentId(assignment); r.setRecipientType(rs.getString(5));
                Timestamp acknowledged = rs.getTimestamp(6); if (acknowledged != null) r.setAcknowledgedAt(acknowledged.toLocalDateTime()); r.setRecipientName(rs.getString(7));
                String template = rs.getString(8); java.sql.Time start = rs.getTime(9), end = rs.getTime(10);
                r.setShiftLabel(template == null ? "Quản lý chi nhánh" : template + " " + start + "–" + end);
                h.getRecipients().add(r);
                if (r.getRecipientUserId() == currentUserId) { h.setCurrentUserRecipient(true); h.setCurrentUserAcknowledged(r.isAcknowledged()); }
            }}
        }

        final String taskSql = "SELECT t.ShiftHandoverId,t.ShiftHandoverTaskId,t.Content,t.Status,t.UpdatedBy,t.UpdatedAt,u.FullName AS UpdatedByName " +
            "FROM hr.ShiftHandoverTask t LEFT JOIN iam.[User] u ON u.UserId=t.UpdatedBy WHERE t.ShiftHandoverId IN (" + in + ") ORDER BY t.ShiftHandoverTaskId";
        try (PreparedStatement ps = conn.prepareStatement(taskSql)) {
            bindIds(ps, byId.keySet());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) {
                ShiftHandover h = byId.get(rs.getInt(1)); if (h == null) continue;
                ShiftHandoverTask t = new ShiftHandoverTask(); t.setShiftHandoverTaskId(rs.getInt(2)); t.setContent(rs.getString(3)); t.setStatus(rs.getString(4));
                int updater = rs.getInt(5); if (!rs.wasNull()) t.setUpdatedBy(updater);
                Timestamp updated = rs.getTimestamp(6); if (updated != null) t.setUpdatedAt(updated.toLocalDateTime()); t.setUpdatedByName(rs.getString(7));
                h.getTasks().add(t);
            }}
        }
        for (ShiftHandover h : handovers) h.applyViewer(currentUserId);
    }

    private static String placeholders(int size) {
        StringBuilder sb = new StringBuilder(size * 2);
        for (int i = 0; i < size; i++) { if (i > 0) sb.append(','); sb.append('?'); }
        return sb.toString();
    }

    private static void bindIds(PreparedStatement ps, Collection<Integer> ids) throws SQLException {
        int index = 1;
        for (Integer id : ids) ps.setInt(index++, id);
    }

    private ShiftHandover mapHandover(ResultSet rs) throws SQLException {
        ShiftHandover h = new ShiftHandover(); h.setShiftHandoverId(rs.getInt("ShiftHandoverId")); h.setBranchId(rs.getInt("BranchId")); h.setNote(rs.getString("Note")); h.setCreatedBy(rs.getInt("CreatedBy"));
        int source = rs.getInt("SourceShiftAssignmentId"); if (!rs.wasNull()) h.setSourceShiftAssignmentId(source); h.setOverallStatus(rs.getString("OverallStatus")); Timestamp created = rs.getTimestamp("CreatedAt"); if (created != null) h.setCreatedAt(created.toLocalDateTime()); h.setCreatedByName(rs.getString("CreatedByName"));
        String template = rs.getString("SourceTemplateName"); java.sql.Time start = rs.getTime("SourceStartTime"), end = rs.getTime("SourceEndTime"); if (template != null) h.setSourceShiftLabel(template + " " + start + "–" + end);
        return h;
    }
}
