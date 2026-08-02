package com.cafe.common;

import java.util.List;
import java.util.Locale;

/**
 * Lý do barista báo sự cố cho một dòng món ở Quầy pha chế.
 *
 * <p>Ba nhóm lý do dẫn tới BA hành động khác nhau, nên nhóm phải nằm trên chính lý do chứ không
 * rải rác ở Controller:
 * <ul>
 *   <li>{@link #OUT_OF_STOCK} — sửa sổ kho (kiểm kê nguyên liệu về 0) rồi chặn món;</li>
 *   <li>{@link #isBlocking()} — chặn món, đưa sang mục "Cần xử lý", rời hàng chờ;</li>
 *   <li>còn lại — chỉ gắn cờ cho Thu ngân/Quản lý, món KHÔNG bị huỷ.</li>
 * </ul>
 *
 * <p>Nhãn tiếng Việt ở đây được GHI THẲNG vào sổ hao hụt/nhật ký món, nên sửa chữ là đổi dữ liệu
 * lịch sử — không đổi tuỳ tiện.
 *
 * <p>Thứ tự khai báo = thứ tự hiện trên dropdown của {@code views/barista/kds.jsp}.
 */
public enum IssueReason {

    OUT_OF_STOCK("Hết nguyên liệu", false),
    EQUIPMENT("Máy móc gặp sự cố", true),
    NOTE_UNSUPPORTED("Không đáp ứng được ghi chú", false),
    DISCONTINUED("Món đã ngừng bán", true),
    UNCLEAR_ORDER("Thông tin đơn không rõ", false),

    /** Barista tự gõ diễn giải; nhãn dưới đây chỉ dùng cho dropdown, KHÔNG ghi vào sổ. */
    OTHER("Lý do khác", false);

    private final String label;
    private final boolean blocking;

    IssueReason(String label, boolean blocking) {
        this.label = label;
        this.blocking = blocking;
    }

    /** True nếu món KHÔNG pha được → chặn món thay vì chỉ gắn cờ. */
    public boolean isBlocking() { return blocking; }

    /** Chữ hiện trên màn hình (không để mã enum lọt ra JSP). */
    public String label() { return label; }
    public String getLabel() { return label; }

    public String getCode() { return name(); }

    /** Mã các lý do gây chặn món, dạng "EQUIPMENT,DISCONTINUED" — để JSP đẩy xuống cho JS. */
    public static String blockingCodesCsv() {
        StringBuilder sb = new StringBuilder();
        for (IssueReason r : values()) {
            if (!r.blocking) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(r.name());
        }
        return sb.toString();
    }

    public static List<IssueReason> selectableValues() {
        return List.of(values());
    }

    /**
     * Đổi mã từ form sang enum — chấp nhận thừa khoảng trắng và khác hoa/thường.
     * Trả {@code null} khi mã rỗng hoặc không thuộc danh sách (không tin client).
     */
    public static IssueReason fromCode(String code) {
        if (code == null) return null;
        String norm = code.trim().toUpperCase(Locale.ROOT);
        if (norm.isEmpty()) return null;
        for (IssueReason r : values()) if (r.name().equals(norm)) return r;
        return null;
    }
}
