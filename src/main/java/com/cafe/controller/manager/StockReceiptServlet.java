package com.cafe.controller.manager;

import com.cafe.common.BusinessException;
import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
import com.cafe.model.StockReceipt;
import com.cafe.model.StockReceiptDetail;
import com.cafe.model.User;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.manager.StockReceiptService;
import com.cafe.service.manager.SupplierService;
import com.cafe.web.form.StockReceiptForm;
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

    private final StockReceiptService service;
    private final SupplierService supplierService;
    private final IngredientService ingredientService;

    public StockReceiptServlet() {
        this(new StockReceiptService(), new SupplierService(), new IngredientService());
    }
    StockReceiptServlet(StockReceiptService service, SupplierService supplierService,
                        IngredientService ingredientService) {
        this.service = java.util.Objects.requireNonNull(service);
        this.supplierService = java.util.Objects.requireNonNull(supplierService);
        this.ingredientService = java.util.Objects.requireNonNull(ingredientService);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("suppliers", supplierService.getSupplierListActive());
                req.setAttribute("ingredients", ingredientService.getIngredientList());
                req.setAttribute("unitChoicesByIngredient", ingredientService.getActiveUnitChoicesByIngredient());
                req.setAttribute("pageTitle", "Tạo phiếu nhập");
                req.getRequestDispatcher("/WEB-INF/views/manager/receipt-form.jsp").forward(req, resp);
            } else if ("view".equals(action)) {
                showReceipt(req, resp, req.getParameter("id"), branchId);
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
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
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
                    StockReceiptDetail firstLine = new StockReceiptDetail();
                    firstLine.setIngredientId(Integer.parseInt(req.getParameter("ingredientId")));
                    firstLine.setEnteredQuantity(dec(req.getParameter("quantity")));
                    firstLine.setUnitCost(dec(req.getParameter("unitCost")));
                    firstLine.setUnitChoice(Integer.parseInt(req.getParameter("unitConversionId")));
                    String batchId = service.createDraftReceipt(r, firstLine);
                    resp.sendRedirect(ctx + "/manager/receipt?action=view&id=" + batchId);
                    return;
                }
                case "addLine": {
                    String batchId = req.getParameter("receiptBatchId");
                    redirect = ctx + "/manager/receipt?action=view&id=" + batchId;
                    BigDecimal qty = dec(req.getParameter("quantity"));
                    BigDecimal cost = dec(req.getParameter("unitCost"));
                    int conversionId = Integer.parseInt(req.getParameter("unitConversionId"));
                    if (qty.signum() > 0) service.addReceiptLine(batchId, branchId,
                            Integer.parseInt(req.getParameter("ingredientId")), qty, cost, conversionId);
                    resp.sendRedirect(redirect);
                    return;
                }
                case "addLines": {   // tickbox chọn nhiều nguyên liệu cùng lúc
                    StockReceiptForm form = StockReceiptForm.from(req);
                    String batchId = form.receiptBatchId();
                    redirect = ctx + "/manager/receipt?action=view&id=" + batchId;
                    List<StockReceiptDetail> lines = new ArrayList<>();
                    for (StockReceiptForm.Line line : form.lines()) {
                        if (line.quantity().signum() <= 0) continue;
                        StockReceiptDetail detail = new StockReceiptDetail();
                        detail.setIngredientId(line.ingredientId());
                        detail.setEnteredQuantity(line.quantity());
                        detail.setUnitCost(line.unitCost());
                        detail.setUnitChoice(line.unitConversionId());
                        lines.add(detail);
                    }
                    service.addReceiptLines(batchId, branchId, lines);
                    resp.sendRedirect(redirect);
                    return;
                }
                case "removeLine": {
                    String batchId = req.getParameter("receiptBatchId");
                    redirect = ctx + "/manager/receipt?action=view&id=" + batchId;
                    service.removeReceiptLine(batchId,
                            Integer.parseInt(req.getParameter("lineId")), branchId);
                    resp.sendRedirect(redirect);
                    return;
                }
                case "confirm": {
                    String batchId = req.getParameter("receiptBatchId");
                    redirect = ctx + "/manager/receipt?action=view&id=" + batchId;
                    service.confirmReceipt(batchId, branchId, u.getUserId());
                    resp.sendRedirect(redirect);
                    return;
                }
                case "cancel": {
                    String batchId = req.getParameter("receiptBatchId");
                    redirect = ctx + "/manager/receipt?action=view&id=" + batchId;
                    service.cancelReceipt(batchId, branchId);
                    resp.sendRedirect(redirect);
                    return;
                }
                case "cancelMany": {   // tickbox huỷ nhiều phiếu (chỉ phiếu DRAFT bị huỷ)
                    String[] ids = req.getParameterValues("rid");
                    List<String> list = new ArrayList<>();
                    if (ids != null) for (String s : ids) {
                        if (s != null && !s.isBlank() && s.length() <= 36) list.add(s);
                    }
                    service.cancelManyReceipts(list, branchId);
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

    private void showReceipt(HttpServletRequest req, HttpServletResponse resp, String batchId, int branchId) throws Exception {
        StockReceipt r = service.getReceipt(batchId, branchId);
        if (r == null) { resp.sendError(404); return; }
        req.setAttribute("receipt", r);
        req.setAttribute("details", service.getReceiptDetails(batchId, branchId));
        req.setAttribute("ingredients", ingredientService.getIngredientList());
        req.setAttribute("unitChoicesByIngredient",ingredientService.getActiveUnitChoicesByIngredient());
        req.setAttribute("pageTitle", "Phiếu nhập #" + batchId);
        req.getRequestDispatcher("/WEB-INF/views/manager/receipt-detail.jsp").forward(req, resp);
    }

    private BigDecimal dec(String s) {
        return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim());
    }
    private String trim(String s) { return s == null ? null : s.trim(); }
}
