package com.cafe.controller.manager;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.StockReceipt;
import com.cafe.model.User;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.manager.StockReceiptService;
import com.cafe.service.manager.SupplierService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** M6 · StockReceiptServlet → /manager/receipt. list | new | view | create | addLine | confirm | cancel. */
@WebServlet("/manager/receipt")
public class StockReceiptServlet extends HttpServlet {

    private final StockReceiptService service = new StockReceiptService();
    private final SupplierService supplierService = new SupplierService();
    private final IngredientService ingredientService = new IngredientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("suppliers", supplierService.getSupplierListActive());
                req.setAttribute("pageTitle", "Tạo phiếu nhập");
                req.getRequestDispatcher("/WEB-INF/views/manager/receipt-form.jsp").forward(req, resp);
            } else if ("view".equals(action)) {
                showReceipt(req, resp, Integer.parseInt(req.getParameter("id")));
            } else {
                req.setAttribute("receipts", service.getReceiptList(branchId));
                req.setAttribute("pageTitle", "Phiếu nhập kho");
                req.getRequestDispatcher("/WEB-INF/views/manager/receipt-list.jsp").forward(req, resp);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        String ctx = req.getContextPath();
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        String action = req.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "create": {
                    StockReceipt r = new StockReceipt();
                    r.setBranchId(branchId);
                    r.setReceivedBy(u.getUserId());
                    String sup = req.getParameter("supplierId");
                    r.setSupplierId(sup == null || sup.isBlank() ? null : Integer.parseInt(sup));
                    r.setNote(trim(req.getParameter("note")));
                    int id = service.createDraftReceipt(r);
                    resp.sendRedirect(ctx + "/manager/receipt?action=view&id=" + id);
                    return;
                }
                case "addLine": {
                    int rid = Integer.parseInt(req.getParameter("receiptId"));
                    BigDecimal qty = dec(req.getParameter("quantity"));
                    BigDecimal cost = dec(req.getParameter("unitCost"));
                    if (qty.signum() > 0) service.addReceiptLine(rid, Integer.parseInt(req.getParameter("ingredientId")), qty, cost);
                    resp.sendRedirect(ctx + "/manager/receipt?action=view&id=" + rid);
                    return;
                }
                case "removeLine": {
                    int rid = Integer.parseInt(req.getParameter("receiptId"));
                    service.removeReceiptLine(Integer.parseInt(req.getParameter("detailId")));
                    resp.sendRedirect(ctx + "/manager/receipt?action=view&id=" + rid);
                    return;
                }
                case "confirm": {
                    int rid = Integer.parseInt(req.getParameter("receiptId"));
                    service.confirmReceipt(rid, branchId, u.getUserId());
                    resp.sendRedirect(ctx + "/manager/receipt?action=view&id=" + rid);
                    return;
                }
                case "cancel": {
                    int rid = Integer.parseInt(req.getParameter("receiptId"));
                    service.cancelReceipt(rid);
                    resp.sendRedirect(ctx + "/manager/receipt?action=view&id=" + rid);
                    return;
                }
                default: resp.sendError(400);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void showReceipt(HttpServletRequest req, HttpServletResponse resp, int id) throws Exception {
        StockReceipt r = service.getReceipt(id);
        if (r == null) { resp.sendError(404); return; }
        req.setAttribute("receipt", r);
        req.setAttribute("details", service.getReceiptDetails(id));
        req.setAttribute("ingredients", ingredientService.getIngredientList());
        req.setAttribute("pageTitle", "Phiếu nhập #" + id);
        req.getRequestDispatcher("/WEB-INF/views/manager/receipt-detail.jsp").forward(req, resp);
    }

    private BigDecimal dec(String s) {
        try { return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
    private String trim(String s) { return s == null ? null : s.trim(); }
}
