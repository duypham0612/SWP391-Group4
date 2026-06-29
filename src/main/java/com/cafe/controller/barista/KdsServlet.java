package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.barista.KdsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** B1 · KdsServlet → /barista/kds. Hàng chờ bếp + start/markReady (★ auto-deduct ở markReady). */
@WebServlet("/barista/kds")
public class KdsServlet extends HttpServlet {

    private final KdsService service = new KdsService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("queue", service.getQueue(branchId));
            req.setAttribute("pageTitle", "Hàng chờ (KDS)");
            req.getRequestDispatcher("/WEB-INF/views/barista/kds.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        String action = req.getParameter("action");
        try {
            int itemId = Integer.parseInt(req.getParameter("orderItemId"));
            if ("start".equals(action)) {
                service.startItem(itemId, userId);
            } else if ("markReady".equals(action)) {
                service.markReady(itemId, userId);
            } else if ("bump".equals(action)) {
                service.bump(itemId);
            }
            resp.sendRedirect(req.getContextPath() + "/barista/kds");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
