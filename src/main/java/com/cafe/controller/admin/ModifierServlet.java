package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.ModifierGroup;
import com.cafe.model.ModifierOption;
import com.cafe.model.Product;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.admin.ModifierService;
import com.cafe.service.admin.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * A4 · ModifierServlet → /admin/modifier.
 * view = list | groupForm | options | impacts | assign(productId)
 */
@WebServlet("/admin/modifier")
public class ModifierServlet extends HttpServlet {

    private final ModifierService service = new ModifierService();
    private final IngredientService ingredientService = new IngredientService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String view = req.getParameter("view");
        try {
            if ("groupForm".equals(view)) {
                String id = req.getParameter("groupId");
                ModifierGroup g = (id == null || id.isBlank()) ? new ModifierGroup() : service.getModifierGroup(Integer.parseInt(id));
                if (g == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("group", g);
                req.setAttribute("pageTitle", g.getModifierGroupId() == 0 ? "Thêm nhóm modifier" : "Sửa nhóm modifier");
                req.getRequestDispatcher("/WEB-INF/views/admin/modifier-form.jsp").forward(req, resp);
            } else if ("options".equals(view)) {
                showOptions(req, resp, Integer.parseInt(req.getParameter("groupId")));
            } else if ("impacts".equals(view)) {
                showImpacts(req, resp, Integer.parseInt(req.getParameter("optionId")));
            } else if ("assign".equals(view)) {
                showAssign(req, resp, Integer.parseInt(req.getParameter("productId")));
            } else {
                req.setAttribute("groups", service.getModifierGroups());
                req.setAttribute("pageTitle", "Modifier");
                req.getRequestDispatcher("/WEB-INF/views/admin/modifier-list.jsp").forward(req, resp);
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
            switch (action == null ? "" : action) {
                case "saveGroup": {
                    ModifierGroup g = bindGroup(req);
                    if (g.getName() == null || g.getName().isBlank()) {
                        req.setAttribute("group", g);
                        req.setAttribute("errorMsg", "Tên nhóm không được để trống.");
                        req.setAttribute("pageTitle", "Nhóm modifier");
                        req.getRequestDispatcher("/WEB-INF/views/admin/modifier-form.jsp").forward(req, resp);
                        return;
                    }
                    if (g.getModifierGroupId() == 0) service.createModifierGroup(g); else service.updateModifierGroup(g);
                    resp.sendRedirect(ctx + "/admin/modifier");
                    return;
                }
                case "addOption": {
                    int groupId = Integer.parseInt(req.getParameter("groupId"));
                    ModifierOption o = new ModifierOption();
                    o.setModifierGroupId(groupId);
                    o.setName(trim(req.getParameter("name")));
                    o.setPriceDelta(decimal(req.getParameter("priceDelta")));
                    o.setActive(true);
                    if (o.getName() != null && !o.getName().isBlank()) service.createModifierOption(o);
                    resp.sendRedirect(ctx + "/admin/modifier?view=options&groupId=" + groupId);
                    return;
                }
                case "deleteOption": {
                    int groupId = Integer.parseInt(req.getParameter("groupId"));
                    service.deleteModifierOption(Integer.parseInt(req.getParameter("optionId")));
                    resp.sendRedirect(ctx + "/admin/modifier?view=options&groupId=" + groupId);
                    return;
                }
                case "addImpact": {
                    int optionId = Integer.parseInt(req.getParameter("optionId"));
                    try { service.saveModifierImpact(optionId, Integer.parseInt(req.getParameter("ingredientId")), decimal(req.getParameter("qtyDelta"))); }
                    catch (Exception ignored) { /* trùng UQ */ }
                    resp.sendRedirect(ctx + "/admin/modifier?view=impacts&optionId=" + optionId);
                    return;
                }
                case "deleteImpact": {
                    int optionId = Integer.parseInt(req.getParameter("optionId"));
                    service.deleteModifierImpact(Integer.parseInt(req.getParameter("impactId")));
                    resp.sendRedirect(ctx + "/admin/modifier?view=impacts&optionId=" + optionId);
                    return;
                }
                case "assignGroup": {
                    int productId = Integer.parseInt(req.getParameter("productId"));
                    service.assignGroupToProduct(productId, Integer.parseInt(req.getParameter("groupId")));
                    resp.sendRedirect(ctx + "/admin/modifier?view=assign&productId=" + productId);
                    return;
                }
                case "unassignGroup": {
                    int productId = Integer.parseInt(req.getParameter("productId"));
                    service.unassignGroupFromProduct(productId, Integer.parseInt(req.getParameter("groupId")));
                    resp.sendRedirect(ctx + "/admin/modifier?view=assign&productId=" + productId);
                    return;
                }
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void showOptions(HttpServletRequest req, HttpServletResponse resp, int groupId) throws Exception {
        ModifierGroup g = service.getModifierGroup(groupId);
        if (g == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        req.setAttribute("group", g);
        req.setAttribute("options", service.getModifierOptions(groupId));
        req.setAttribute("pageTitle", "Option: " + g.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/modifier-options.jsp").forward(req, resp);
    }

    private void showImpacts(HttpServletRequest req, HttpServletResponse resp, int optionId) throws Exception {
        ModifierOption o = service.getModifierOption(optionId);
        if (o == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        req.setAttribute("option", o);
        req.setAttribute("impacts", service.getModifierImpacts(optionId));
        req.setAttribute("ingredients", ingredientService.getIngredientList());
        req.setAttribute("pageTitle", "Định mức theo option: " + o.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/modifier-impacts.jsp").forward(req, resp);
    }

    private void showAssign(HttpServletRequest req, HttpServletResponse resp, int productId) throws Exception {
        Product p = productService.getProduct(productId);
        if (p == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        req.setAttribute("product", p);
        req.setAttribute("assigned", service.getProductGroups(productId));
        req.setAttribute("allGroups", service.getModifierGroups());
        req.setAttribute("pageTitle", "Gán modifier: " + p.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/modifier-assign.jsp").forward(req, resp);
    }

    private ModifierGroup bindGroup(HttpServletRequest req) {
        ModifierGroup g = new ModifierGroup();
        String id = req.getParameter("modifierGroupId");
        if (id != null && !id.isBlank()) g.setModifierGroupId(Integer.parseInt(id));
        g.setName(trim(req.getParameter("name")));
        g.setRequired(req.getParameter("required") != null);
        g.setMinSelect(intval(req.getParameter("minSelect"), 0));
        g.setMaxSelect(intval(req.getParameter("maxSelect"), 1));
        return g;
    }

    private int intval(String s, int def) {
        try { return s == null || s.isBlank() ? def : Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
    private BigDecimal decimal(String s) {
        try { return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
    private String trim(String s) { return s == null ? null : s.trim(); }
}
