package com.cafe.controller.admin;

import com.cafe.common.Constants;
import com.cafe.common.SessionUtil;
import com.cafe.model.ReportRow;
import com.cafe.model.User;
import com.cafe.service.admin.ReportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Landing sau đăng nhập: điều hướng tới dashboard theo role. */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User u = SessionUtil.currentUser(req);
        if (u == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }
        // Manager có dashboard riêng (M1) cần truy vấn số liệu → điều hướng tới servlet chuyên trách.
        if (Constants.ROLE_MANAGER.equals(u.getRoleCode())) {
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }
        String view;
        switch (u.getRoleCode() == null ? "" : u.getRoleCode()) {
            case Constants.ROLE_ADMIN:   view = "/WEB-INF/views/admin/dashboard.jsp";   break;
            case Constants.ROLE_MANAGER: view = "/WEB-INF/views/manager/dashboard.jsp"; break;
            case Constants.ROLE_CASHIER: view = "/WEB-INF/views/cashier/dashboard.jsp"; break;
            case Constants.ROLE_BARISTA: view = "/WEB-INF/views/barista/dashboard.jsp"; break;
            default:
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Role không hợp lệ");
                return;
        }
        if (Constants.ROLE_ADMIN.equals(u.getRoleCode())) {
            try {
                LocalDate today = LocalDate.now();
                LocalDate to = parseDate(req.getParameter("to"), today);
                LocalDate from = parseDate(req.getParameter("from"), to.minusDays(29));
                if (from.isAfter(to)) { LocalDate t = from; from = to; to = t; }   // chống đảo ngược
                req.setAttribute("summary",      reportService.getChainSummary(from, to));
                req.setAttribute("byBranch",     reportService.getRevenueByBranch(from, to));
                req.setAttribute("byMethod",     reportService.getPaymentBreakdown(from, to));
                req.setAttribute("topProducts",  reportService.getTopProducts(10, from, to));
                List<ReportRow> daily = reportService.getDailyRevenue(from, to);
                BigDecimal maxDaily = BigDecimal.ZERO;
                for (ReportRow r : daily) if (r.getAmount().compareTo(maxDaily) > 0) maxDaily = r.getAmount();
                req.setAttribute("daily", daily);
                req.setAttribute("maxDaily", maxDaily);
                req.setAttribute("fromDate", from.toString());
                req.setAttribute("toDate", to.toString());
            } catch (Exception e) { throw new ServletException(e); }
        }
        req.setAttribute("pageTitle", "Bảng điều khiển");
        req.getRequestDispatcher(view).forward(req, resp);
    }

    /** Parse yyyy-MM-dd; rỗng/sai → mặc định. */
    private LocalDate parseDate(String s, LocalDate fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return fallback; }
    }
}
