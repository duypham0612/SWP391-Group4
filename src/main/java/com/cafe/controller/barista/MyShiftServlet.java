package com.cafe.controller.barista;

import com.cafe.common.BusinessDay;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.MonthlyAttendanceRow;
import com.cafe.model.User;
import com.cafe.service.manager.AttendanceService;
import com.cafe.service.barista.HandoverService;
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
        try {
            BaristaShift.expose(req, PATH);
            if (u != null) {
                List<MonthlyAttendanceRow> monthRows =
                        attendanceService.getMyMonthlyHistory(u.getUserId(), branchId, ym);
                req.setAttribute("monthRows", monthRows);
                req.setAttribute("monthSummary",
                        attendanceService.getMyMonthlySummary(u.getUserId(), branchId, ym, monthRows));
                if (BaristaShift.onShift(req)) {
                    req.setAttribute("pendingHandoverCount", handoverService.countUnacknowledgedForUser(branchId, u.getUserId()));
                }
            }
            req.setAttribute("month", ym.toString());
            req.setAttribute("prevMonth", ym.minusMonths(1));
            req.setAttribute("nextMonth", ym.plusMonths(1));
            req.setAttribute("pageTitle", "Ca làm của tôi");
            req.getRequestDispatcher("/WEB-INF/views/barista/shift.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        BaristaShift.handleClock(req, req.getParameter("action"));
        resp.sendRedirect(req.getContextPath() + PATH);
    }

    /** Tháng từ URL; rác hoặc rỗng -> tháng hiện tại. Không để 500 vì người dùng sửa URL. */
    private static YearMonth parseMonth(String s) {
        try {
            return s == null || s.isBlank() ? YearMonth.now(BusinessDay.VN_ZONE) : YearMonth.parse(s);
        } catch (DateTimeParseException e) {
            return YearMonth.now(BusinessDay.VN_ZONE);
        }
    }
}
