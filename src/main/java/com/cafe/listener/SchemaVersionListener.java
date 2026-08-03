package com.cafe.listener;

import com.cafe.config.SchemaVersionGuard;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/** Không chạy migration; chỉ từ chối khởi động WAR nếu deployment step chưa migrate đủ. */
public final class SchemaVersionListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        SchemaVersionGuard.Status status = SchemaVersionGuard.check();
        event.getServletContext().setAttribute("schemaVersionStatus", status);
        if (!status.up()) {
            event.getServletContext().log("[DB] " + status.message());
            throw new IllegalStateException(status.message());
        }
        event.getServletContext().log("[DB] Flyway schema version "
                + status.actualVersion() + " đã sẵn sàng.");
    }
}
