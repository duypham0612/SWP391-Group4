package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.StockReceipt;
import com.cafe.model.StockReceiptDetail;
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
import java.util.ArrayList;
import java.util.List;

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
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Mã phiếu nhập không hợp lệ.");
            resp.sendRedirect(req.getContextPath() + "/manager/receipt");
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
        String redirect = ctx + "/manager/receipt";
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
                    redirect = ctx + "/manager/receipt?action=view&id=" + rid;
                    BigDecimal qty = dec(req.getParameter("quantity"));
                    BigDecimal cost = dec(req.getParameter("unitCost"));
                    String unit = trim(req.getParameter("unit"));
                    if (qty.signum() > 0) service.addReceiptLine(rid, Integer.parseInt(req.getParameter("ingredientId")), qty, cost, unit);
                    resp.sendRedirect(redirect);
                    return;
                }
                case "addLines": {   // tickbox chọn nhiều nguyên liệu cùng lúc
                    int rid = Integer.parseInt(req.getParameter("receiptId"));
                    redirect = ctx + "/manager/receipt?action=view&id=" + rid;
                    String[] picks = req.getParameterValues("pick");
                    List<StockReceiptDetail> lines = new ArrayList<>();
                    if (picks != null) {
                        for (String p : picks) {
                            int ingId;
                            try { ingId = Integer.parseInt(p); } catch (NumberFormatException e) { continue; }
                            BigDecimal qty = dec(req.getParameter("qty_" + ingId));
                            if (qty.signum() <= 0) continue;   // bỏ qua dòng được tick nhưng chưa nhập SL
                            StockReceiptDetail d = new StockReceiptDetail();
                            d.setIngredientId(ingId);
                            d.setQuantity(qty);
                            d.setUnitCost(dec(req.getParameter("cost_" + ingId)));
                            d.setUnit(trim(req.getParameter("unit_" + ingId)));
                            lines.add(d);
                        }
                    }
                    service.addReceiptLines(rid, lines);
                    resp.sendRedirect(redirect);
                    return;
                }
                case "removeLine": {
                    int rid = Integer.parseInt(req.getParameter("receiptId"));
                    redirect = ctx + "/manager/receipt?action=view&id=" + rid;
                    service.removeReceiptLine(Integer.parseInt(req.getParameter("detailId")));
                    resp.sendRedirect(redirect);
                    return;
                }
                case "confirm": {
                    int rid = Integer.parseInt(req.getParameter("receiptId"));
                    redirect = ctx + "/manager/receipt?action=view&id=" + rid;
                    service.confirmReceipt(rid, branchId, u.getUserId());
                    resp.sendRedirect(redirect);
                    return;
                }
                case "cancel": {
                    int rid = Integer.parseInt(req.getParameter("receiptId"));
                    redirect = ctx + "/manager/receipt?action=view&id=" + rid;
                    service.cancelReceipt(rid);
                    resp.sendRedirect(redirect);
                    return;
                }
                case "cancelMany": {   // tickbox huỷ nhiều phiếu (chỉ phiếu DRAFT bị huỷ)
                    String[] ids = req.getParameterValues("rid");
                    List<Integer> list = new ArrayList<>();
                    if (ids != null) for (String s : ids) {
                        try { list.add(Integer.parseInt(s)); } catch (NumberFormatException ignore) {}
                    }
                    service.cancelManyReceipts(list);
                    resp.sendRedirect(ctx + "/manager/receipt");
                    return;
                }
                default: resp.sendError(400);
            }
        } catch (BusinessException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(redirect);
        } catch (NumberFormatException e) {
            req.getSession().setAttribute("flashError", "Số lượng, đơn giá hoặc mã phiếu nhập không hợp lệ.");
            resp.sendRedirect(redirect);
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
        return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim());
    }
    private String trim(String s) { return s == null ? null : s.trim(); }
}
