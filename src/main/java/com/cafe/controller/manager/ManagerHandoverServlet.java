package com.cafe.controller.manager;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.barista.HandoverService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Bàn giao không có ca barista nhận sẽ được giao cho quản lý chi nhánh ở đây. */
@WebServlet("/manager/handover")
public class ManagerHandoverServlet extends HttpServlet {
    private final HandoverService service = new HandoverService();
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req); User user = SessionUtil.currentUser(req);
        try { req.setAttribute("handovers", service.getManagerFallbacks(branchId, user.getUserId())); req.setAttribute("pageTitle", "Bàn giao cần tiếp nhận"); req.getRequestDispatcher("/WEB-INF/views/manager/handover.jsp").forward(req, resp); }
        catch (Exception e) { throw new ServletException(e); }
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req); User user = SessionUtil.currentUser(req);
        try {
            int handoverId = number(req, "handoverId");
            if ("acknowledge".equals(req.getParameter("action"))) service.acknowledge(branchId, handoverId, user.getUserId());
            else if ("updateTask".equals(req.getParameter("action"))) service.updateTaskStatus(branchId, handoverId, number(req, "taskId"), req.getParameter("status"), user.getUserId());
            req.getSession().setAttribute("flashOk", "Đã cập nhật bàn giao.");
        } catch (IllegalArgumentException | IllegalStateException e) { req.getSession().setAttribute("flashError", e.getMessage()); }
        catch (Exception e) { throw new ServletException(e); }
        resp.sendRedirect(req.getContextPath() + "/manager/handover");
    }
    private static int number(HttpServletRequest req, String key) { try { int n = Integer.parseInt(req.getParameter(key)); if(n > 0) return n; } catch (NumberFormatException ignored) {} throw new IllegalArgumentException("Dữ liệu bàn giao không hợp lệ."); }
}
