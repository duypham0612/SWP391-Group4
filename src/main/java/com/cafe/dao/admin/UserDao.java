package com.cafe.dao.admin;

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
 * Truy vấn iam.[User]. DAO nhận Connection từ Service — không tự mở/đóng,
 * không chứa nghiệp vụ.
 */
public class UserDao {

    private static final String BASE_SELECT =
        "SELECT u.UserId, u.Username, u.PasswordHash, u.FullName, u.Email, u.Phone, " +
        "       u.RoleId, u.BranchId, u.Status, " +
        "       r.Code AS RoleCode, r.Name AS RoleName, b.Name AS BranchName " +
        "FROM iam.[User] u " +
        "JOIN iam.Role r       ON u.RoleId = r.RoleId " +
        "LEFT JOIN org.Branch b ON u.BranchId = b.BranchId ";

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
        final String sql = "SELECT UserId, PasswordHash FROM iam.[User] " +
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
        final String sql = "UPDATE iam.[User] SET PasswordHash = ? WHERE UserId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection conn, int userId, String status) throws SQLException {
        final String sql = "UPDATE iam.[User] SET Status = ? WHERE UserId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public List<User> findAll(Connection conn) throws SQLException {
        final String sql = BASE_SELECT + "ORDER BY r.RoleId, u.Username";
        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    public List<User> findByBranch(Connection conn, int branchId) throws SQLException {
        final String sql = BASE_SELECT + "WHERE u.BranchId = ? ORDER BY r.RoleId, u.Username";
        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    /** A2 · lọc theo vai trò và/hoặc chi nhánh (null = bỏ qua tiêu chí đó). */
    public List<User> findFiltered(Connection conn, Integer roleId, Integer branchId) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT + "WHERE 1=1");
        if (roleId != null) sql.append(" AND u.RoleId = ?");
        if (branchId != null) sql.append(" AND u.BranchId = ?");
        sql.append(" ORDER BY r.RoleId, u.Username");
        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            if (roleId != null) ps.setInt(i++, roleId);
            if (branchId != null) ps.setInt(i++, branchId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
        }
        return out;
    }

    /** A2.F6 · danh sách user theo mã vai trò (vd BRANCH_MANAGER cho dropdown gán quản lý). */
    public List<User> findByRoleCode(Connection conn, String roleCode) throws SQLException {
        final String sql = BASE_SELECT + "WHERE r.Code = ? AND u.Status = 'ACTIVE' ORDER BY u.FullName";
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

    public boolean usernameExists(Connection conn, String username, int excludeId) throws SQLException {
        final String sql = "SELECT 1 FROM iam.[User] WHERE Username = ? AND UserId <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int insert(Connection conn, User u, String passwordHash) throws SQLException {
        final String sql = "INSERT INTO iam.[User](Username, PasswordHash, FullName, Email, Phone, RoleId, BranchId, Status) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, passwordHash);
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getEmail());
            ps.setString(5, u.getPhone());
            ps.setInt(6, u.getRoleId());
            if (u.getBranchId() == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, u.getBranchId());
            ps.setString(8, u.getStatus() == null ? "ACTIVE" : u.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    /** Cập nhật hồ sơ (không đổi mật khẩu). */
    public void update(Connection conn, User u) throws SQLException {
        final String sql = "UPDATE iam.[User] SET FullName=?, Email=?, Phone=?, RoleId=?, BranchId=?, Status=? WHERE UserId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPhone());
            ps.setInt(4, u.getRoleId());
            if (u.getBranchId() == null) ps.setNull(5, Types.INTEGER); else ps.setInt(5, u.getBranchId());
            ps.setString(6, u.getStatus());
            ps.setInt(7, u.getUserId());
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("UserId"));
        u.setUsername(rs.getString("Username"));
        u.setPasswordHash(rs.getString("PasswordHash"));
        u.setFullName(rs.getString("FullName"));
        u.setEmail(rs.getString("Email"));
        u.setPhone(rs.getString("Phone"));
        u.setRoleId(rs.getInt("RoleId"));
        int branchId = rs.getInt("BranchId");
        u.setBranchId(rs.wasNull() ? null : branchId);
        u.setStatus(rs.getString("Status"));
        u.setRoleCode(rs.getString("RoleCode"));
        u.setRoleName(rs.getString("RoleName"));
        u.setBranchName(rs.getString("BranchName"));
        return u;
    }
}
