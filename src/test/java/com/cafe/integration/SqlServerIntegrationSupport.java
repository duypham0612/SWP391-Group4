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

    private static void createCafeDatabaseIfMissing(String jdbcUrl, String username,
                                                    String password) throws SQLException {
        String masterUrl = jdbcUrl.matches("(?i).*databaseName=[^;]+.*")
                ? jdbcUrl.replaceFirst("(?i)databaseName=[^;]+", "databaseName=master")
                : jdbcUrl + (jdbcUrl.endsWith(";") ? "" : ";") + "databaseName=master";
        try (Connection connection = DriverManager.getConnection(masterUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("IF DB_ID(N'CafeChain') IS NULL CREATE DATABASE CafeChain");
        }
    }

    protected static String cafeJdbcUrl() {
        if (usesExternalDatabase()) {
            if (!INIT_EXTERNAL_SCHEMA) return EXTERNAL_URL;
            if (EXTERNAL_URL.matches("(?i).*databaseName=[^;]+.*")) {
                return EXTERNAL_URL.replaceFirst(
                        "(?i)databaseName=[^;]+", "databaseName=CafeChain");
            }
            return EXTERNAL_URL + (EXTERNAL_URL.endsWith(";") ? "" : ";")
                    + "databaseName=CafeChain";
        }
        return SQL.getJdbcUrl().replaceFirst(
                "(?i)databaseName=[^;]+", "databaseName=CafeChain");
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
