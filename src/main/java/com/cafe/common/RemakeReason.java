package com.cafe.common;

import java.util.List;
import java.util.Locale;

/**
 * Lý do làm lại một dòng món ở Quầy pha chế.
 *
 * <p>Khác {@link IssueReason}: mọi lý do ở đây đều dẫn tới CÙNG một hành động — đưa món về hàng chờ
 * với ưu tiên làm lại và ghi hao hụt lượt pha cũ. Lý do chỉ để đối soát sau ca, nên không có cờ
 * phân nhóm.
 *
 * <p>Nhãn tiếng Việt được GHI THẲNG vào sổ hao hụt — sửa chữ là đổi dữ liệu lịch sử.
 *
 * <p>Thứ tự khai báo = thứ tự hiện trên dropdown của {@code views/barista/kds.jsp}.
 */
public enum RemakeReason {

    WRONG_RECIPE("Pha sai công thức"),
    SPILLED("Làm đổ hoặc hư món"),
    QUALITY("Chất lượng không đạt"),
    CUSTOMER_FEEDBACK("Khách phản hồi"),
    WRONG_DELIVERY("Giao nhầm"),
    CHANGED_REQUEST("Khách thay đổi yêu cầu");

    private final String label;

    RemakeReason(String label) {
        this.label = label;
    }

    /** Chữ hiện trên màn hình (không để mã enum lọt ra JSP). */
    public String label() { return label; }
    public String getLabel() { return label; }

    public String getCode() { return name(); }

    public static List<RemakeReason> selectableValues() {
        return List.of(values());
    }

    /**
     * Đổi mã từ form sang enum — chấp nhận thừa khoảng trắng và khác hoa/thường.
     * Trả {@code null} khi mã rỗng hoặc không thuộc danh sách (không tin client).
     */
    public static RemakeReason fromCode(String code) {
        if (code == null) return null;
        String norm = code.trim().toUpperCase(Locale.ROOT);
        if (norm.isEmpty()) return null;
        for (RemakeReason r : values()) if (r.name().equals(norm)) return r;
        return null;
    }
}
