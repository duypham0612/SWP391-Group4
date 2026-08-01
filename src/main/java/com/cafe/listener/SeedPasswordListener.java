package com.cafe.listener;

import com.cafe.service.auth.SeedAccountAuditService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.Objects;

/**
 * Lifecycle hook: cảnh báo tài khoản seed còn khóa và đóng pool khi undeploy.
 * Không tự sinh mật khẩu dùng chung; mật khẩu bootstrap phải được cấp qua quy trình Admin.
 */
public class SeedPasswordListener implements ServletContextListener {

    private final SeedAccountAuditService auditService;

    public SeedPasswordListener() {
        this(new SeedAccountAuditService());
    }

    SeedPasswordListener(SeedAccountAuditService auditService) {
        this.auditService = Objects.requireNonNull(auditService, "auditService");
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            int pending = auditService.countUsersWithoutRealHash();
            if (pending > 0) {
                log(sce, "[Security] Có " + pending
                        + " tài khoản seed chưa có BCrypt hash hợp lệ; các tài khoản này vẫn bị khóa đăng nhập.");
            }
        } catch (Throwable t) {
            log(sce, "[Security] Không kiểm tra được tài khoản seed: " + t.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            auditService.closeDatabasePool();
        } finally {
            log(sce, "[DB] Đã đóng HikariCP/JDBC khi ứng dụng dừng/redeploy.");
        }
    }

    private void log(ServletContextEvent sce, String msg) {
        sce.getServletContext().log(msg);
    }
}
