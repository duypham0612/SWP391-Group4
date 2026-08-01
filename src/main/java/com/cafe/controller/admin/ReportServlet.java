package com.cafe.controller.admin;

import com.cafe.model.ChainSummary;
import com.cafe.model.ReportRow;
import com.cafe.service.admin.ReportService;
import com.cafe.web.renderer.ReportCsvRenderer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * Doanh thu toàn chuỗi đã MERGE vào Dashboard (/dashboard).
 * Servlet này giữ lại làm endpoint XUẤT EXCEL (CSV) theo khoảng ngày,
 * và redirect truy cập xem thường về /dashboard.
 */
@WebServlet("/admin/report")
public class ReportServlet extends HttpServlet {

    private final ReportService service;
    private final ReportCsvRenderer csvRenderer;

    public ReportServlet() {
        this(new ReportService(), new ReportCsvRenderer());
    }

    ReportServlet(ReportService service, ReportCsvRenderer csvRenderer) {
        this.service = Objects.requireNonNull(service, "service");
        this.csvRenderer = Objects.requireNonNull(csvRenderer, "csvRenderer");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!"export".equals(req.getParameter("action"))) {
            // đã gộp vào bảng điều khiển — giữ tương thích link/bookmark cũ
            String qs = req.getQueryString() == null ? "" : "?" + req.getQueryString();
            resp.sendRedirect(req.getContextPath() + "/dashboard" + qs);
            return;
        }
        LocalDate today = com.cafe.common.BusinessDay.todayVn();
        LocalDate to = parseDate(req.getParameter("to"), today);
        LocalDate from = parseDate(req.getParameter("from"), to.minusDays(29));
        if (from.isAfter(to)) { LocalDate t = from; from = to; to = t; }
        try {
            csvRenderer.render(resp, from, to,
                    service.getChainSummary(from, to),
                    service.getRevenueByBranch(from, to),
                    service.getPaymentBreakdown(from, to),
                    service.getTopProducts(20, from, to),
                    service.getDailyRevenue(from, to));
        } catch (Exception e) { throw new ServletException(e); }
    }

    private LocalDate parseDate(String s, LocalDate fb) {
        if (s == null || s.isBlank()) return fb;
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return fb; }
    }
}
