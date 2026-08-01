package com.cafe.controller.admin;

import com.cafe.web.support.CsrfUtil;
import com.cafe.common.BusinessException;
import com.cafe.model.Voucher;
import com.cafe.service.admin.BranchService;
import com.cafe.service.shared.VoucherService;
import com.cafe.web.form.FormBindingException;
import com.cafe.web.form.VoucherForm;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

/** Admin voucher management. */
@WebServlet("/admin/voucher")
public class VoucherServlet extends HttpServlet {

    private final VoucherService service;
    private final BranchService branchService;

    public VoucherServlet() {
        this(new VoucherService(), new BranchService());
    }

    VoucherServlet(VoucherService service, BranchService branchService) {
        this.service = Objects.requireNonNull(service, "service");
        this.branchService = Objects.requireNonNull(branchService, "branchService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("voucher", new Voucher());
                forwardForm(req, resp, "Thêm voucher");
            } else if ("edit".equals(action)) {
                Voucher voucher = service.getVoucher(Integer.parseInt(req.getParameter("id")));
                if (voucher == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                req.setAttribute("voucher", voucher);
                forwardForm(req, resp, "Sửa voucher");
            } else {
                req.setAttribute("vouchers", service.getVoucherList());
                req.setAttribute("pageTitle", "Voucher");
                req.getRequestDispatcher("/WEB-INF/views/admin/voucher-list.jsp").forward(req, resp);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF");
            return;
        }

        String ctx = req.getContextPath();
        String action = req.getParameter("action");
        try {
            if ("toggleActive".equals(action)) {
                toggleActive(req);
                resp.sendRedirect(ctx + "/admin/voucher");
                return;
            }

            Voucher voucher = VoucherForm.from(req).voucher();

            if (voucher.getVoucherId() == 0) {
                service.createVoucher(voucher);
                req.getSession().setAttribute("flashOk", "Đã thêm voucher thành công.");
            } else {
                service.updateVoucher(voucher);
                req.getSession().setAttribute("flashOk", "Đã cập nhật voucher thành công.");
            }
            resp.sendRedirect(ctx + "/admin/voucher");
        } catch (BusinessException | FormBindingException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(ctx + "/admin/voucher");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void toggleActive(HttpServletRequest req) throws Exception {
        service.toggleActive(Integer.parseInt(req.getParameter("id")));
        req.getSession().setAttribute("flashOk", "Đã cập nhật trạng thái voucher.");
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        try {
            req.setAttribute("branches", branchService.getBranchListActive());
        } catch (Exception e) {
            throw new ServletException(e);
        }
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/voucher-form.jsp").forward(req, resp);
    }

}
