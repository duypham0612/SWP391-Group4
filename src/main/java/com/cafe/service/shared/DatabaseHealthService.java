package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.config.SchemaVersionGuard;

import java.sql.Connection;

/** Health use case; không để tầng HTTP biết JDBC hoặc Flyway history. */
public class DatabaseHealthService {

    public HealthStatus check() {
        try (Connection connection = DBConnection.getConnection()) {
            SchemaVersionGuard.Status schema = SchemaVersionGuard.check(connection);
            if (!schema.up()) {
                return new HealthStatus(false, schema.actualVersion(), schema.message());
            }
            return new HealthStatus(true, schema.actualVersion(), "DB connected");
        } catch (Exception ignored) {
            return new HealthStatus(false, null, "Không kết nối được database.");
        }
    }

    public record HealthStatus(boolean up, String schemaVersion, String message) { }
}
