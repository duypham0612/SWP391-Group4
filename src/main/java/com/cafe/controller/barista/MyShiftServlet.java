package com.cafe.controller.barista;

import com.cafe.common.BusinessDay;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.MonthlyAttendanceRow;
import com.cafe.model.User;
import com.cafe.service.barista.HandoverService;
import com.cafe.service.manager.AttendanceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/** B7 · MyShiftServlet -> /barista/shift. Chấm công vào/tan ca + bảng công tháng của chính barista. */
@WebServlet("/barista/shift")
public class MyShiftServlet extends HttpServlet {

    /** Màn khác trỏ nút chấm công về đây (vd thanh tan ca ở màn bàn giao). */
    static final String PATH = "/barista/shift";
    private final AttendanceService attendanceService = new AttendanceService();
    private final HandoverService handoverService = new HandoverService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        YearMonth ym = parseMonth(req.getParameter("month"));
        String query = textParam(req, "q", 100);
        String state = stateParam(req);
        int pageSize = pageSizeParam(req);
        try {
            BaristaShift.expose(req, PATH);
            if (u != null) {
                // Tổng hợp tháng đọc cả tháng; bảng lịch sử chỉ lấy đúng trang đang xem từ DB.
                List<MonthlyAttendanceRow> monthRows =
                        attendanceService.getMyMonthlyHistory(u.getUserId(), branchId, ym);
                req.setAttribute("hasMonthRows", !monthRows.isEmpty());
                req.setAttribute("monthSummary",
                        attendanceService.getMyMonthlySummary(u.getUserId(), branchId, ym, monthRows));
                req.setAttribute("historyPage", attendanceService.getMyMonthlyHistoryPage(
                        u.getUserId(), branchId, ym, query, state, positiveIntParam(req, "page", 1), pageSize));
                if (BaristaShift.onShift(req)) {
                    req.setAttribute("handoverRequired",
                            handoverService.requiresHandoverBeforeClockOut(branchId, u.getUserId()));
                }
            }
            req.setAttribute("month", ym.toString());
            req.setAttribute("prevMonth", ym.minusMonths(1));
            req.setAttribute("nextMonth", ym.plusMonths(1));
            req.setAttribute("historyQuery", query);
            req.setAttribute("historyState", state);
            req.setAttribute("pageTitle", "Ca làm của tôi");
            req.getRequestDispatcher("/WEB-INF/views/barista/shift/list.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        String action = req.getParameter("action");
        if (!BaristaWritePolicy.isShiftAction(action)) {
            req.getSession().setAttribute("flashError", BaristaWritePolicy.invalidActionMessage());
            resp.sendRedirect(req.getContextPath() + PATH);
            return;
        }
        String redirect = BaristaShift.handleClock(req, action, PATH);
        resp.sendRedirect(req.getContextPath() + (redirect == null ? PATH : redirect));
    }

    /** Tháng từ URL; rác hoặc rỗng -> tháng hiện tại. Không để 500 vì người dùng sửa URL. */
    private static YearMonth parseMonth(String s) {
        try {
            return s == null || s.isBlank() ? YearMonth.now(BusinessDay.VN_ZONE) : YearMonth.parse(s);
        } catch (DateTimeParseException e) {
            return YearMonth.now(BusinessDay.VN_ZONE);
        }
    }

    private static String textParam(HttpServletRequest req, String name, int maxLength) {
        String value = req.getParameter(name);
        if (value == null || value.isBlank()) return "";
        value = value.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * Bộ lọc trạng thái chỉ nhận đúng các mục có trên giao diện; giá trị lạ coi như "Tất cả".
     * ABSENT/OPEN là trạng thái suy ra từ mốc chấm công, không phải cột Attendance.Status.
     */
    private static String stateParam(HttpServletRequest req) {
        String value = textParam(req, "state", 20).toUpperCase();
        for (String allowed : new String[]{"APPROVED", "PENDING", "REJECTED", "OPEN", "ABSENT"}) {
            if (allowed.equals(value)) return value;
        }
        return "";
    }

    private static int positiveIntParam(HttpServletRequest req, String name, int fallback) {
        try {
            int value = Integer.parseInt(req.getParameter(name));
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int pageSizeParam(HttpServletRequest req) {
        return normalizePageSize(positiveIntParam(req, "pageSize", 10));
    }

    /** Chỉ nhận đúng các mức có trên giao diện; giá trị lạ (kể cả rất lớn) rơi về mặc định. */
    static int normalizePageSize(int value) {
        return value == 5 || value == 20 || value == 50 ? value : 10;
    }
}
