package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.common.BusinessException;
import com.cafe.model.Ingredient;
import com.cafe.service.admin.IngredientService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
/** Admin ingredient management. */
@WebServlet("/admin/ingredient")
public class IngredientServlet extends HttpServlet {

    private final IngredientService service = new IngredientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("ingredient", new Ingredient());
                forwardForm(req, resp, "Thêm nguyên liệu");
            } else if ("edit".equals(action)) {
                Ingredient i = service.getIngredient(Integer.parseInt(req.getParameter("id")));
                if (i == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("ingredient", i);
                forwardForm(req, resp, "Sửa nguyên liệu");
            } else {
                req.setAttribute("ingredients", service.getIngredientList());
                req.setAttribute("pageTitle", "Nguyên liệu");
                req.getRequestDispatcher("/WEB-INF/views/admin/ingredient-list.jsp").forward(req, resp);
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
                service.deleteIngredient(Integer.parseInt(req.getParameter("id")));
                req.getSession().setAttribute("flashOk", "Đã xoá nguyên liệu thành công.");
                resp.sendRedirect(ctx + "/admin/ingredient");
                return;
            }
            Ingredient i = bind(req);
            try {
                if (i.getIngredientId() == 0) {
                    service.createIngredient(i);
                    req.getSession().setAttribute("flashOk", "Đã thêm nguyên liệu thành công.");
                } else {
                    service.updateIngredient(i);
                    req.getSession().setAttribute("flashOk", "Đã cập nhật nguyên liệu thành công.");
                }
            } catch (BusinessException e) {
                req.setAttribute("ingredient", i);
                req.setAttribute("errorMsg", e.getMessage());
                forwardForm(req, resp, i.getIngredientId() == 0 ? "Thêm nguyên liệu" : "Sửa nguyên liệu");
                return;
            }
            resp.sendRedirect(ctx + "/admin/ingredient");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private Ingredient bind(HttpServletRequest req) {
        Ingredient i = new Ingredient();
        String id = req.getParameter("ingredientId");
        if (id != null && !id.isBlank()) i.setIngredientId(Integer.parseInt(id));
        i.setName(trim(req.getParameter("name")));
        i.setUnit(trim(req.getParameter("unit")));
        i.setIngredientType(trim(req.getParameter("ingredientType")));
        if ("PREPPED".equals(i.getIngredientType())) {
            String hours = trim(req.getParameter("shelfLifeHours"));
            if (hours != null && !hours.isBlank()) {
                try {
                    java.math.BigDecimal hoursValue = new java.math.BigDecimal(hours);
                    if (hoursValue.stripTrailingZeros().scale() > 0)
                        throw new IllegalArgumentException("Shelf life must be whole hours.");
                    java.math.BigDecimal minutes = hoursValue.multiply(java.math.BigDecimal.valueOf(60));
                    i.setShelfLifeMinutes(minutes.intValueExact());
                } catch (RuntimeException e) {
                    i.setShelfLifeMinutes(-1);
                }
            }
        }
        i.setActive(req.getParameter("active") != null);
        return i;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        req.setAttribute("supportedUnits", IngredientService.SUPPORTED_UNITS);
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/ingredient-form.jsp").forward(req, resp);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }
}
