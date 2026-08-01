package com.cafe.controller.admin;

import com.cafe.web.support.CsrfUtil;
import com.cafe.common.BusinessException;
import com.cafe.model.Branch;
import com.cafe.service.admin.BranchService;
import com.cafe.web.form.BranchForm;
import com.cafe.web.form.FormBindingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

/** Admin branch management. */
@WebServlet("/admin/branch")
public class BranchServlet extends HttpServlet {

    private final BranchService service;

    public BranchServlet() {
        this(new BranchService());
    }

    BranchServlet(BranchService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("branch", new Branch());
                forwardForm(req, resp, "Thêm chi nhánh");
            } else if ("edit".equals(action)) {
                Branch b = service.getBranch(Integer.parseInt(req.getParameter("id")));
                if (b == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("branch", b);
                forwardForm(req, resp, "Sửa chi nhánh");
            } else {
                req.setAttribute("branches", service.getBranchList());
                req.setAttribute("pageTitle", "Chi nhánh");
                req.getRequestDispatcher("/WEB-INF/views/admin/branch-list.jsp").forward(req, resp);
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
                int id = Integer.parseInt(req.getParameter("id"));
                service.toggleActive(id);
                req.getSession().setAttribute("flashOk", "Đã cập nhật trạng thái chi nhánh.");
                resp.sendRedirect(ctx + "/admin/branch");
                return;
            }
            Branch b = BranchForm.from(req).branch();
            if (b.getBranchId() == 0) {
                service.createBranch(b);
                req.getSession().setAttribute("flashOk", "Đã thêm chi nhánh thành công.");
            } else {
                service.updateBranch(b);
                req.getSession().setAttribute("flashOk", "Đã cập nhật chi nhánh thành công.");
            }
            resp.sendRedirect(ctx + "/admin/branch");
        } catch (BusinessException | FormBindingException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(ctx + "/admin/branch");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/branch-form.jsp").forward(req, resp);
    }

}
