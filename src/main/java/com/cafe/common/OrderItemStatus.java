package com.cafe.common;

/**
 * Trạng thái dòng món — DÙNG CHUNG cho KDS (Barista), Cashier và QR tracking (khách).
 * Khớp ràng buộc CK_Item_Status ở sales.OrderItem.
 * Không hard-code các chuỗi này rải rác trong code/JSP.
 */
public enum OrderItemStatus {
    WAITING,   // chờ làm
    MAKING,    // đang pha
    READY,     // sẵn lấy
    SERVED,    // đã phục vụ
    CANCELLED; // đã huỷ

    public static OrderItemStatus of(String s) {
        return s == null ? null : OrderItemStatus.valueOf(s.trim().toUpperCase());
    }

    /** Nhãn tiếng Việt cho hiển thị. */
    public String label() {
        switch (this) {
            case WAITING:   return "Chờ làm";
            case MAKING:    return "Đang pha";
            case READY:     return "Sẵn lấy";
            case SERVED:    return "Đã phục vụ";
            case CANCELLED: return "Đã huỷ";
            default:        return name();
        }
    }
}
