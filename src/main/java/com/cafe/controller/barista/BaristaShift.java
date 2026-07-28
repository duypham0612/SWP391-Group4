package com.cafe.controller.barista;

import com.cafe.common.SessionUtil;
import com.cafe.common.BusinessDay;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.ShiftClockStatus;
import com.cafe.model.User;
import com.cafe.service.barista.HandoverService;
import com.cafe.service.manager.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Trực ca cho barista dùng chung mọi màn (KDS · Prep · Pickup · Waste).
 * Nguồn sự thật là chấm công (ShiftAssignment + Attendance): "trong ca" = đã vào ca, chưa tan ca.
 * Ngoài ca → màn chỉ xem, mọi POST ghi bị chặn ở server; JSP khoá thao tác + hiện nút Vào ca.
 */
public final class BaristaShift {

    /** Màn bàn giao ca — tan ca phải đi qua đây khi ca chưa được bàn giao. */
    public static final String HANDOVER_PATH = "/barista/handover";

    /** Lời báo khi thao tác ghi bị chặn vì ngoài ca — dùng chung cho cả nhánh redirect lẫn AJAX. */
    public static final String OFF_SHIFT_MESSAGE = "Bạn đang ngoài ca — cần vào ca trước khi thao tác.";

    /** Màn hàng chờ, đã lọc sẵn về món của chính mình — nơi barista gỡ nốt món dở trước khi tan ca. */
    public static final String MY_QUEUE_PATH = "/barista/kds?owner=mine";

    private static final AttendanceService attendance = new AttendanceService();
    private static final HandoverService handover = new HandoverService();
    private static final com.cafe.service.shared.OrderService orders = new com.cafe.service.shared.OrderService();

    private BaristaShift() {}

    /**
     * Nạp trạng thái chấm công cho JSP: clockStatus, onShift, clockPostUrl, handoverUrl.
     *
     * <p>clockPostUrl chỉ có tác dụng ở màn "Ca làm của tôi" — nơi duy nhất còn form chấm công.
     * Các màn vận hành chỉ dùng clockStatus/onShift để dựng banner trỏ sang màn đó.
     */
    public static void expose(HttpServletRequest req, String selfPath) throws SQLException {
        expose(req, selfPath, true);
    }

    /**
     * Nạp trạng thái trực ca dùng chung. Fragment AJAX của KDS không render cảnh báo bàn giao nên
     * có thể bỏ truy vấn đếm; các trang đầy đủ luôn dùng overload mặc định ở trên.
     */
    public static void expose(HttpServletRequest req, String selfPath, boolean includeHandoverCount)
            throws SQLException {
        ShiftClockStatus status = status(req);
        req.setAttribute("clockStatus", status);
        req.setAttribute("onShift", status != null && status.isCanClockOut());
        req.setAttribute("clockPostUrl", req.getContextPath() + selfPath);
        req.setAttribute("handoverUrl", req.getContextPath() + HANDOVER_PATH);
        User user = SessionUtil.currentUser(req);
        if (includeHandoverCount && user != null) {
            req.setAttribute("pendingHandoverCount", handover.countUnacknowledgedForUser(
                    InventoryDashboardServlet.branchId(req), user.getUserId()));
        }
    }

