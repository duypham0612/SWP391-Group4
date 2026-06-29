package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.service.shared.BranchMenuService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** B3 · EightySixServlet → /barista/eightysix. Bật/tắt hết món (khoá khỏi POS + QR). */
@WebServlet("/barista/eightysix")
public class EightySixServlet extends HttpServlet {

    private final BranchMenuService service = new BranchMenuService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("items", service.getMenuAvailability(branchId));
            req.setAttribute("pageTitle", "Hết món (86)");
            req.getRequestDispatcher("/WEB-INF/views/barista/eightysix.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("toggle86".equals(req.getParameter("action"))) {
                int productId = Integer.parseInt(req.getParameter("productId"));
                boolean is86 = "true".equals(req.getParameter("is86"));
                java.time.LocalDateTime eta = null;
                String etaStr = req.getParameter("backInEta");
                if (is86 && etaStr != null && !etaStr.isBlank()) {
                    try { eta = java.time.LocalDateTime.parse(etaStr); }
                    catch (java.time.format.DateTimeParseException ignore) { /* bỏ qua ETA sai định dạng */ }
                }
                service.set86(branchId, productId, is86, eta);
            }
            resp.sendRedirect(req.getContextPath() + "/barista/eightysix");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
