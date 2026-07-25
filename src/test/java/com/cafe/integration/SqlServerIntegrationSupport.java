package com.cafe.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

/** Khởi tạo SQL Server disposable từ đúng database.sql, không đụng DB local. */
@Testcontainers(disabledWithoutDocker = true)
public abstract class SqlServerIntegrationSupport {
    private static final String IMAGE = "mcr.microsoft.com/mssql/server:2022-latest";
    protected static final MSSQLServerContainer<?> SQL = new MSSQLServerContainer<>(IMAGE).acceptLicense();
    private static final Pattern GO = Pattern.compile("(?im)^\\s*GO\\s*(?:--.*)?$");

    @BeforeAll
    static void startDatabase() throws Exception {
        SQL.start();
        try (Connection conn = DriverManager.getConnection(SQL.getJdbcUrl(), SQL.getUsername(), SQL.getPassword())) {
            runScript(conn, Files.readString(Path.of("sql", "database.sql")));
        }
        System.setProperty("db.url", cafeJdbcUrl());
        System.setProperty("db.username", SQL.getUsername());
        System.setProperty("db.password", SQL.getPassword());
        System.setProperty("db.driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        System.setProperty("db.pool.maxSize", "6");
        System.setProperty("db.pool.minIdle", "0");
    }

    @AfterAll
    static void stopDatabase() {
        for (String key : new String[]{"db.url", "db.username", "db.password", "db.driver", "db.pool.maxSize", "db.pool.minIdle"}) {
            System.clearProperty(key);
        }
        SQL.stop();
    }

    protected static Connection connection() throws SQLException {
        return DriverManager.getConnection(cafeJdbcUrl(), SQL.getUsername(), SQL.getPassword());
    }

    private static String cafeJdbcUrl() {
        return SQL.getJdbcUrl().replaceFirst("(?i)databaseName=[^;]+", "databaseName=CafeChain");
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
