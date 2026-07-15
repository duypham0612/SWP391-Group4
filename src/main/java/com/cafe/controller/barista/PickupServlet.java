package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.barista.PickupService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** B2 · PickupServlet → /barista/pickup. Món READY gom theo bàn → kiểm tra cả đơn → markServed. */
@WebServlet("/barista/pickup")
public class PickupServlet extends HttpServlet {

    private final PickupService service = new PickupService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("tickets", service.getReadyTickets(branchId));
            req.setAttribute("pageTitle", "Món chờ giao");
            boolean partial = "1".equals(req.getParameter("partial"));
            if (!partial) BaristaShift.expose(req, "/barista/pickup");   // trực ca: banner + khoá thao tác
            String view = partial
                    ? "/WEB-INF/views/barista/pickup_cards.jsp"
                    : "/WEB-INF/views/barista/pickup.jsp";
            req.getRequestDispatcher(view).forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        String action = req.getParameter("action");
        if (BaristaShift.guardWrite(req, resp, action, "/barista/pickup")) return;   // vào ca / chặn ngoài ca
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("markServed".equals(action)) {
                service.serveItem(Integer.parseInt(req.getParameter("orderItemId")), userId, branchId);
            } else if ("serveAllReady".equals(action)) {
                service.serveAllReady(Integer.parseInt(req.getParameter("orderId")), userId, branchId);
            }
            resp.sendRedirect(req.getContextPath() + "/barista/pickup");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
