package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Category;
import com.cafe.service.admin.CategoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Admin category management. */
@WebServlet("/admin/category")
public class CategoryServlet extends HttpServlet {

    private final CategoryService service = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("category", new Category());
                forwardForm(req, resp, "Them danh muc");
            } else if ("edit".equals(action)) {
                Category c = service.getCategory(Integer.parseInt(req.getParameter("id")));
                if (c == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("category", c);
                forwardForm(req, resp, "Sua danh muc");
            } else {
                req.setAttribute("categories", service.getCategoryList());
                req.setAttribute("pageTitle", "Danh muc");
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
                resp.sendRedirect(ctx + "/admin/category");
                return;
            }
            Category c = bind(req);
            String error = validate(c);
            if (error != null) {
                req.setAttribute("category", c);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, c.getCategoryId() == 0 ? "Them danh muc" : "Sua danh muc");
                return;
            }
            if (c.getCategoryId() == 0) service.createCategory(c); else service.updateCategory(c);
            resp.sendRedirect(ctx + "/admin/category");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private Category bind(HttpServletRequest req) {
        Category c = new Category();
        String id = req.getParameter("categoryId");
        if (id != null && !id.isBlank()) c.setCategoryId(Integer.parseInt(id));
        c.setName(trim(req.getParameter("name")));
        c.setSortOrder(parseNonNegativeInt(req.getParameter("sortOrder"), -1));
        c.setActive(req.getParameter("active") != null);
        return c;
    }

    private String validate(Category c) {
        if (c.getName() == null || c.getName().isBlank()) return "Ten danh muc khong duoc de trong.";
        if (c.getName().length() > 100) return "Ten danh muc toi da 100 ky tu.";
        if (c.getSortOrder() < 0) return "Thu tu phai >= 0.";
        return null;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/category-form.jsp").forward(req, resp);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }

    private int parseNonNegativeInt(String raw, int fallback) {
        try {
            if (raw == null || raw.isBlank()) return 0;
            int value = Integer.parseInt(raw.trim());
            return value >= 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
