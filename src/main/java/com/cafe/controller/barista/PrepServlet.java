package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.barista.PrepService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** B4 · PrepServlet → /barista/prep. Pha sẵn (RAW→PREPPED, Contract #2). */
@WebServlet("/barista/prep")
public class PrepServlet extends HttpServlet {

    private final PrepService service = new PrepService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("preppedIngredients", service.getPreppedIngredients());
            req.setAttribute("batches", service.getBatches(branchId));
            req.setAttribute("pageTitle", "Pha sẵn (Prep)");
            req.getRequestDispatcher("/WEB-INF/views/barista/prep.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
        String action = req.getParameter("action");
        try {
            if ("createBatch".equals(action)) {
                int preppedId = Integer.parseInt(req.getParameter("preppedIngredientId"));
                BigDecimal qty = new BigDecimal(req.getParameter("quantityProduced").trim());
                if (qty.signum() > 0) service.createBatch(branchId, preppedId, qty, userId);
                else req.getSession().setAttribute("flashError", "Sản lượng phải > 0.");
            } else if ("cancelBatch".equals(action)) {
                int batchId = Integer.parseInt(req.getParameter("prepBatchId"));
                service.cancelBatch(branchId, batchId, userId);
                req.getSession().setAttribute("flashOk", "Đã huỷ mẻ — tồn kho hoàn lại qua sổ cái (txn bù).");
            } else if ("updateBatch".equals(action)) {
                int batchId = Integer.parseInt(req.getParameter("prepBatchId"));
                BigDecimal qty = new BigDecimal(req.getParameter("quantityProduced").trim());
                if (qty.signum() > 0) {
                    service.updateBatch(branchId, batchId, qty, userId);
                    req.getSession().setAttribute("flashOk", "Đã cập nhật sản lượng — chênh lệch ghi vào sổ cái.");
                } else {
                    req.getSession().setAttribute("flashError", "Sản lượng phải > 0.");
                }
            }
            resp.sendRedirect(req.getContextPath() + "/barista/prep");
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Sản lượng không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/barista/prep");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
