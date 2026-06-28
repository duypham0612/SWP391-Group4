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

/** B2 · PickupServlet → /barista/pickup. Bảng món READY → markServed. */
@WebServlet("/barista/pickup")
public class PickupServlet extends HttpServlet {

    private final KdsService service = new KdsService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("readyItems", service.getReadyItems(branchId));
            req.setAttribute("pageTitle", "Món sẵn lấy");
            req.getRequestDispatcher("/WEB-INF/views/barista/pickup.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        try {
            if ("markServed".equals(req.getParameter("action"))) {
                service.markServed(Integer.parseInt(req.getParameter("orderItemId")), userId);
            }
            resp.sendRedirect(req.getContextPath() + "/barista/pickup");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
