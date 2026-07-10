package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.barista.HandoverService;
import com.cafe.service.manager.AttendanceService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/** B7 · ShiftHandoverServlet → /barista/handover. Ghi chú bàn giao ca + KPI lead-time. */
@WebServlet("/barista/handover")
public class ShiftHandoverServlet extends HttpServlet {

    private final HandoverService service = new HandoverService();
    private final AttendanceService attendanceService = new AttendanceService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        try {
            req.setAttribute("handovers", service.getHandovers(branchId));
            req.setAttribute("kpi", service.getKpi(branchId));
            if (u != null) {
                req.setAttribute("clockStatus", attendanceService.getMyShiftStatus(u.getUserId(), branchId, LocalDate.now()));
            }
            req.setAttribute("pageTitle", "Ca làm & Bàn giao");
            req.getRequestDispatcher("/WEB-INF/views/barista/handover.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
        String action = req.getParameter("action");
        String redirect = req.getContextPath() + "/barista/handover";
        try {
            if ("clockIn".equals(action)) {
                attendanceService.clockIn(userId, branchId);
                req.getSession().setAttribute("flashOk", "Đã vào ca.");
            } else if ("clockOut".equals(action)) {
                attendanceService.clockOut(userId, branchId);
                req.getSession().setAttribute("flashOk", "Đã tan ca.");
            } else if ("create".equals(action)) {
                String note = req.getParameter("note");
                if (note != null && !note.isBlank()) {
                    service.createHandover(branchId, note.trim(), userId);
                } else {
                    req.getSession().setAttribute("flashError", "Nội dung bàn giao không được rỗng.");
                }
            }
            resp.sendRedirect(redirect);
        } catch (IllegalStateException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(redirect);
        } catch (Exception e) { throw new ServletException(e); }
    }
}
