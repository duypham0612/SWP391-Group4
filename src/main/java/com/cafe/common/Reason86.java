package com.cafe.common;

import java.util.List;

/**
 * Lý do báo tạm hết món (Barista). Mã lưu vào catalog.MenuBlockRequest.Reason.
 *
 * <p>Mỗi lý do kèm bộ ghi chú bấm nhanh — barista đứng máy giữa ca không gõ tay được,
 * chip chỉ là lối tắt điền sẵn, ô ghi chú vẫn sửa tay được. Chip KHÔNG lưu riêng vào DB:
 * thống kê group theo lý do là đủ, ghi chú lưu text thuần.
 *
 * <p>Danh sách chip cố định trong code (không làm bảng config + màn admin — thừa cho phạm vi này).
 */
public enum Reason86 {

    INGREDIENT_OUT("Hết nguyên liệu",
            List.of("Hết sữa tươi", "Hết si-rô", "Hết topping", "Hết ly/nắp", "Hết đá")),

    SPOILED("Hỏng / quá hạn",
            List.of("Sữa chua/hỏng", "Trái cây hỏng", "Quá hạn dùng", "Bảo quản sai nhiệt độ")),

    EQUIPMENT("Máy móc hỏng",
            List.of("Máy pha lỗi", "Máy xay lỗi", "Máy đá lỗi", "Tủ mát lỗi", "Mất điện")),

    QUALITY("Lỗi chất lượng",
            List.of("Vị không đạt", "Pha bị lỗi mẻ", "Sai công thức")),

    /** Không có chip — buộc barista ghi rõ bằng tay. */
    OTHER("Khác", List.of());

    private final String label;
    private final List<String> quickNotes;

    Reason86(String label, List<String> quickNotes) {
        this.label = label;
        this.quickNotes = quickNotes;
    }

    /** Chữ hiện trên màn hình (không để mã enum lọt ra JSP). */
    public String label() { return label; }
    public String getLabel() { return label; }

    /** Chip bấm nhanh của lý do này (rỗng với OTHER). */
    public List<String> quickNotes() { return quickNotes; }
    public List<String> getQuickNotes() { return quickNotes; }

    public String getCode() { return name(); }

    public String getQuickNotesJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < quickNotes.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(jsonEscape(quickNotes.get(i))).append('"');
        }
        return sb.append(']').toString();
    }

    private static String jsonEscape(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Đổi mã từ form sang enum — chấp nhận thừa khoảng trắng và khác hoa/thường.
     * Trả {@code null} khi mã rỗng hoặc không thuộc danh sách (không tin client).
     */
    public static Reason86 fromCode(String code) {
        if (code == null) return null;
        String norm = code.trim().toUpperCase(java.util.Locale.ROOT);
        if (norm.isEmpty()) return null;
        for (Reason86 r : values()) if (r.name().equals(norm)) return r;
        return null;
    }
}
