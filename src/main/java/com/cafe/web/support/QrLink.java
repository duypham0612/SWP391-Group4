package com.cafe.web.support;

import com.cafe.common.AppConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Dựng URL tuyệt đối nhúng vào mã QR dán tại bàn. Tách khỏi servlet để test thuần. */
public final class QrLink {

    private QrLink() { }

    /**
     * Base công khai suy ra từ request hiện tại, đã tính X-Forwarded-*.
     * Mọi màn hình sinh mã QR đều đi qua đây để local và bản deploy không lệch nhau.
     */
    public static String absoluteBase(jakarta.servlet.http.HttpServletRequest req) {
        String configured = AppConfig.get("app.publicBaseUrl", "CAFE_PUBLIC_BASE_URL");
        if (configured != null) {
            String normalized = normalizeConfiguredBase(configured);
            if (normalized != null) return normalized;
        }
        return absoluteBase(
                req.getScheme(), req.getServerName(), req.getServerPort(), req.getContextPath(),
                req.getHeader("X-Forwarded-Proto"),
                req.getHeader("X-Forwarded-Host"),
                req.getHeader("X-Forwarded-Port"));
    }

    static String normalizeConfiguredBase(String configured) {
        if (configured == null) return null;
        String value = configured.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value.matches("(?i)^https?://[^\\s/]+(?::\\d+)?(?:/[^\\s]*)?$") ? value : null;
    }

    /**
     * Bản dùng khi app chạy SAU reverse proxy (nginx / Cloudflare / PaaS). Proxy nhận HTTPS ở 443
     * rồi gọi Tomcat bằng HTTP ở cổng nội bộ, nên {@code request.getScheme()/getServerPort()} trả về
     * thông tin nội bộ — in thẳng ra QR sẽ thành "http://10.0.0.4:8080/..." và khách quét không vào
     * được. Ưu tiên X-Forwarded-* khi proxy có gửi; không có thì giữ nguyên giá trị của request.
     *
     * @param forwardedProto giá trị header {@code X-Forwarded-Proto} (có thể null)
     * @param forwardedHost  giá trị header {@code X-Forwarded-Host} (có thể kèm ":port", có thể null)
     * @param forwardedPort  giá trị header {@code X-Forwarded-Port} (có thể null)
     */
    public static String absoluteBase(String scheme, String serverName, int port, String contextPath,
                                      String forwardedProto, String forwardedHost, String forwardedPort) {
        String proto = firstHop(forwardedProto);
        if (proto == null) proto = scheme;

        String host = firstHop(forwardedHost);
        int resolvedPort;
        if (host == null) {
            host = serverName;
            resolvedPort = proto.equals(scheme) ? port : defaultPort(proto);
        } else {
            // X-Forwarded-Host thường là "shop.vn" hoặc "shop.vn:8443"; IPv6 dạng "[::1]:8443".
            int colon = host.lastIndexOf(':');
            int bracket = host.lastIndexOf(']');
            if (colon > bracket && colon < host.length() - 1) {
                resolvedPort = parsePort(host.substring(colon + 1), defaultPort(proto));
                host = host.substring(0, colon);
            } else {
                resolvedPort = defaultPort(proto);
            }
        }
        String explicitPort = firstHop(forwardedPort);
        if (explicitPort != null) resolvedPort = parsePort(explicitPort, resolvedPort);

        return absoluteBase(proto, host, resolvedPort, contextPath);
    }

    /** Header X-Forwarded-* qua nhiều proxy là danh sách "a, b, c" — hop ĐẦU mới là của khách. */
    private static String firstHop(String headerValue) {
        if (headerValue == null) return null;
        String first = headerValue.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }

    private static int defaultPort(String scheme) {
        return "https".equals(scheme) ? 443 : 80;
    }

    private static int parsePort(String raw, int fallback) {
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 && parsed <= 65535 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** scheme://host[:port]+contextPath — bỏ port khi là cổng chuẩn của scheme. */
    public static String absoluteBase(String scheme, String serverName, int port, String contextPath) {
        boolean standard = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        return scheme + "://" + serverName + (standard ? "" : ":" + port)
                + (contextPath == null ? "" : contextPath);
    }

    /** URL khách quét ra: {@code <base>/qr/menu?t=<qrCode>}. */
    public static String menuUrl(String base, String qrCode) {
        String normalizedBase = base != null && base.endsWith("/")
                ? base.substring(0, base.length() - 1)
                : base;
        return normalizedBase + "/qr/menu?t="
                + URLEncoder.encode(qrCode, StandardCharsets.UTF_8);
    }
}
