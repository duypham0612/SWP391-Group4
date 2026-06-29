package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Voucher;
import com.cafe.service.admin.BranchService;
import com.cafe.service.shared.VoucherService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/** A5 · VoucherServlet → /admin/voucher. Actions: list/create/update/toggleActive. */
@WebServlet("/admin/voucher")
public class VoucherServlet extends HttpServlet {

    private static final Set<String> TYPES = Set.of("PERCENT", "FIXED");
    private static final Set<String> SCOPES = Set.of("CHAIN", "BRANCH");

    private final VoucherService service = new VoucherService();
    private final BranchService branchService = new BranchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("voucher", new Voucher());
                forwardForm(req, resp, "Thêm voucher");
            } else if ("edit".equals(action)) {
                Voucher v = service.getVoucher(Integer.parseInt(req.getParameter("id")));
                if (v == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("voucher", v);
                forwardForm(req, resp, "Sửa voucher");
            } else {
                req.setAttribute("vouchers", service.getVoucherList());
                req.setAttribute("pageTitle", "Voucher");
                req.getRequestDispatcher("/WEB-INF/views/admin/voucher-list.jsp").forward(req, resp);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF"); return; }
        String ctx = req.getContextPath();
        String action = req.getParameter("action");
        try {
            if ("toggleActive".equals(action)) {
                service.toggleActive(Integer.parseInt(req.getParameter("id")));
                resp.sendRedirect(ctx + "/admin/voucher");
                return;
            }
            Voucher v = bind(req);
            if (v.getVoucherId() != 0) {                      // sửa: mã voucher KHÔNG đổi được
                Voucher existing = service.getVoucher(v.getVoucherId());
                if (existing != null) v.setCode(existing.getCode());
            }
            String error = validate(v);
            if (error != null) {
                req.setAttribute("voucher", v);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, v.getVoucherId() == 0 ? "Thêm voucher" : "Sửa voucher");
                return;
            }
            if (v.getVoucherId() == 0) service.createVoucher(v); else service.updateVoucher(v);
            resp.sendRedirect(ctx + "/admin/voucher");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private Voucher bind(HttpServletRequest req) {
        Voucher v = new Voucher();
        String id = req.getParameter("voucherId");
        if (id != null && !id.isBlank()) v.setVoucherId(Integer.parseInt(id));
        v.setCode(up(req.getParameter("code")));
        v.setDiscountType(trim(req.getParameter("discountType")));
        v.setDiscountValue(decimal(req.getParameter("discountValue")));
        v.setMinOrderAmount(decimal(req.getParameter("minOrderAmount")));
        v.setScope(trim(req.getParameter("scope")));
        String branch = req.getParameter("branchId");
        v.setBranchId(branch == null || branch.isBlank() ? null : Integer.parseInt(branch));
        v.setStartDate(dateTime(req.getParameter("startDate")));
        v.setEndDate(dateTime(req.getParameter("endDate")));
        String limit = req.getParameter("usageLimit");
        v.setUsageLimit(limit == null || limit.isBlank() ? null : Integer.parseInt(limit));
        v.setActive(req.getParameter("active") != null);
        return v;
    }

    private String validate(Voucher v) {
        if (v.getCode() == null || v.getCode().isBlank()) return "Mã voucher không được để trống.";
        if (v.getDiscountType() == null || !TYPES.contains(v.getDiscountType())) return "Loại giảm phải là PERCENT hoặc FIXED.";
        if (v.getDiscountValue() == null || v.getDiscountValue().signum() < 0) return "Giá trị giảm phải >= 0.";
        if ("PERCENT".equals(v.getDiscountType()) && v.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0)
            return "Giảm theo % không vượt quá 100.";
        if (v.getScope() == null || !SCOPES.contains(v.getScope())) return "Phạm vi phải là CHAIN hoặc BRANCH.";
        if ("BRANCH".equals(v.getScope()) && v.getBranchId() == null) return "Voucher phạm vi BRANCH phải chọn chi nhánh.";
        if ("CHAIN".equals(v.getScope())) v.setBranchId(null);
        if (v.getStartDate() != null && v.getEndDate() != null && v.getEndDate().isBefore(v.getStartDate()))
            return "Ngày kết thúc phải sau ngày bắt đầu.";
        return null;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        try { req.setAttribute("branches", branchService.getBranchListActive()); }
        catch (Exception e) { throw new ServletException(e); }
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/voucher-form.jsp").forward(req, resp);
    }

    private BigDecimal decimal(String s) {
        try { return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return BigDecimal.valueOf(-1); }
    }
    private LocalDateTime dateTime(String s) {
        try { return s == null || s.isBlank() ? null : LocalDateTime.parse(s.trim()); }
        catch (Exception e) { return null; }
    }
    private String trim(String s) { return s == null ? null : s.trim(); }
    private String up(String s) { return s == null ? null : s.trim().toUpperCase(); }
}
