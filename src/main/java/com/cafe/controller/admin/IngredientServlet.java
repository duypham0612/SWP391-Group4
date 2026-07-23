package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Ingredient;
import com.cafe.service.admin.IngredientService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/** Admin ingredient management. */
@WebServlet("/admin/ingredient")
public class IngredientServlet extends HttpServlet {

    private static final Set<String> TYPES = Set.of("RAW", "PREPPED");
    private final IngredientService service = new IngredientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("ingredient", new Ingredient());
                forwardForm(req, resp, "Them nguyen lieu");
            } else if ("edit".equals(action)) {
                Ingredient i = service.getIngredient(Integer.parseInt(req.getParameter("id")));
                if (i == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("ingredient", i);
                forwardForm(req, resp, "Sua nguyen lieu");
            } else {
                req.setAttribute("ingredients", service.getIngredientList());
                req.setAttribute("pageTitle", "Nguyen lieu");
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
                resp.sendRedirect(ctx + "/admin/ingredient");
                return;
            }
            Ingredient i = bind(req);
            String error = validate(i);
            if (error != null) {
                req.setAttribute("ingredient", i);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, i.getIngredientId() == 0 ? "Them nguyen lieu" : "Sua nguyen lieu");
                return;
            }
            if (i.getIngredientId() == 0) service.createIngredient(i); else service.updateIngredient(i);
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
        i.setActive(req.getParameter("active") != null);
        return i;
    }

    private String validate(Ingredient i) {
        if (i.getName() == null || i.getName().isBlank()) return "Ten nguyen lieu khong duoc de trong.";
        if (i.getUnit() == null || i.getUnit().isBlank()) return "Don vi khong duoc de trong.";
        if (i.getIngredientType() == null || !TYPES.contains(i.getIngredientType()))
            return "Loai nguyen lieu phai la RAW hoac PREPPED.";
        return null;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/ingredient-form.jsp").forward(req, resp);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }
}
