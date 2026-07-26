package com.cafe.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/** Khởi tạo SQL Server disposable từ đúng database.sql, không đụng DB local. */
public abstract class SqlServerIntegrationSupport {
    private static final String IMAGE = "mcr.microsoft.com/mssql/server:2022-latest";
    private static final String EXTERNAL_URL = System.getProperty("it.db.url");
    private static final String EXTERNAL_USERNAME = System.getProperty("it.db.username");
    private static final String EXTERNAL_PASSWORD = System.getProperty("it.db.password");
    protected static final MSSQLServerContainer<?> SQL = new MSSQLServerContainer<>(IMAGE).acceptLicense();
    private static final Pattern GO = Pattern.compile("(?im)^\\s*GO\\s*(?:--.*)?$");

    @BeforeAll
    static void startDatabase() throws Exception {
        if (!usesExternalDatabase()) {
            Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                    "Docker không khả dụng và chưa cấu hình it.db.url.");
            SQL.start();
            try (Connection conn = DriverManager.getConnection(SQL.getJdbcUrl(), SQL.getUsername(), SQL.getPassword())) {
                runScript(conn, Files.readString(Path.of("sql", "database.sql")));
            }
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

    private static String cafeJdbcUrl() {
        String url = usesExternalDatabase() ? EXTERNAL_URL : SQL.getJdbcUrl();
        return url.replaceFirst("(?i)databaseName=[^;]+", "databaseName=CafeChain");
    }

    private static boolean usesExternalDatabase() {
        return EXTERNAL_URL != null && !EXTERNAL_URL.isBlank();
    }

    private static String databaseUsername() {
        return usesExternalDatabase() ? EXTERNAL_USERNAME : SQL.getUsername();
    }

    private static String databasePassword() {
        return usesExternalDatabase() ? EXTERNAL_PASSWORD : SQL.getPassword();
    }

    private static void runScript(Connection conn, String script) throws SQLException, IOException {
        for (String batch : GO.split(script)) {
            String trimmed = batch.trim();
            if (trimmed.isEmpty()) continue;
            try (Statement statement = conn.createStatement()) {
                statement.execute(trimmed);
            }
        }
    }
}
