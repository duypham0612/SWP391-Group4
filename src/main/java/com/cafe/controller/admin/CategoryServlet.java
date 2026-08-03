package com.cafe.controller.admin;

import com.cafe.web.support.CsrfUtil;
import com.cafe.model.Category;
import com.cafe.service.admin.CategoryService;
import com.cafe.common.BusinessException;
import com.cafe.web.form.CategoryForm;
import com.cafe.web.form.FormBindingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

/** Admin category management. */
@WebServlet("/admin/category")
public class CategoryServlet extends HttpServlet {

    private final CategoryService service;

    public CategoryServlet() {
        this(new CategoryService());
    }

    CategoryServlet(CategoryService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("category", new Category());
                forwardForm(req, resp, "Thêm danh mục");
            } else if ("edit".equals(action)) {
                Category c = service.getCategory(Integer.parseInt(req.getParameter("id")));
                if (c == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("category", c);
                forwardForm(req, resp, "Sửa danh mục");
            } else {
                req.setAttribute("categories", service.getCategoryList());
                req.setAttribute("pageTitle", "Danh mục");
                req.getRequestDispatcher("/WEB-INF/views/admin/category-list.jsp").forward(req, resp);
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
            if ("delete".equals(action)) {
                service.deleteCategory(Integer.parseInt(req.getParameter("id")));
                req.getSession().setAttribute("flashOk", "Đã xoá danh mục thành công.");
                resp.sendRedirect(ctx + "/admin/category");
                return;
            }
            Category c = CategoryForm.from(req).category();
            if (c.getCategoryId() == 0) {
                service.createCategory(c);
                req.getSession().setAttribute("flashOk", "Đã thêm danh mục thành công.");
            } else {
                service.updateCategory(c);
                req.getSession().setAttribute("flashOk", "Đã cập nhật danh mục thành công.");
            }
            resp.sendRedirect(ctx + "/admin/category");
        } catch (BusinessException | FormBindingException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(ctx + "/admin/category");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(req, resp);
    }

}
