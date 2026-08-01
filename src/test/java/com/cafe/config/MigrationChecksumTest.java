package com.cafe.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Khóa checksum và bảo đảm dự án chỉ có một nguồn SQL database. */
class MigrationChecksumTest {
    private static final Path DATABASE_SQL =
            Path.of("src", "main", "resources", "db", "migration", "V1__database.sql");

    @Test
    void repository_has_exactly_one_database_sql_file() throws Exception {
        List<Path> roots = List.of(Path.of("src"), Path.of("sql"));
        List<Path> sqlFiles = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".sql"))
                        .forEach(sqlFiles::add);
            }
        }
        assertEquals(List.of(DATABASE_SQL), sqlFiles.stream().sorted().toList());
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
            String actual = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(migration)));
            assertEquals(parts[0].toLowerCase(), actual,
                    "Schema SQL đã thay đổi nhưng manifest checksum chưa được cập nhật: " + migration);
        }
        assertEquals(1, entries, "Manifest phải chỉ chứa đúng một database SQL");
    }
}
