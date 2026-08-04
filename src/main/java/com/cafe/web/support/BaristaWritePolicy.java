package com.cafe.web.support;

import java.util.Set;

/**
 * Single allowlist for every POST in the Barista area.
 *
 * <p>The controller is still responsible for choosing the right response (a normal page
 * redirect or a KDS AJAX fragment); this class only decides whether an action is allowed to
 * proceed into the service. This stops a hand-crafted or mistyped POST from silently turning
 * into a no-op request.
 */
public final class BaristaWritePolicy {
    private static final Set<String> CLOCK = Set.of("clockIn", "clockOut");
    private static final Set<String> KDS = Set.of(
            "start", "startOrder", "markReady", "markOrderReady", "reclaim", "returnQueue",
            "reportIssue", "unblock", "remake");
    private static final Set<String> PREP = Set.of(
            "createBatch", "writeOffExpired");
    private static final Set<String> WASTE = Set.of(
            "createIngredientWaste", "update", "void");
    private static final Set<String> EIGHTY_SIX = Set.of("report86", "askReopen");

    private BaristaWritePolicy() { }

    public static boolean isClockAction(String action) { return contains(CLOCK, action); }
    public static boolean isKdsAction(String action) { return contains(KDS, action); }
    public static boolean isPrepAction(String action) { return contains(PREP, action); }
    public static boolean isWasteAction(String action) { return contains(WASTE, action); }
    public static boolean isEightySixAction(String action) { return contains(EIGHTY_SIX, action); }

    /**
     * Clock actions are only accepted on the "My Shift" screen — no other operational screen.
     *
     * <p>Clocking in is a context-dependent step: a barista should see their assigned shift
     * before taking the counter. Previously every screen accepted clockIn/clockOut, so the
     * action collapsed into a single tap while standing at the machine, skipping all that
     * context. This restriction is a real server-side gate, not just a hidden button in the UI.
     */
    public static boolean isShiftAction(String action) { return isClockAction(action); }

    public static String invalidActionMessage() {
        return "Thao tác không hợp lệ hoặc đã hết phiên. Vui lòng tải lại màn hình rồi thử lại.";
    }

    private static boolean contains(Set<String> allowed, String action) {
        return action != null && allowed.contains(action);
    }
}
