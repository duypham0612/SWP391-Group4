package com.cafe.controller.admin;

import com.cafe.service.admin.ReportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Phase 7 · ReportServlet → /admin/report. Dashboard doanh thu toàn chuỗi (Admin). */
@WebServlet("/admin/report")
public class ReportServlet extends HttpServlet {

    private final ReportService service = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            req.setAttribute("summary", service.getChainSummary());
            req.setAttribute("byBranch", service.getRevenueByBranch());
            req.setAttribute("topProducts", service.getTopProducts(10));
            req.setAttribute("byMethod", service.getPaymentBreakdown());
            req.setAttribute("pageTitle", "Doanh thu toàn chuỗi");
            req.getRequestDispatcher("/WEB-INF/views/admin/report.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }
}
