package com.cafe.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Nguồn kết nối DB duy nhất của ứng dụng — HikariCP pool.
 * Mọi nơi lấy connection qua {@link #getConnection()} / {@link #getDataSource()}.
 * KHÔNG hard-code chuỗi kết nối ở chỗ khác.
 */
public final class DBConnection {

    private static final HikariDataSource DS;

    static {
        try {
            Properties p = loadFallbackProperties();

            HikariConfig cfg = new HikariConfig();
            // System property chỉ dành cho môi trường deploy/CI (đặc biệt Testcontainers).
            // File db.properties vẫn là fallback cho developer chạy Tomcat cục bộ.
            cfg.setJdbcUrl(value("db.url", p));
            cfg.setUsername(value("db.username", p));
            cfg.setPassword(value("db.password", p));
            cfg.setDriverClassName(value("db.driver", p));
            cfg.setMaximumPoolSize(parseInt(value("db.pool.maxSize", p), 20));
            cfg.setMinimumIdle(parseInt(value("db.pool.minIdle", p), 5));
            cfg.setConnectionTimeout(parseLong(value("db.pool.connectionTimeout", p), 30000L));
            cfg.setPoolName("CafeChainPool");
            DS = new HikariDataSource(cfg);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("DBConnection init failed: " + e.getMessage());
        }
    }

    private DBConnection() { }

    public static DataSource getDataSource() {
        return DS;
    }

    public static Connection getConnection() throws SQLException {
        return DS.getConnection();
    }

    private static int parseInt(String v, int def) {
        try { return v == null ? def : Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static long parseLong(String v, long def) {
        try { return v == null ? def : Long.parseLong(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static String value(String key, Properties properties) {
        String override = System.getProperty(key);
        return override == null || override.isBlank() ? properties.getProperty(key) : override;
    }

    /**
     * Khi mọi thông số thiết yếu đã được cấp từ system properties (CI/Testcontainers), không mở
     * db.properties cục bộ. Điều này giúp test disposable không phụ thuộc cấu hình hay credential
     * của developer; file chỉ là fallback cho Tomcat chạy local.
     */
    private static Properties loadFallbackProperties() throws Exception {
        String[] required = {"db.url", "db.username", "db.password", "db.driver"};
        boolean allProvided = true;
        for (String key : required) {
            String value = System.getProperty(key);
            if (value == null || value.isBlank()) {
                allProvided = false;
                break;
            }
        }
        if (allProvided) return new Properties();

        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) throw new IllegalStateException("Không tìm thấy db.properties trên classpath");
            Properties p = new Properties();
            p.load(is);
            return p;
        }
    }
}
