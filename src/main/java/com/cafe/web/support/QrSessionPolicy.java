package com.cafe.web.support;

/**
 * Chốt quyền của app khách QR. Khách ẩn danh — KHÔNG đăng nhập — nên thứ duy nhất chứng minh
 * "đây là bàn của tôi" là phiên bàn đã gắn vào HTTP session lúc quét QR ở /qr/menu.
 * Tin theo tham số URL thì đổi một con số là xem/huỷ được đơn của bàn khác.
 */
public final class QrSessionPolicy {

    private QrSessionPolicy() { }

    /**
     * Phiên khách được phép thao tác, hoặc null nếu phải từ chối.
     *
     * @param boundSessionId giá trị "qrSessionId" trong HTTP session (null nếu chưa quét QR)
     * @param requested      id phiên trong URL/form; bỏ trống thì dùng luôn phiên đã gắn
     */
    public static Integer resolve(Object boundSessionId, String requested) {
        if (!(boundSessionId instanceof Integer)) return null;
        Integer bound = (Integer) boundSessionId;
        if (requested == null || requested.isBlank()) return bound;
        try {
            return Integer.parseInt(requested.trim()) == bound.intValue() ? bound : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
