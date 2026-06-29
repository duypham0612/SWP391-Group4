package com.cafe.service.auth;

import com.cafe.common.PasswordHasher;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.UserDao;
import com.cafe.model.User;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Nghiệp vụ xác thực (đặc tả mục 3). Mở connection ở Service; trả về User
 * (đã xoá passwordHash) khi đăng nhập thành công, null khi sai.
 */
public class AuthService {

    private final UserDao userDao = new UserDao();

    public User authenticate(String username, String rawPwd) throws SQLException {
        if (username == null || rawPwd == null) return null;
        try (Connection conn = DBConnection.getConnection()) {
            User u = userDao.findByUsername(conn, username.trim());
            if (u == null) return null;
            if (!"ACTIVE".equals(u.getStatus())) return null;
            if (!PasswordHasher.verifyPassword(rawPwd, u.getPasswordHash())) return null;
            u.setPasswordHash(null); // không giữ hash trong session
            return u;
        }
    }

    /**
     * A1 · Quên mật khẩu (tự phục vụ, không cần email-link): xác minh username + email
     * khớp đúng 1 tài khoản ACTIVE rồi đặt lại mật khẩu mới (băm BCrypt trong cùng tx).
     * Trả về true nếu đặt lại thành công; false nếu không khớp / tài khoản khoá.
     */
    public boolean resetPasswordSelfService(String username, String email, String newRawPwd) throws SQLException {
        if (username == null || email == null || newRawPwd == null) return false;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User u = userDao.findByUsername(conn, username.trim());
                boolean match = u != null
                        && "ACTIVE".equals(u.getStatus())
                        && u.getEmail() != null
                        && u.getEmail().trim().equalsIgnoreCase(email.trim());
                if (!match) { conn.rollback(); return false; }
                userDao.updatePassword(conn, u.getUserId(), PasswordHasher.hashPassword(newRawPwd));
                conn.commit();
                return true;
            } catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public String hashPassword(String raw) {
        return PasswordHasher.hashPassword(raw);
    }

    public boolean verifyPassword(String raw, String hash) {
        return PasswordHasher.verifyPassword(raw, hash);
    }
}
