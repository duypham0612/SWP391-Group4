package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.cashier.TableSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** C3 · TableServlet → /cashier/table. Sơ đồ bàn + phiên bàn. */
@WebServlet("/cashier/table")
public class TableServlet extends HttpServlet {

    private final TableSessionService service = new TableSessionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("tables", service.getFloorMap(branchId));
            req.setAttribute("pageTitle", "Sơ đồ bàn");
            req.getRequestDispatcher("/WEB-INF/views/cashier/table-map.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        String action = req.getParameter("action");
        String ctx = req.getContextPath();
        try {
            if ("openTable".equals(action)) {
                int tableId = Integer.parseInt(req.getParameter("tableId"));
                int sessionId = service.openSession(branchId, tableId, userId);
                resp.sendRedirect(ctx + "/cashier/pos?sessionId=" + sessionId);
                return;
            } else if ("closeTable".equals(action)) {
                service.closeSession(Integer.parseInt(req.getParameter("sessionId")));
            } else if ("setStatus".equals(action)) {
                service.setTableStatus(Integer.parseInt(req.getParameter("tableId")), req.getParameter("status"));
            } else if ("merge".equals(action)) {
                service.mergeSessions(Integer.parseInt(req.getParameter("srcSessionId")),
                        Integer.parseInt(req.getParameter("dstSessionId")));
            }
            resp.sendRedirect(ctx + "/cashier/table");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
