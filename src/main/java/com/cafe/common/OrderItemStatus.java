package com.cafe.common;

/**
 * Trạng thái dòng món — DÙNG CHUNG cho KDS (Barista), Cashier và QR tracking (khách).
 * Khớp ràng buộc CK_Item_Status ở sales.OrderItem.
 * Không hard-code các chuỗi này rải rác trong code/JSP.
 */
public enum OrderItemStatus {
    WAITING,       // chờ barista nhận
    MAKING,   // barista đã nhận, đang pha
    READY,         // pha xong, chờ nhân viên nhận
    PICKED_UP,     // nhân viên phục vụ/thu ngân đã nhận
    SERVED,        // đã giao khách
    BLOCKED,       // bị chặn do sự cố
    CANCELLED,     // đã huỷ
    REMAKE;        // trạng thái chuyển tiếp khi tạo lượt làm lại

    public static OrderItemStatus of(String s) {
        return s == null ? null : OrderItemStatus.valueOf(s.trim().toUpperCase());
    }

    /** Nhãn tiếng Việt cho hiển thị. */
    public String label() {
        switch (this) {
            case WAITING:     return "Chờ pha";
            case MAKING: return "Đang pha";
            case READY:       return "Đã pha xong";
            case PICKED_UP:   return "Đã được nhận";
            case SERVED:      return "Đã giao khách";
            case BLOCKED:     return "Bị chặn";
            case CANCELLED:   return "Đã huỷ";
            case REMAKE:      return "Làm lại";
            default:        return name();
        }
    }
}
