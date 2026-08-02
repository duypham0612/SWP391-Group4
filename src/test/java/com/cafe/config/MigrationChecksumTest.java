package com.cafe.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Khóa checksum và bảo đảm Flyway chỉ có một nguồn SQL tạo schema. */
class MigrationChecksumTest {
    private static final Path DATABASE_SQL =
            Path.of("src", "main", "resources", "db", "migration", "V1__database.sql");

    @Test
    void flyway_has_exactly_one_database_migration_sql_file() throws Exception {
        Path migrationDirectory = DATABASE_SQL.getParent();
        try (Stream<Path> files = Files.list(migrationDirectory)) {
            List<Path> migrations = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
            assertEquals(List.of(DATABASE_SQL), migrations);
        }
    }

    @Test
    void database_sql_matches_sha256_manifest() throws Exception {
        Path manifest = Path.of("sql", "migration-checksums.sha256");
        assertTrue(Files.isRegularFile(manifest));
        int entries = 0;
        for (String line : Files.readAllLines(manifest)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            entries++;
            String[] parts = line.trim().split("\\s+", 2);
            assertEquals(2, parts.length, "Manifest line không hợp lệ: " + line);
            Path migration = Path.of(parts[1].trim());
            assertTrue(Files.isRegularFile(migration), "Thiếu migration: " + migration);
            String sql = Files.readString(migration, StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace('\r', '\n');
            String actual = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(sql.getBytes(StandardCharsets.UTF_8)));
            assertEquals(parts[0].toLowerCase(), actual,
                    "Schema SQL đã thay đổi nhưng manifest checksum chưa được cập nhật: " + migration);
        }
        assertEquals(1, entries, "Manifest phải chỉ chứa đúng một database SQL");
    }
}
