package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Branch;
import com.cafe.service.admin.BranchService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** A2 · BranchServlet → /admin/branch. Actions: list/create/update/toggleActive. */
@WebServlet("/admin/branch")
public class BranchServlet extends HttpServlet {

    private final BranchService service = new BranchService();

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
                service.setBranchActive(Integer.parseInt(req.getParameter("id")), false);
                resp.sendRedirect(ctx + "/admin/branch");
                return;
            }
            Branch b = bind(req);
            String error = validate(b);
            if (error != null) {
                req.setAttribute("branch", b);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, b.getBranchId() == 0 ? "Thêm chi nhánh" : "Sửa chi nhánh");
                return;
            }
            if (b.getBranchId() == 0) service.createBranch(b); else service.updateBranch(b);
            resp.sendRedirect(ctx + "/admin/branch");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private Branch bind(HttpServletRequest req) {
        Branch b = new Branch();
        String id = req.getParameter("branchId");
        if (id != null && !id.isBlank()) b.setBranchId(Integer.parseInt(id));
        b.setCode(trim(req.getParameter("code")));
        b.setName(trim(req.getParameter("name")));
        b.setAddress(trim(req.getParameter("address")));
        b.setPhone(trim(req.getParameter("phone")));
        b.setActive(req.getParameter("active") != null);
        return b;
    }

    private String validate(Branch b) {
        if (b.getCode() == null || b.getCode().isBlank()) return "Mã chi nhánh không được để trống.";
        if (b.getName() == null || b.getName().isBlank()) return "Tên chi nhánh không được để trống.";
        return null;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/branch-form.jsp").forward(req, resp);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }
}
