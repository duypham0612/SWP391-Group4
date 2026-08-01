package com.cafe.integration;

import com.cafe.service.auth.AuthService;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Fresh database localhost dùng DDL cuối trực tiếp; migrate lần hai phải là no-op. */
public class DatabaseMigrationIT extends SqlServerIntegrationSupport {

    @Test
    void fresh_database_is_at_single_expected_version_and_second_migrate_is_noop()
            throws Exception {
        assertEquals("1", scalar(connection(),
                "SELECT TOP (1) Version FROM ops.flyway_schema_history WHERE Success=1 "
                        + "ORDER BY Installed_Rank DESC"));
        assertEquals(0, migrate(cafeJdbcUrl(), databaseUsername(), databasePassword())
                .migrationsExecuted);
        assertEquals(1, scalarInt(connection(),
                "SELECT COUNT(*) FROM ops.flyway_schema_history "
                        + "WHERE Success=1 AND Version IS NOT NULL"));
        assertEquals(4, scalarInt(connection(),
                "SELECT COUNT(*) FROM iam.UserAccount WHERE Username IN "
                        + "('admin','manager1','cashier1','barista1')"));
        assertEquals(3, scalarInt(connection(),
                "SELECT COUNT(*) FROM catalog.Product"));
        assertEquals(6, scalarInt(connection(),
                "SELECT COUNT(*) FROM catalog.IngredientUnitConversion "
                        + "WHERE IsBaseUnit=1 AND IsActive=1 AND FactorToBase=1"));
        AuthService auth = new AuthService();
        for (String username : new String[]{"admin", "manager1", "cashier1", "barista1"}) {
            assertNotNull(auth.authenticate(username, "123456"),
                    "Tài khoản demo phải đăng nhập được: " + username);
        }
    }

    private String scalar(Connection connection, String sql) throws Exception {
        try (connection; PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getString(1) : null;
        }
    }

    private int scalarInt(Connection connection, String sql) throws Exception {
        try (connection; PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getInt(1) : 0;
        }
    }
}
