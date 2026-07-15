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
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is == null) {
                throw new IllegalStateException("Không tìm thấy db.properties trên classpath");
            }
            Properties p = new Properties();
            p.load(is);

            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(p.getProperty("db.url"));
            cfg.setUsername(p.getProperty("db.username"));
            cfg.setPassword(p.getProperty("db.password"));
            cfg.setDriverClassName(p.getProperty("db.driver"));
            cfg.setMaximumPoolSize(parseInt(p.getProperty("db.pool.maxSize"), 20));
            cfg.setMinimumIdle(parseInt(p.getProperty("db.pool.minIdle"), 5));
            cfg.setConnectionTimeout(parseLong(p.getProperty("db.pool.connectionTimeout"), 30000L));
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
}
