package com.cafe.controller.barista;

import com.cafe.common.SessionUtil;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.ShiftClockStatus;
import com.cafe.model.User;
import com.cafe.service.manager.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Trực ca cho barista dùng chung mọi màn (KDS · Prep · Pickup · Waste).
 * Nguồn sự thật là chấm công (ShiftAssignment + Attendance): "trong ca" = đã vào ca, chưa tan ca.
 * Ngoài ca → màn chỉ xem, mọi POST ghi bị chặn ở server; JSP khoá thao tác + hiện nút Vào ca.
 */
public final class BaristaShift {

    private static final AttendanceService attendance = new AttendanceService();

    private BaristaShift() {}

    /** Nạp trạng thái chấm công cho JSP: clockStatus, onShift, clockPostUrl (nút Vào ca post về đây). */
    public static void expose(HttpServletRequest req, String selfPath) throws SQLException {
        ShiftClockStatus status = status(req);
        req.setAttribute("clockStatus", status);
        req.setAttribute("onShift", status != null && status.isCanClockOut());
        req.setAttribute("clockPostUrl", req.getContextPath() + selfPath);
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
     * Chốt chặn ghi cho doPost. Trả về true nếu request đã được xử lý xong (caller phải return ngay):
     * - action là clockIn/clockOut → chấm công rồi redirect.
     * - ngoài ca → chặn, flashError, redirect.
     * Trả về false nghĩa là được phép thao tác tiếp.
     */
    public static boolean guardWrite(HttpServletRequest req, HttpServletResponse resp,
                                     String action, String selfPath) throws IOException {
        String self = req.getContextPath() + selfPath;
        if (handleClock(req, action)) {
            resp.sendRedirect(self);
            return true;
        }
        if (!onShift(req)) {
            req.getSession().setAttribute("flashError", "Bạn đang ngoài ca — cần vào ca trước khi thao tác.");
            resp.sendRedirect(self);
            return true;
        }
        return false;
    }

    /** Xử lý vào ca / tan ca. Trả về true nếu action là clockIn/clockOut (đã consume). */
    public static boolean handleClock(HttpServletRequest req, String action) {
        if (!"clockIn".equals(action) && !"clockOut".equals(action)) return false;
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("clockIn".equals(action)) {
                attendance.clockIn(userId, branchId);
                req.getSession().setAttribute("flashOk", "Đã vào ca.");
            } else {
                attendance.clockOut(userId, branchId);
                req.getSession().setAttribute("flashOk", "Đã tan ca.");
            }
        } catch (IllegalStateException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
        } catch (SQLException e) {
            req.getSession().setAttribute("flashError", "Không chấm công được: " + e.getMessage());
        }
        return true;
    }

    private static ShiftClockStatus status(HttpServletRequest req) throws SQLException {
        User u = SessionUtil.currentUser(req);
        if (u == null) return null;
        int branchId = InventoryDashboardServlet.branchId(req);
        return attendance.getMyShiftStatus(u.getUserId(), branchId, LocalDate.now());
    }
}
