package com.cafe.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

/** Fail-fast contract giữa WAR và database do Flyway quản lý bên ngoài ứng dụng. */
public final class SchemaVersionGuard {
    private static final String RESOURCE = "db/expected-schema.properties";
    private static final String EXPECTED = loadExpectedVersion();

    private SchemaVersionGuard() { }

    public static String expectedVersion() { return EXPECTED; }

    public static Status check() {
        try (Connection connection = DBConnection.getConnection()) {
            return check(connection);
        } catch (Exception e) {
            return new Status(false, EXPECTED, null,
                    "Không đọc được Flyway history: " + safeMessage(e));
        }
    }

    /** Kiểm trên connection sẵn có; hữu ích cho health check mà không mở pool connection thứ hai. */
    public static Status check(Connection connection) {
        final String sql = "SELECT TOP (1) Version FROM ops.flyway_schema_history "
                + "WHERE Success=1 AND Version IS NOT NULL ORDER BY Installed_Rank DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            String actual = result.next() ? result.getString(1) : null;
            if (!EXPECTED.equals(actual)) {
                return new Status(false, EXPECTED, actual,
                        "Database version " + (actual == null ? "<missing>" : actual)
                                + ", WAR yêu cầu version " + EXPECTED + ".");
            }
            return new Status(true, EXPECTED, actual, "Schema version hợp lệ.");
        } catch (Exception e) {
            return new Status(false, EXPECTED, null,
                    "Thiếu hoặc không đọc được ops.flyway_schema_history: " + safeMessage(e));
        }
    }

    public static void requireCurrent() {
        Status status = check();
        if (!status.up()) throw new IllegalStateException(status.message());
    }

    private static String loadExpectedVersion() {
        try (InputStream input = SchemaVersionGuard.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing " + RESOURCE);
            Properties properties = new Properties();
            properties.load(input);
            String version = properties.getProperty("flyway.expectedVersion");
            if (version == null || version.isBlank()) {
                throw new IllegalStateException("Missing flyway.expectedVersion in " + RESOURCE);
            }
            return version.trim();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    public record Status(boolean up, String expectedVersion, String actualVersion, String message) { }
}
