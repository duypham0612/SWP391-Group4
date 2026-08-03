package com.cafe.dao.shared;

import com.cafe.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy vấn iam.UserAccount. DAO nhận Connection từ Service — không tự mở/đóng,
 * không chứa nghiệp vụ.
 */
public class UserDao {

    private static final String BASE_SELECT =
        "SELECT u.UserId, u.Username, u.PasswordHash, u.FullName, u.Email, u.Phone, " +
        "       u.RoleCode, u.BranchId, u.HourlyRate, u.Status, " +
        "       " + roleNameCase("u.RoleCode") + " AS RoleName, b.Name AS BranchName, " +
        "       b.IsActive AS BranchActive, b.ManagerUserId AS BranchManagerUserId " +
        "FROM iam.UserAccount u " +
        "LEFT JOIN org.Branch b ON u.BranchId = b.BranchId ";

    private static final String ROLE_ORDER =
            "CASE u.RoleCode WHEN 'ADMIN' THEN 1 WHEN 'BRANCH_MANAGER' THEN 2 " +
            "WHEN 'CASHIER' THEN 3 WHEN 'BARISTA' THEN 4 ELSE 5 END";

    public User findByUsername(Connection conn, String username) throws SQLException {
        final String sql = BASE_SELECT + "WHERE u.Username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<User> findUsersWithoutRealHash(Connection conn) throws SQLException {
        // BCrypt hash thật dài đúng 60 ký tự; placeholder '$2a$placeholder' (15 ký tự) sẽ lọt vào đây.
        final String sql = "SELECT UserId, PasswordHash FROM iam.UserAccount " +
                "WHERE PasswordHash IS NULL OR LEN(PasswordHash) < 60";
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("UserId"));
                u.setPasswordHash(rs.getString("PasswordHash"));
                list.add(u);
            }
        }
        return list;
    }

    public void updatePassword(Connection conn, int userId, String hash) throws SQLException {
        final String sql = "UPDATE iam.UserAccount SET PasswordHash = ? WHERE UserId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection conn, int userId, String status) throws SQLException {
        final String sql = "UPDATE iam.UserAccount SET Status = ? WHERE UserId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateRole(Connection conn, int userId, String roleCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE iam.UserAccount SET RoleCode=? WHERE UserId=?")) {
            ps.setString(1, roleCode);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public List<User> findAll(Connection conn) throws SQLException {
        final String sql = BASE_SELECT + "ORDER BY " + ROLE_ORDER + ", u.Username";
        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<User> findByBranch(Connection conn, int branchId) throws SQLException {
        final String sql = BASE_SELECT + "WHERE u.BranchId = ? ORDER BY " + ROLE_ORDER + ", u.Username";
        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /** Nhân sự ACTIVE cùng chi nhánh có thể được chọn làm quản lý mới. */
    public List<User> findManagerReplacementCandidates(Connection conn, int branchId,
                                                        int currentManagerId) throws SQLException {
        final String sql = BASE_SELECT
                + "WHERE u.BranchId=? AND u.Status='ACTIVE' AND u.RoleCode<>'ADMIN' "
                + "AND u.UserId<>? "
                + "AND NOT EXISTS(SELECT 1 FROM payment.CashierShift cs "
                + "WHERE cs.CashierId=u.UserId AND cs.ClosedAt IS NULL) "
                + "AND NOT EXISTS(SELECT 1 FROM hr.ShiftAssignment sa "
                + "WHERE sa.UserId=u.UserId AND sa.CheckInAt IS NOT NULL AND sa.CheckOutAt IS NULL) "
                + "ORDER BY " + ROLE_ORDER + ", u.FullName";
        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, currentManagerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /** A2 · lọc theo vai trò/chi nhánh/từ khoá và phân trang (null = bỏ qua). */
    public List<User> findFiltered(Connection conn, String roleCode, Integer branchId,
                                   String q, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT + "WHERE 1=1");
        appendFilterWhere(sql, roleCode, branchId, q);
        sql.append(" ORDER BY ").append(ROLE_ORDER)
           .append(", u.Username OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = bindFilters(ps, roleCode, branchId, q);
            ps.setInt(i++, Math.max(0, offset));
            ps.setInt(i, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public int countFiltered(Connection conn, String roleCode, Integer branchId, String q) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM iam.UserAccount u " +
            "LEFT JOIN org.Branch b ON u.BranchId = b.BranchId " +
            "WHERE 1=1");
        appendFilterWhere(sql, roleCode, branchId, q);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindFilters(ps, roleCode, branchId, q);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    /** A2.F6 · danh sách user theo mã vai trò (vd BRANCH_MANAGER cho dropdown gán quản lý). */
    public List<User> findByRoleCode(Connection conn, String roleCode) throws SQLException {
        final String sql = BASE_SELECT + "WHERE u.RoleCode = ? AND u.Status = 'ACTIVE' ORDER BY u.FullName";
        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleCode);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    public User findById(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(BASE_SELECT + "WHERE u.UserId = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Khóa user tới hết transaction khi cập nhật role/branch/trạng thái. */
    public User findByIdForUpdate(Connection conn, int id) throws SQLException {
        String lockedSelect = BASE_SELECT.replace(
                "FROM iam.UserAccount u ",
                "FROM iam.UserAccount u WITH (UPDLOCK, HOLDLOCK) ");
        try (PreparedStatement ps = conn.prepareStatement(lockedSelect + "WHERE u.UserId = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean isActiveInBranch(Connection conn, int userId, int branchId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM iam.UserAccount WHERE UserId=? AND BranchId=? AND Status='ACTIVE'")) {
            ps.setInt(1, userId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean usernameExists(Connection conn, String username, int excludeId) throws SQLException {
        final String sql = "SELECT 1 FROM iam.UserAccount WHERE Username = ? AND UserId <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean emailExists(Connection conn, String email, int excludeId) throws SQLException {
        final String sql = "SELECT 1 FROM iam.UserAccount WHERE LOWER(Email) = LOWER(?) AND UserId <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean phoneExists(Connection conn, String phone, int excludeId) throws SQLException {
        final String sql = "SELECT 1 FROM iam.UserAccount WHERE Phone = ? AND UserId <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int insert(Connection conn, User u, String passwordHash) throws SQLException {
        final String sql = "INSERT INTO iam.UserAccount(Username, PasswordHash, FullName, Email, Phone, RoleCode, BranchId, HourlyRate, Status) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, passwordHash);
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getEmail());
            ps.setString(5, u.getPhone());
            ps.setString(6, u.getRoleCode());
            if (u.getBranchId() == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, u.getBranchId());
            if (u.getHourlyRate() == null) ps.setNull(8, Types.DECIMAL);
            else ps.setBigDecimal(8, u.getHourlyRate());
            ps.setString(9, u.getStatus() == null ? "ACTIVE" : u.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /** Cập nhật nhân sự; BranchId bất biến sau khi tạo. */
    public void update(Connection conn, User u) throws SQLException {
        final String sql = "UPDATE iam.UserAccount SET FullName=?, Email=?, Phone=?, RoleCode=?, HourlyRate=?, Status=? WHERE UserId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getRoleCode());
            if (u.getHourlyRate() == null) ps.setNull(5, Types.DECIMAL);
            else ps.setBigDecimal(5, u.getHourlyRate());
            ps.setString(6, u.getStatus());
            ps.setInt(7, u.getUserId());
            ps.executeUpdate();
        }
    }

    /** Trả lý do chặn đổi quyền khi nhân sự còn nghiệp vụ đang mở. */
    public String findManagerReplacementBlock(Connection conn, int userId) throws SQLException {
        final String sql = "SELECT CASE "
                + "WHEN EXISTS(SELECT 1 FROM payment.CashierShift WITH (UPDLOCK,HOLDLOCK) "
                + "WHERE CashierId=? AND ClosedAt IS NULL) THEN 'CASHIER_SHIFT' "
                + "WHEN EXISTS(SELECT 1 FROM hr.ShiftAssignment WITH (UPDLOCK,HOLDLOCK) "
                + "WHERE UserId=? AND CheckInAt IS NOT NULL AND CheckOutAt IS NULL) "
                + "THEN 'ATTENDANCE' ELSE NULL END";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Cập nhật thông tin cá nhân; không đổi role/branch/status/password. */
    public void updateProfile(Connection conn, int userId, String fullName, String email, String phone) throws SQLException {
        final String sql = "UPDATE iam.UserAccount SET FullName=?, Email=?, Phone=? WHERE UserId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    private void appendFilterWhere(StringBuilder sql, String roleCode, Integer branchId, String q) {
        if (roleCode != null) sql.append(" AND u.RoleCode = ?");
        if (branchId != null) sql.append(" AND u.BranchId = ?");
        if (q != null && !q.isBlank()) {
            sql.append(" AND (u.FullName LIKE ? OR u.Username LIKE ? OR u.Email LIKE ? OR u.Phone LIKE ?)");
        }
    }

    private int bindFilters(PreparedStatement ps, String roleCode, Integer branchId, String q) throws SQLException {
        int i = 1;
        if (roleCode != null) ps.setString(i++, roleCode);
        if (branchId != null) ps.setInt(i++, branchId);
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim() + "%";
            ps.setString(i++, like);
            ps.setString(i++, like);
            ps.setString(i++, like);
            ps.setString(i++, like);
        }
        return i;
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("UserId"));
        u.setUsername(rs.getString("Username"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setFullName(rs.getString("FullName"));
        u.setEmail(rs.getString("Email"));
        u.setPhone(rs.getString("Phone"));
        u.setHourlyRate(rs.getBigDecimal("HourlyRate"));
        int branchId = rs.getInt("BranchId");
        u.setBranchId(rs.wasNull() ? null : branchId);
        u.setStatus(rs.getString("Status"));
        u.setRoleCode(rs.getString("RoleCode"));
        u.setRoleName(rs.getString("RoleName"));
        u.setBranchName(rs.getString("BranchName"));
        Object branchActive = rs.getObject("BranchActive");
        u.setBranchActive(branchActive == null ? null : rs.getBoolean("BranchActive"));
        Object managerUserId = rs.getObject("BranchManagerUserId");
        u.setBranchHasManager(u.getBranchId() == null ? null : managerUserId != null);
        u.setAssignedBranchManager(u.getBranchId() == null
                ? null : managerUserId != null && rs.getInt("BranchManagerUserId") == u.getUserId());
        return u;
    }

    private static String roleNameCase(String column) {
        return "CASE " + column +
                " WHEN 'ADMIN' THEN N'Quản trị hệ thống'" +
                " WHEN 'BRANCH_MANAGER' THEN N'Quản lý chi nhánh'" +
                " WHEN 'CASHIER' THEN N'Thu ngân'" +
                " WHEN 'BARISTA' THEN N'Pha chế' ELSE " + column + " END";
    }
}
