package com.cafe.controller.admin;

import com.cafe.common.Constants;
import com.cafe.web.support.SessionUtil;
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

    private final ReportService reportService;

    public DashboardServlet() { this(new ReportService()); }
    DashboardServlet(ReportService reportService) {
        this.reportService = java.util.Objects.requireNonNull(reportService);
    }

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
        // Cashier có dashboard số liệu riêng (R2: doanh thu + số đơn) → servlet chuyên trách.
        if (Constants.ROLE_CASHIER.equals(u.getRoleCode())) {
            resp.sendRedirect(req.getContextPath() + "/cashier/dashboard");
            return;
        }
        // Barista không có bảng điều khiển riêng → landing thẳng vào Quầy pha chế.
        if (Constants.ROLE_BARISTA.equals(u.getRoleCode())) {
            resp.sendRedirect(req.getContextPath() + "/barista/kds");
            return;
        }
        if (!Constants.ROLE_ADMIN.equals(u.getRoleCode())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Role không hợp lệ");
            return;
        }
        try {
            LocalDate today = com.cafe.common.BusinessDay.todayVn();
            LocalDate to = parseDate(req.getParameter("to"), today);
            LocalDate from = parseDate(req.getParameter("from"), to.minusDays(29));
            if (from.isAfter(to)) { LocalDate t = from; from = to; to = t; }
            req.setAttribute("summary", reportService.getChainSummary(from, to));
            req.setAttribute("byBranch", reportService.getRevenueByBranch(from, to));
            req.setAttribute("byMethod", reportService.getPaymentBreakdown(from, to));
            req.setAttribute("topProducts", reportService.getTopProducts(10, from, to));
            List<ReportRow> daily = reportService.getDailyRevenue(from, to);
            BigDecimal maxDaily = BigDecimal.ZERO;
            for (ReportRow row : daily) if (row.getAmount().compareTo(maxDaily) > 0) maxDaily = row.getAmount();
            req.setAttribute("daily", daily);
            req.setAttribute("maxDaily", maxDaily);
            req.setAttribute("fromDate", from.toString());
            req.setAttribute("toDate", to.toString());
        } catch (Exception e) { throw new ServletException(e); }
        req.setAttribute("pageTitle", "Bảng điều khiển");
        req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
    }

    /** Parse yyyy-MM-dd; rỗng/sai → mặc định. */
    private LocalDate parseDate(String s, LocalDate fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return fallback; }
    }
}
