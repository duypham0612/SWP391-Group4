package com.cafe.controller.manager;

import com.cafe.common.BusinessDay;
import com.cafe.service.manager.ManagerDashboardService;
import com.cafe.service.barista.HandoverService;
import com.cafe.common.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/** M1 · ManagerDashboardServlet → /manager/dashboard. show. */
@WebServlet("/manager/dashboard")
public class ManagerDashboardServlet extends HttpServlet {

    private final ManagerDashboardService service = new ManagerDashboardService();
    private final HandoverService handoverService = new HandoverService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        LocalDate today = LocalDate.now(BusinessDay.VN_ZONE);
        try {
            req.setAttribute("summary", service.getTodaySummary(branchId, today));
            req.setAttribute("staffOnShift", service.getStaffOnShift(branchId, today));
            req.setAttribute("lowStockAlerts", service.getLowStockAlerts(branchId));
            req.setAttribute("oversoldAlerts", service.getOversoldAlerts(branchId));
            req.setAttribute("managerHandoverFallbacks", handoverService.getManagerFallbacks(branchId, SessionUtil.currentUser(req).getUserId()));
            req.setAttribute("today", today);
            req.setAttribute("pageTitle", "Tổng quan chi nhánh");
            req.getRequestDispatcher("/WEB-INF/views/manager/dashboard.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }
}