    /** True nếu barista đang trong ca (đã vào, chưa tan). Lỗi → coi như ngoài ca (fail-closed, an toàn). */
    public static boolean onShift(HttpServletRequest req) {
        try {
            ShiftClockStatus status = status(req);
            return status != null && status.isCanClockOut();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Chốt chặn ghi cho doPost của các màn vận hành: ngoài ca → flashError rồi redirect về chính màn đó.
     *
     * <p>KHÔNG xử lý chấm công. Chấm công chỉ diễn ra ở màn "Ca làm của tôi" (xem
     * {@link BaristaWritePolicy#isShiftAction}), nơi barista thấy đủ ca được xếp và bàn giao
     * đang chờ trước khi nhận quầy.
     *
     * @return true nếu request đã được xử lý xong — caller phải return ngay.
     */
    public static boolean guardWrite(HttpServletRequest req, HttpServletResponse resp, String selfPath)
            throws IOException {
        if (!onShift(req)) {
            req.getSession().setAttribute("flashError", OFF_SHIFT_MESSAGE);
            resp.sendRedirect(req.getContextPath() + selfPath);
            return true;
        }
        return false;
    }

    /**
     * Nhánh chặn ghi cho màn trả fragment AJAX (Quầy pha chế). Khác {@link #guardWrite} ở chỗ
     * KHÔNG tự redirect: caller phải tự dựng lại phần thân đúng định dạng client đang chờ.
     * Redirect ở đây làm fetch đi theo 302, nhận về NGUYÊN trang rồi nhồi cả header/sidebar
     * vào khung bảng — hỏng giao diện đúng lúc barista đang cần thao tác.
     *
     * @return true nếu ngoài ca (đã ghi flashError, caller chỉ việc render lại), false nếu được phép.
     */
    public static boolean blockedOffShift(HttpServletRequest req) {
        if (onShift(req)) return false;
        req.getSession().setAttribute("flashError", OFF_SHIFT_MESSAGE);
        return true;
    }

    /**
     * Xử lý vào ca / tan ca. Trả về path cần redirect nếu action là clockIn/clockOut (đã consume),
     * null nếu action khác. Tan ca khi ca chưa bàn giao → không chấm công, đẩy sang màn bàn giao.
     */
    public static String handleClock(HttpServletRequest req, String action, String selfPath) {
        if (!"clockIn".equals(action) && !"clockOut".equals(action)) return null;
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("clockIn".equals(action)) {
                attendance.clockIn(userId, branchId);
                req.getSession().setAttribute("flashOk", "Đã vào ca.");
            } else if (pendingBrewBlock(req, branchId, userId)) {
                return MY_QUEUE_PATH;
            } else if (handover.requiresHandoverBeforeClockOut(branchId, userId)) {
                req.getSession().setAttribute("flashError",
                        "Bạn cần bàn giao ca trước khi tan ca. Nhập việc cần bàn giao rồi bấm “Lưu bàn giao & Tan ca”.");
                return HANDOVER_PATH;
            } else {
                attendance.clockOut(userId, branchId);
                req.getSession().setAttribute("flashOk", "Đã tan ca.");
            }
        } catch (IllegalStateException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (SQLException e) {
            req.getSession().setAttribute("flashError", "Không chấm công được: " + e.getMessage());
        }
        return selfPath;
    }

    /**
     * Cổng tan ca: còn ly đang pha dưới tên mình thì chưa được tan ca.
     *
     * <p>Món MAKING mang BaristaId của người nhận, mà cả "Xong" lẫn "Trả lại chờ" đều guard theo
     * BaristaId — đi về mà không gỡ thì ly đó bị khoá, ca sau nhìn thấy nhưng không đụng được và
     * khách ngồi đợi mãi. Chặn ở đây là chính đáng vì lối thoát nằm trọn trong tay barista:
     * bấm Xong, hoặc Trả lại chờ (không đụng kho, chạy được cả với món chưa có công thức).
     *
     * @return true nếu đã chặn (đã ghi flashError, caller phải đưa họ về hàng chờ).
     */
    private static boolean pendingBrewBlock(HttpServletRequest req, int branchId, int userId)
            throws SQLException {
        int pending = orders.countMyMakingItems(branchId, userId);
        if (pending <= 0) return false;
        req.getSession().setAttribute("flashError", "Bạn còn " + pending
                + " ly đang pha — bấm “Xong” hoặc “Trả lại chờ” cho từng ly rồi mới tan ca được.");
        return true;
    }

    private static ShiftClockStatus status(HttpServletRequest req) throws SQLException {
        User u = SessionUtil.currentUser(req);
        if (u == null) return null;
        int branchId = InventoryDashboardServlet.branchId(req);
        return attendance.getMyShiftStatus(u.getUserId(), branchId, BusinessDay.todayVn());
    }
}
