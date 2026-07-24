package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Branch;
import com.cafe.service.admin.BranchService;
import com.cafe.service.shared.BranchMenuService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** Admin updates per-branch menu availability and local pricing. */
@WebServlet("/admin/branch-menu")
public class BranchMenuServlet extends HttpServlet {

    private final BranchMenuService service = new BranchMenuService();
    private final BranchService branchService = new BranchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String bid = req.getParameter("branchId");
        try {
            if (bid == null || bid.isBlank()) {
                req.setAttribute("branches", branchService.getBranchListActive());
                req.setAttribute("pageTitle", "Menu chi nhanh - chon chi nhanh");
                req.getRequestDispatcher("/WEB-INF/views/admin/branch-menu-branches.jsp").forward(req, resp);
                return;
            }
            int branchId = Integer.parseInt(bid);
            Branch b = branchService.getBranch(branchId);
            if (b == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            req.setAttribute("branch", b);
            req.setAttribute("items", service.listForBranch(branchId));
            req.setAttribute("pageTitle", "Menu: " + b.getName());
            req.getRequestDispatcher("/WEB-INF/views/admin/branch-menu.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF"); return; }
        String ctx = req.getContextPath();
        int branchId = parsePositiveInt(req.getParameter("branchId"));
        int productId = parsePositiveInt(req.getParameter("productId"));
        String action = req.getParameter("action");
        try {
            if (branchId <= 0 || productId <= 0) {
                req.getSession().setAttribute("flashError", "Du lieu menu khong hop le.");
                resp.sendRedirect(ctx + "/admin/branch-menu");
                return;
            }
            if ("remove".equals(action)) {
                service.remove(branchId, productId);
            } else {
                boolean available = req.getParameter("available") != null;
                boolean is86 = req.getParameter("is86") != null;
                BigDecimal localPrice = null;
                String lp = req.getParameter("localPrice");
                if (lp != null && !lp.isBlank()) {
                    try {
                        localPrice = new BigDecimal(lp.trim().replace(",", ""));
                    } catch (NumberFormatException ignored) {
                        req.getSession().setAttribute("flashError", "Gia rieng phai la so hop le.");
                        resp.sendRedirect(ctx + "/admin/branch-menu?branchId=" + branchId);
                        return;
                    }
                    if (localPrice.signum() < 0) {
                        req.getSession().setAttribute("flashError", "Gia rieng khong duoc am.");
                        resp.sendRedirect(ctx + "/admin/branch-menu?branchId=" + branchId);
                        return;
                    }
                }
                if (!available) is86 = false;
                service.save(branchId, productId, available, localPrice, is86);
            }
            resp.sendRedirect(ctx + "/admin/branch-menu?branchId=" + branchId);
        } catch (Exception e) { throw new ServletException(e); }
    }

    private int parsePositiveInt(String raw) {
        try {
            if (raw == null || raw.isBlank()) return 0;
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
