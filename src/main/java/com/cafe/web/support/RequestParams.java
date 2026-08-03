package com.cafe.web.support;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Đọc tham số query/form theo kiểu KHÔNG BAO GIỜ NÉM LỖI.
 *
 * <p>Mọi tham số ở đây đều nằm trên URL nên người dùng sửa tay được: gõ {@code ?page=abc} hay
 * {@code ?pageSize=999999} phải rơi về mặc định chứ không được thành trang lỗi 500. Vì vậy các
 * hàm dưới đây luôn trả giá trị hợp lệ thay vì ném {@link NumberFormatException}.
 *
 * <p>Đặt ở {@code web/support} chứ không phải {@code common}: lớp này phụ thuộc
 * {@code jakarta.servlet}, mà ArchUnit cấm {@code common} chạm tới servlet.
 *
 * <p>Cố ý KHÔNG gom {@code normalizePageSize} vào đây — mỗi màn có bộ mức riêng (quầy pha 5/10/20/50
 * mặc định 5, ca làm mặc định 10, báo hết món 10/20/50) nên ép chung sẽ đổi hành vi.
 */
public final class RequestParams {

    private RequestParams() { }

    /** Chuỗi đã trim và cắt trần độ dài; thiếu/rỗng → chuỗi rỗng. Cắt trần để chặn query dài bất thường. */
    public static String text(HttpServletRequest request, String name, int maxLength) {
        String value = request.getParameter(name);
        if (isBlank(value)) return "";
        value = value.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /** Số nguyên dương; thiếu/không phải số/nhỏ hơn 1 → {@code fallback}. */
    public static int positiveInt(HttpServletRequest request, String name, int fallback) {
        try {
            int value = Integer.parseInt(request.getParameter(name));
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Như {@link #positiveInt} nhưng trả {@code null} khi thiếu/không phải số — để phía gọi tự quyết. */
    public static Integer optionalInt(HttpServletRequest request, String name) {
        String raw = request.getParameter(name);
        if (isBlank(raw)) return null;
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Chỉ nhận đúng các giá trị có trên giao diện (so khớp sau khi viết hoa); giá trị lạ → chuỗi rỗng
     * nghĩa là "tất cả". Dùng cho bộ lọc trạng thái/loại, tránh để giá trị gõ tay lọt xuống câu SQL.
     */
    public static String allowed(HttpServletRequest request, String name, String... allowedValues) {
        String value = text(request, name, 20).toUpperCase();
        for (String item : allowedValues) {
            if (item.equals(value)) return value;
        }
        return "";
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
