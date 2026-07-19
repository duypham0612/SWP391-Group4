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
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * A4 · ModifierServlet → /admin/modifier.
 * view = list (tổng quan) | group (workspace gộp: cấu hình + option + định mức) | assign(productId)
 */
@WebServlet("/admin/modifier")
public class ModifierServlet extends HttpServlet {

    private final ModifierService service = new ModifierService();
    private final IngredientService ingredientService = new IngredientService();
    private final ProductService productService = new ProductService();

    private boolean modifierAdminDisabled() {
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (modifierAdminDisabled()) {
            req.getSession().setAttribute("flashOk", "T\u00f9y ch\u1ecdn m\u00f3n \u0111\u00e3 chuy\u1ec3n sang 3 nh\u00f3m c\u1ed1 \u0111\u1ecbnh: Size, \u0110\u01b0\u1eddng, \u0110\u00e1.");
            resp.sendRedirect(req.getContextPath() + "/admin/product");
            return;
        }
        String view = req.getParameter("view");
        try {
            consumeFlash(req);
            if ("group".equals(view)) {
                showWorkspace(req, resp);
            } else if ("assign".equals(view)) {
                showAssign(req, resp, Integer.parseInt(req.getParameter("productId")));
            } else {
                req.setAttribute("groups", service.getModifierGroups());
                req.setAttribute("pageTitle", "Tuỳ chọn (Modifier)");
                req.getRequestDispatcher("/WEB-INF/views/admin/modifier-list.jsp").forward(req, resp);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (modifierAdminDisabled()) {
            req.getSession().setAttribute("flashOk", "T\u00f9y ch\u1ecdn m\u00f3n \u0111\u00e3 chuy\u1ec3n sang 3 nh\u00f3m c\u1ed1 \u0111\u1ecbnh: Size, \u0110\u01b0\u1eddng, \u0110\u00e1.");
            resp.sendRedirect(req.getContextPath() + "/admin/product");
            return;
        }
        if (!CsrfUtil.isValid(req)) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF"); return; }
        String ctx = req.getContextPath();
        String action = req.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "saveGroup":    saveGroup(req, resp, ctx); return;
                case "deleteGroup": {
                    service.deleteModifierGroup(Integer.parseInt(req.getParameter("groupId")));
                    flashOk(req, "Đã xoá nhóm modifier.");
                    resp.sendRedirect(ctx + "/admin/modifier");
                    return;
                }
                case "addOption":    addOption(req, resp, ctx); return;
                case "updateOption": updateOption(req, resp, ctx); return;
                case "deleteOption": {
                    int groupId = Integer.parseInt(req.getParameter("groupId"));
                    service.deleteModifierOption(Integer.parseInt(req.getParameter("optionId")));
                    flashOk(req, "Đã xoá option.");
                    resp.sendRedirect(workspace(ctx, groupId, null));
                    return;
                }
                case "addImpact":    addImpact(req, resp, ctx); return;
                case "deleteImpact": {
                    int groupId = Integer.parseInt(req.getParameter("groupId"));
                    int optionId = Integer.parseInt(req.getParameter("optionId"));
                    service.deleteModifierImpact(Integer.parseInt(req.getParameter("impactId")));
                    resp.sendRedirect(workspace(ctx, groupId, optionId));
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

    // ---------- GET views ----------

    /** Workspace 1 nhóm: cấu hình + option + định mức inline (groupId rỗng/0 = tạo mới). */
    private void showWorkspace(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String idParam = req.getParameter("groupId");
        int groupId = (idParam == null || idParam.isBlank()) ? 0 : Integer.parseInt(idParam);
        ModifierGroup g;
        if (groupId == 0) {
            g = new ModifierGroup();               // tạo mới: chỉ hiện card cấu hình
        } else {
            g = service.getModifierGroup(groupId);
            if (g == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            req.setAttribute("options", service.getModifierOptions(groupId));
            req.setAttribute("impactsByOption", service.getImpactsByOptionMap(groupId));
            req.setAttribute("ingredients", ingredientService.getIngredientList());
        }
        req.setAttribute("group", g);
        req.setAttribute("pageTitle", groupId == 0 ? "Thêm nhóm modifier" : "Nhóm: " + g.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/modifier-group.jsp").forward(req, resp);
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

    // ---------- POST handlers ----------

    private void saveGroup(HttpServletRequest req, HttpServletResponse resp, String ctx) throws Exception {
        ModifierGroup g = bindGroup(req);
        String error = validateGroup(g);
        if (error != null) {
            flashError(req, error);
            resp.sendRedirect(g.getModifierGroupId() == 0
                    ? ctx + "/admin/modifier?view=group"
                    : workspace(ctx, g.getModifierGroupId(), null));
            return;
        }
        if (g.getModifierGroupId() == 0) {
            int newId = service.createModifierGroup(g);
            flashOk(req, "Đã tạo nhóm. Thêm các option bên dưới.");
            resp.sendRedirect(workspace(ctx, newId, null));
        } else {
            service.updateModifierGroup(g);
            flashOk(req, "Đã lưu cấu hình nhóm.");
            resp.sendRedirect(workspace(ctx, g.getModifierGroupId(), null));
        }
    }

    private void addOption(HttpServletRequest req, HttpServletResponse resp, String ctx) throws Exception {
        int groupId = Integer.parseInt(req.getParameter("groupId"));
        String name = trim(req.getParameter("name"));
        if (name == null || name.isBlank()) {
            flashError(req, "Tên option không được để trống.");
            resp.sendRedirect(workspace(ctx, groupId, null));
            return;
        }
        ModifierOption o = new ModifierOption();
        o.setModifierGroupId(groupId);
        o.setName(name);
        o.setPriceDelta(decimal(req.getParameter("priceDelta")));
        o.setActive(true);
        service.createModifierOption(o);
        flashOk(req, "Đã thêm option \"" + name + "\".");
        resp.sendRedirect(workspace(ctx, groupId, null));
    }

    private void updateOption(HttpServletRequest req, HttpServletResponse resp, String ctx) throws Exception {
        int groupId = Integer.parseInt(req.getParameter("groupId"));
        int optionId = Integer.parseInt(req.getParameter("optionId"));
        String name = trim(req.getParameter("name"));
        if (name == null || name.isBlank()) {
            flashError(req, "Tên option không được để trống.");
            resp.sendRedirect(workspace(ctx, groupId, optionId));
            return;
        }
        ModifierOption o = new ModifierOption();
        o.setModifierOptionId(optionId);
        o.setModifierGroupId(groupId);
        o.setName(name);
        o.setPriceDelta(decimal(req.getParameter("priceDelta")));
        o.setActive(req.getParameter("active") != null);
        service.updateModifierOption(o);
        flashOk(req, "Đã lưu option.");
        resp.sendRedirect(workspace(ctx, groupId, optionId));
    }

    private void addImpact(HttpServletRequest req, HttpServletResponse resp, String ctx) throws Exception {
        int groupId = Integer.parseInt(req.getParameter("groupId"));
        int optionId = Integer.parseInt(req.getParameter("optionId"));
        String ingParam = req.getParameter("ingredientId");
        if (ingParam == null || ingParam.isBlank()) {
            flashError(req, "Hãy chọn nguyên liệu.");
            resp.sendRedirect(workspace(ctx, groupId, optionId));
            return;
        }
        BigDecimal qty = decimal(req.getParameter("qtyDelta"));
        if (qty.signum() == 0) {
            flashError(req, "Lượng thay đổi phải khác 0 (dương = thêm, âm = bớt).");
            resp.sendRedirect(workspace(ctx, groupId, optionId));
            return;
        }
        boolean added = service.saveModifierImpact(optionId, Integer.parseInt(ingParam), qty);
        if (!added) flashError(req, "Nguyên liệu này đã có trong option — hãy sửa hoặc xoá dòng cũ.");
        else flashOk(req, "Đã thêm định mức nguyên liệu.");
        resp.sendRedirect(workspace(ctx, groupId, optionId));
    }

    // ---------- helpers ----------

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

    private String validateGroup(ModifierGroup g) {
        if (g.getName() == null || g.getName().isBlank()) return "Tên nhóm không được để trống.";
        if (g.getMaxSelect() < 1) return "Số lượng chọn tối đa phải ≥ 1.";
        if (g.getMinSelect() < 0) return "Số lượng chọn tối thiểu không được âm.";
        if (g.getMinSelect() > g.getMaxSelect()) return "Số chọn tối thiểu không được lớn hơn tối đa.";
        if (g.isRequired() && g.getMinSelect() < 1) return "Nhóm bắt buộc phải cho chọn tối thiểu ≥ 1.";
        return null;
    }

    /** URL workspace của 1 nhóm, kèm anchor #opt-{id} để cuộn về đúng option sau POST. */
    private String workspace(String ctx, int groupId, Integer optionId) {
        String url = ctx + "/admin/modifier?view=group&groupId=" + groupId;
        return optionId == null ? url : url + "#opt-" + optionId;
    }

    private void flashOk(HttpServletRequest req, String msg) { req.getSession().setAttribute("flashOk", msg); }
    private void flashError(HttpServletRequest req, String msg) { req.getSession().setAttribute("flashError", msg); }
    private void consumeFlash(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return;
        Object ok = s.getAttribute("flashOk");
        if (ok != null) { req.setAttribute("flashOk", ok); s.removeAttribute("flashOk"); }
        Object er = s.getAttribute("flashError");
        if (er != null) { req.setAttribute("flashError", er); s.removeAttribute("flashError"); }
    }

    private int intval(String s, int def) {
        try { return s == null || s.isBlank() ? def : Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
    private BigDecimal decimal(String s) {
        try { return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
    private String trim(String s) { return s == null ? null : s.trim(); }
}
