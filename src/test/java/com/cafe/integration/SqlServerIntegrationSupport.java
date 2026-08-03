package com.cafe.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Khởi tạo SQL Server disposable bằng Flyway; chỉ dùng DB ngoài khi caller truyền -Dit.db.url. */
public abstract class SqlServerIntegrationSupport {
    private static final String IMAGE = "mcr.microsoft.com/mssql/server:2022-latest";
    private static final String EXTERNAL_URL = System.getProperty("it.db.url");
    private static final String EXTERNAL_USERNAME = System.getProperty("it.db.username");
    private static final String EXTERNAL_PASSWORD = System.getProperty("it.db.password");
    private static final boolean INIT_EXTERNAL_SCHEMA =
            Boolean.parseBoolean(System.getProperty("it.db.initSchema", "false"));
    protected static final MSSQLServerContainer<?> SQL = new MSSQLServerContainer<>(IMAGE).acceptLicense();

    @BeforeAll
    static void startDatabase() throws Exception {
        if (!usesExternalDatabase()) {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                throw new IllegalStateException(
                        "Integration profile yêu cầu Docker hoặc -Dit.db.url; không được bỏ qua với 0 test.");
            }
            SQL.start();
            createCafeDatabaseIfMissing(SQL.getJdbcUrl(), SQL.getUsername(), SQL.getPassword());
            migrate(cafeJdbcUrl(), SQL.getUsername(), SQL.getPassword());
        } else if (INIT_EXTERNAL_SCHEMA) {
            createCafeDatabaseIfMissing(EXTERNAL_URL, EXTERNAL_USERNAME, EXTERNAL_PASSWORD);
            migrate(cafeJdbcUrl(), EXTERNAL_USERNAME, EXTERNAL_PASSWORD);
        } else {
            migrate(cafeJdbcUrl(), EXTERNAL_USERNAME, EXTERNAL_PASSWORD);
        }
        System.setProperty("db.url", cafeJdbcUrl());
        System.setProperty("db.username", databaseUsername());
        System.setProperty("db.password", databasePassword());
        System.setProperty("db.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        System.setProperty("db.pool.maxSize", "6");
        System.setProperty("db.pool.minIdle", "0");
    }

    @AfterAll
    static void stopDatabase() {
        for (String key : new String[]{"db.url", "db.username", "db.password", "db.driver", "db.pool.maxSize", "db.pool.minIdle"}) {
            System.clearProperty(key);
        }
        if (!usesExternalDatabase()) SQL.stop();
    }

    protected static Connection connection() throws SQLException {
        return DriverManager.getConnection(cafeJdbcUrl(), databaseUsername(), databasePassword());
    }

    protected static MigrateResult migrate(String url, String username, String password) {
        return Flyway.configure()
                .dataSource(url, username, password)
                .schemas("ops")
                .defaultSchema("ops")
                .table("flyway_schema_history")
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .outOfOrder(false)
                .validateOnMigrate(true)
                .baselineOnMigrate(false)
                .load()
                .migrate();
    }

    /**
     * Đặt {@code databaseName} vào chuỗi kết nối, thay thế nếu đã có và nối thêm nếu chưa.
     *
     * <p>Nhánh "nối thêm" là bắt buộc, không phải phòng xa: {@code MSSQLServerContainer.getJdbcUrl()}
     * trả về {@code jdbc:sqlserver://localhost:32769;encrypt=false} — KHÔNG có {@code databaseName}.
     * Trước đây nhánh container chỉ gọi {@code replaceFirst}, nên đó là một phép thay thế không
     * khớp gì cả: mọi test âm thầm chạy trong {@code master}, còn CafeChain vừa tạo xong thì bỏ
     * không. Chỉ lộ ra khi {@code DatabaseNormalizationIT} so sánh DB hiện tại với master và thấy
     * hai bên y hệt nhau.
     */
    private static String withDatabaseName(String jdbcUrl, String databaseName) {
        return jdbcUrl.matches("(?i).*databaseName=[^;]+.*")
                ? jdbcUrl.replaceFirst("(?i)databaseName=[^;]+", "databaseName=" + databaseName)
                : jdbcUrl + (jdbcUrl.endsWith(";") ? "" : ";") + "databaseName=" + databaseName;
    }

    private static void createCafeDatabaseIfMissing(String jdbcUrl, String username,
                                                    String password) throws SQLException {
        String masterUrl = withDatabaseName(jdbcUrl, "master");
        try (Connection connection = DriverManager.getConnection(masterUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("IF DB_ID(N'CafeChain') IS NULL CREATE DATABASE CafeChain");
        }
    }

    protected static String cafeJdbcUrl() {
        if (usesExternalDatabase()) {
            if (!INIT_EXTERNAL_SCHEMA) return EXTERNAL_URL;
            return withDatabaseName(EXTERNAL_URL, "CafeChain");
        }
        return withDatabaseName(SQL.getJdbcUrl(), "CafeChain");
    }

    private static boolean usesExternalDatabase() {
        return EXTERNAL_URL != null && !EXTERNAL_URL.isBlank();
    }

    protected static String databaseUsername() {
        return usesExternalDatabase() ? EXTERNAL_USERNAME : SQL.getUsername();
    }

    protected static String databasePassword() {
        return usesExternalDatabase() ? EXTERNAL_PASSWORD : SQL.getPassword();
    }
}
