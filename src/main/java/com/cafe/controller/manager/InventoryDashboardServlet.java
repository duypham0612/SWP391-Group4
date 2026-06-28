package com.cafe.controller.manager;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.shared.InventoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** M5 · InventoryDashboardServlet → /manager/inventory. list | ledger | setThreshold. */
@WebServlet("/manager/inventory")
public class InventoryDashboardServlet extends HttpServlet {

    private final InventoryService inventoryService = new InventoryService();
    private final IngredientService ingredientService = new IngredientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = branchId(req);
        String action = req.getParameter("action");
        try {
            if ("ledger".equals(action)) {
                int ingredientId = Integer.parseInt(req.getParameter("ingredientId"));
                req.setAttribute("ingredient", ingredientService.getIngredient(ingredientId));
                req.setAttribute("ledger", inventoryService.getIngredientLedger(branchId, ingredientId));
                req.setAttribute("pageTitle", "Sổ cái tồn kho");
                req.getRequestDispatcher("/WEB-INF/views/manager/inventory-ledger.jsp").forward(req, resp);
            } else {
                req.setAttribute("inventory", inventoryService.getBranchInventory(branchId));
                req.setAttribute("lowStock", inventoryService.getLowStock(branchId));
                req.setAttribute("pageTitle", "Tồn kho");
                req.getRequestDispatcher("/WEB-INF/views/manager/inventory-list.jsp").forward(req, resp);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = branchId(req);
        try {
            if ("setThreshold".equals(req.getParameter("action"))) {
                int ingredientId = Integer.parseInt(req.getParameter("ingredientId"));
                BigDecimal th = new BigDecimal(req.getParameter("threshold").trim());
                inventoryService.setMinThreshold(branchId, ingredientId, th);
            }
            resp.sendRedirect(req.getContextPath() + "/manager/inventory");
        } catch (Exception e) { throw new ServletException(e); }
    }

    public static int branchId(HttpServletRequest req) {
        User u = SessionUtil.currentUser(req);
        if (u != null && u.getBranchId() != null) return u.getBranchId();
        String p = req.getParameter("branchId");
        return (p != null && !p.isBlank()) ? Integer.parseInt(p) : 1;
    }
}
