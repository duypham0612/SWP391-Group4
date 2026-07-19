package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.model.BranchMenuItem;
import com.cafe.service.shared.BranchMenuService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * M8 · ManagerMenuServlet → /manager/menu. list | toggleAvailable | setLocalPrice.
 * Dùng chung BranchMenuService nhưng KHOÁ theo chi nhánh của manager (không chọn chi nhánh khác).
 */
@WebServlet("/manager/menu")
public class ManagerMenuServlet extends HttpServlet {

    private final BranchMenuService service = new BranchMenuService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("items", service.listForBranch(branchId));
            req.setAttribute("branchId", branchId);
            req.setAttribute("pageTitle", "Menu chi nhánh");
            req.getRequestDispatcher("/WEB-INF/views/manager/branch-menu.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        String action = req.getParameter("action");
        try {
            if ("hideMany".equals(action)) {   // ẩn (ngừng bán) nhiều món đã tick cùng lúc
                Set<Integer> ids = new HashSet<>();
                String[] picks = req.getParameterValues("pick");
                if (picks != null) for (String p : picks) {
                    try { ids.add(Integer.parseInt(p)); } catch (NumberFormatException ignore) {}
                }
                service.hideMany(branchId, ids);
                req.getSession().setAttribute("flashOk", "Đã ẩn (ngừng bán) " + ids.size() + " món đã chọn.");
                resp.sendRedirect(req.getContextPath() + "/manager/menu");
                return;
            }
            int productId = Integer.parseInt(req.getParameter("productId"));
            BranchMenuItem cur = findItem(branchId, productId);
            boolean available = cur != null && cur.isAvailable();
            BigDecimal localPrice = cur != null ? cur.getLocalPrice() : null;
            boolean is86 = cur != null && cur.isIs86();

            if ("toggleAvailable".equals(action)) {
                available = !available;
            } else if ("setLocalPrice".equals(action)) {
                String lp = req.getParameter("localPrice");
                localPrice = (lp == null || lp.isBlank()) ? null : new BigDecimal(lp.trim());
            } else if ("toggle86".equals(action)) {
                is86 = !is86;
            }
            service.save(branchId, productId, available, localPrice, is86);
            resp.sendRedirect(req.getContextPath() + "/manager/menu");
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/manager/menu");
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Dữ liệu menu hoặc giá không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/manager/menu");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private BranchMenuItem findItem(int branchId, int productId) throws Exception {
        List<BranchMenuItem> items = service.listForBranch(branchId);
        for (BranchMenuItem it : items) if (it.getProductId() == productId) return it;
        return null;
    }
}
