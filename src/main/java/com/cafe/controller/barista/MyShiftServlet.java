package com.cafe.controller.barista;

import com.cafe.common.BusinessDay;
import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
import com.cafe.web.support.BaristaShiftSupport;
import com.cafe.web.support.BaristaWritePolicy;
import com.cafe.model.MonthlyAttendanceRow;
import com.cafe.model.User;
import com.cafe.service.manager.AttendanceService;
import com.cafe.web.support.BranchContext;
import com.cafe.web.support.RequestParams;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/** B7 · MyShiftServlet -> /barista/shift. Chấm công vào/tan ca + bảng công tháng của chính barista. */
@WebServlet("/barista/shift")
public class MyShiftServlet extends HttpServlet {

    static final String PATH = "/barista/shift";
    private final AttendanceService attendanceService;
    private final BaristaShiftSupport shiftSupport;

    public MyShiftServlet() { this(new AttendanceService(), new BaristaShiftSupport()); }
    MyShiftServlet(AttendanceService attendanceService, BaristaShiftSupport shiftSupport) {
        this.attendanceService = Objects.requireNonNull(attendanceService);
        this.shiftSupport = Objects.requireNonNull(shiftSupport);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = BranchContext.requireBranchId(req);
        User u = SessionUtil.currentUser(req);
        YearMonth ym = parseMonth(req.getParameter("month"));
        String query = RequestParams.text(req, "q", 100);
        String state = stateParam(req);
        int pageSize = pageSizeParam(req);
        try {
            shiftSupport.expose(req, PATH);
            if (u != null) {
                // Tổng hợp tháng đọc cả tháng; bảng lịch sử chỉ lấy đúng trang đang xem từ DB.
                List<MonthlyAttendanceRow> monthRows =
                        attendanceService.getMyMonthlyHistory(u.getUserId(), branchId, ym);
                req.setAttribute("hasMonthRows", !monthRows.isEmpty());
                req.setAttribute("monthSummary",
                        attendanceService.getMyMonthlySummary(u.getUserId(), branchId, ym, monthRows));
                req.setAttribute("historyPage", attendanceService.getMyMonthlyHistoryPage(
                        u.getUserId(), branchId, ym, query, state, RequestParams.positiveInt(req, "page", 1), pageSize));
            }
            req.setAttribute("month", ym.toString());
            req.setAttribute("prevMonth", ym.minusMonths(1));
            req.setAttribute("nextMonth", ym.plusMonths(1));
            req.setAttribute("historyQuery", query);
            req.setAttribute("historyState", state);
            req.setAttribute("pageTitle", "Ca làm của tôi");
            req.getRequestDispatcher("/WEB-INF/views/barista/shift.jsp").forward(req, resp);
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
        String redirect = shiftSupport.handleClock(req, action, PATH);
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

    /**
     * Bộ lọc trạng thái chỉ nhận đúng các mục có trên giao diện; giá trị lạ coi như "Tất cả".
     * ABSENT/OPEN là trạng thái suy ra từ mốc chấm công, không chỉ từ AttendanceStatus.
     */
    private static String stateParam(HttpServletRequest req) {
        return RequestParams.allowed(req, "state", "APPROVED", "PENDING", "REJECTED", "OPEN", "ABSENT");
    }

    private static int pageSizeParam(HttpServletRequest req) {
        return normalizePageSize(RequestParams.positiveInt(req, "pageSize", 10));
    }

    /** Chỉ nhận đúng các mức có trên giao diện; giá trị lạ (kể cả rất lớn) rơi về mặc định. */
    static int normalizePageSize(int value) {
        return value == 5 || value == 20 || value == 50 ? value : 10;
    }
}
