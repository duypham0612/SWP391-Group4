package com.cafe.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Cấu hình ứng dụng không thuộc riêng DB, đọc một lần từ app.properties. */
public final class AppConfig {
    private static final Properties VALUES = load();

    private AppConfig() { }

    /**
     * Thứ tự ưu tiên: JVM property → environment → app.properties.
     * Ví dụ key app.publicBaseUrl tương ứng env CAFE_PUBLIC_BASE_URL.
     */
    public static String get(String key, String environmentName) {
        String value = clean(System.getProperty(key));
        if (value == null) value = clean(System.getenv(environmentName));
        if (value == null) value = clean(VALUES.getProperty(key));
        return value;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) properties.load(in);
        } catch (IOException ignored) {
            // Cấu hình này có fallback từ request; không làm ứng dụng ngừng khởi động.
        }
        return properties;
    }

    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
