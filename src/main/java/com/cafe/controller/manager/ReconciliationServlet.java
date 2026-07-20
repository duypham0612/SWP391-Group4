package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.StockAdjustment;
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
import java.util.ArrayList;
import java.util.List;

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
                req.setAttribute("pageTitle", "Đối soát tồn & hao hụt");
                req.setAttribute("combinedInventoryView", Boolean.TRUE);
                req.getRequestDispatcher("/manager/waste").forward(req, resp);
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
            // Tickbox kiểm kê nhiều nguyên liệu: mỗi nguyên liệu được tick + có nhập tồn thực tế → 1 dòng điều chỉnh.
            String[] picks = req.getParameterValues("pick");
            List<StockAdjustment> lines = new ArrayList<>();
            if (picks != null) {
                for (String p : picks) {
                    int ingId;
                    try { ingId = Integer.parseInt(p); } catch (NumberFormatException e) { continue; }
                    String aq = req.getParameter("actual_" + ingId);
                    if (aq == null || aq.isBlank()) continue;   // tick nhưng chưa nhập tồn thực tế → bỏ qua
                    BigDecimal actual;
                    try { actual = new BigDecimal(aq.trim()); } catch (NumberFormatException e) { throw e; }
                    StockAdjustment a = new StockAdjustment();
                    a.setIngredientId(ingId);
                    a.setActualQty(actual);
                    a.setReason(trim(req.getParameter("reason_" + ingId)));
                    a.setUnit(trim(req.getParameter("unit_" + ingId)));
                    lines.add(a);
                }
            }
            service.createAdjustments(branchId, lines, u.getUserId());
            resp.sendRedirect(req.getContextPath() + "/manager/reconciliation");
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/manager/reconciliation?action=new");
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Tồn thực tế không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/manager/reconciliation?action=new");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private String trim(String s) { return s == null ? null : s.trim(); }
}
