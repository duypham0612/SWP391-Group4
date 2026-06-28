package com.cafe.listener;

import com.cafe.common.PasswordHasher;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.UserDao;
import com.cafe.model.User;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.util.List;

/**
 * Khi app khởi động: thay mọi PasswordHash dạng placeholder ($2a$placeholder) trong seed
 * bằng BCrypt hash thật của mật khẩu mặc định (idempotent — chỉ đụng các dòng chưa phải $2...).
 * Mục đích: tài khoản seed đăng nhập được ngay mà không cần precompute hash.
 * Không làm app sập nếu DB chưa sẵn sàng (chỉ log).
 */
@WebListener
public class SeedPasswordListener implements ServletContextListener {

    /** Mật khẩu mặc định cho mọi tài khoản seed. ĐỔI khi lên production. */
    public static final String DEFAULT_SEED_PASSWORD = "123456";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        UserDao dao = new UserDao();
        try (Connection conn = DBConnection.getConnection()) {
            List<User> pending = dao.findUsersWithoutRealHash(conn);
            if (pending.isEmpty()) {
                log(sce, "[SeedPassword] Tất cả user đã có BCrypt hash, bỏ qua.");
                return;
            }
            String hash = PasswordHasher.hashPassword(DEFAULT_SEED_PASSWORD);
            for (User u : pending) {
                dao.updatePassword(conn, u.getUserId(), hash);
            }
            log(sce, "[SeedPassword] Đã set mật khẩu mặc định ('" + DEFAULT_SEED_PASSWORD
                    + "') cho " + pending.size() + " tài khoản seed.");
        } catch (Exception e) {
            log(sce, "[SeedPassword] Bỏ qua (DB chưa sẵn sàng?): " + e.getMessage());
        }
    }

    private void log(ServletContextEvent sce, String msg) {
        sce.getServletContext().log(msg);
    }
}
