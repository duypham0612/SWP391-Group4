package com.cafe.controller.barista;

import java.util.Set;

/**
 * Allowlist duy nhất cho mọi POST của khu vực Barista.
 *
 * <p>Controller vẫn chịu trách nhiệm chọn phản hồi phù hợp (redirect trang thường hay fragment
 * AJAX của KDS), còn lớp này chỉ quyết định action có được phép đi tiếp vào service hay không.
 * Điều này chặn POST tự soạn/typo biến thành một request im lặng không làm gì.
 */
public final class BaristaWritePolicy {
    private static final Set<String> CLOCK = Set.of("clockIn", "clockOut");
    private static final Set<String> KDS = Set.of(
            "start", "startOrder", "markReady", "markOrderReady", "reclaim", "returnQueue",
            "reportIssue", "unblock", "remake");
    private static final Set<String> PREP = Set.of(
            "createBatch", "writeOffExpired");
    private static final Set<String> WASTE = Set.of(
            "createIngredientWaste", "create", "update", "void");
    private static final Set<String> EIGHTY_SIX = Set.of("report86", "askReopen");
    private static final Set<String> HANDOVER = Set.of(
            "create", "createAndClockOut", "acknowledge", "claim", "updateTask");

    private BaristaWritePolicy() { }

    public static boolean isClockAction(String action) { return contains(CLOCK, action); }
    public static boolean isKdsAction(String action) { return contains(KDS, action); }
    public static boolean isPrepAction(String action) { return contains(PREP, action); }
    public static boolean isWasteAction(String action) { return contains(WASTE, action); }
    public static boolean isEightySixAction(String action) { return contains(EIGHTY_SIX, action); }
    public static boolean isHandoverAction(String action) { return contains(HANDOVER, action); }

    /**
     * Chấm công chỉ nhận ở màn "Ca làm của tôi" — không màn vận hành nào khác.
     *
     * <p>Vào ca là bước có ngữ cảnh: barista phải thấy ca được xếp và bàn giao ca trước đang
     * chờ xác nhận rồi mới nhận quầy. Trước đây mọi màn đều nhận clockIn/clockOut nên thao
     * tác đó rút gọn thành một cú bấm giữa lúc đứng máy, bỏ qua toàn bộ ngữ cảnh. Giới hạn ở
     * đây là chốt thật phía server, không chỉ là chuyện ẩn nút trên giao diện.
     */
    public static boolean isShiftAction(String action) { return isClockAction(action); }

    public static String invalidActionMessage() {
        return "Thao tác không hợp lệ hoặc đã hết phiên. Vui lòng tải lại màn hình rồi thử lại.";
    }

    private static boolean contains(Set<String> allowed, String action) {
        return action != null && allowed.contains(action);
    }
}
