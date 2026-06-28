package com.cafe.controller.manager;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.manager.StockAdjustmentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** M7 · ReconciliationServlet → /manager/reconciliation. list | new | create. */
@WebServlet("/manager/reconciliation")
public class ReconciliationServlet extends HttpServlet {

    private final StockAdjustmentService service = new StockAdjustmentService();
    private final IngredientService ingredientService = new IngredientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("new".equals(req.getParameter("action"))) {
                req.setAttribute("ingredients", ingredientService.getIngredientList());
                req.setAttribute("pageTitle", "Điều chỉnh tồn");
                req.getRequestDispatcher("/WEB-INF/views/manager/reconciliation-form.jsp").forward(req, resp);
            } else {
                req.setAttribute("adjustments", service.getAdjustmentList(branchId));
                req.setAttribute("pageTitle", "Đối soát tồn");
                req.getRequestDispatcher("/WEB-INF/views/manager/reconciliation-list.jsp").forward(req, resp);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        try {
            int ingredientId = Integer.parseInt(req.getParameter("ingredientId"));
            BigDecimal actual = new BigDecimal(req.getParameter("actualQty").trim());
            String reason = req.getParameter("reason");
            service.createAdjustment(branchId, ingredientId, actual, reason, u.getUserId());
            resp.sendRedirect(req.getContextPath() + "/manager/reconciliation");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
