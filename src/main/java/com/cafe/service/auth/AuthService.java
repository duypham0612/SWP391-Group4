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

    public String hashPassword(String raw) {
        return PasswordHasher.hashPassword(raw);
    }

    public boolean verifyPassword(String raw, String hash) {
        return PasswordHasher.verifyPassword(raw, hash);
    }
}
