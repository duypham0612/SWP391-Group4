package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.barista.WasteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** B5 · WasteServlet → /barista/waste. Ghi hao hụt/làm lại (qua ledger). */
@WebServlet("/barista/waste")
public class WasteServlet extends HttpServlet {

    private final WasteService service = new WasteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("ingredients", service.getIngredients());
            req.setAttribute("logs", service.getWasteLogs(branchId));
            req.setAttribute("pageTitle", "Hao hụt / Làm lại");
            req.getRequestDispatcher("/WEB-INF/views/barista/waste.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
        try {
            if ("create".equals(req.getParameter("action"))) {
                int ingredientId = Integer.parseInt(req.getParameter("ingredientId"));
                BigDecimal qty = new BigDecimal(req.getParameter("quantity").trim());
                String wasteType = req.getParameter("wasteType");
                String reason = req.getParameter("reason");
                if (qty.signum() > 0) service.logWaste(branchId, ingredientId, qty, wasteType, reason, userId);
                else req.getSession().setAttribute("flashError", "Số lượng phải > 0.");
            }
            resp.sendRedirect(req.getContextPath() + "/barista/waste");
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Số lượng không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/barista/waste");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
