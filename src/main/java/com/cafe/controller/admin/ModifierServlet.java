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
 * Admin screen for drink choices. The underlying tables keep the historical
 * modifier names, but the business UI is limited to Size, Duong, and Da.
 */
@WebServlet("/admin/modifier")
public class ModifierServlet extends HttpServlet {

    private final ModifierService service = new ModifierService();
    private final IngredientService ingredientService = new IngredientService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!legacyModifierUiEnabled()) {
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
                req.setAttribute("groups", service.getChoiceGroups());
                req.setAttribute("pageTitle", "Tuy chon Size - Duong - Da");
                req.getRequestDispatcher("/WEB-INF/views/admin/modifier-list.jsp").forward(req, resp);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!legacyModifierUiEnabled()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!CsrfUtil.isValid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF");
            return;
        }
        String ctx = req.getContextPath();
        String action = req.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "saveGroup":
                    saveGroup(req, resp, ctx);
                    return;
                case "deleteGroup": {
                    service.deleteModifierGroup(Integer.parseInt(req.getParameter("groupId")));
                    flashOk(req, "Da xoa nhom tuy chon.");
                    resp.sendRedirect(ctx + "/admin/modifier");
                    return;
                }
                case "addOption":
                    addOption(req, resp, ctx);
                    return;
                case "updateOption":
                    updateOption(req, resp, ctx);
                    return;
                case "deleteOption": {
                    int groupId = Integer.parseInt(req.getParameter("groupId"));
                    service.deleteModifierOption(Integer.parseInt(req.getParameter("optionId")));
                    flashOk(req, "Da xoa muc chon.");
                    resp.sendRedirect(workspace(ctx, groupId, null));
                    return;
                }
                case "addImpact":
                    addImpact(req, resp, ctx);
                    return;
                case "deleteImpact": {
                    int groupId = Integer.parseInt(req.getParameter("groupId"));
                    int optionId = Integer.parseInt(req.getParameter("optionId"));
                    service.deleteModifierImpact(Integer.parseInt(req.getParameter("impactId")));
                    resp.sendRedirect(workspace(ctx, groupId, optionId));
                    return;
                }
                case "assignGroup": {
                    int productId = Integer.parseInt(req.getParameter("productId"));
                    int groupId = Integer.parseInt(req.getParameter("groupId"));
                    ModifierGroup group = service.getModifierGroup(groupId);
                    if (group == null || !service.isChoiceGroup(group.getName())) {
                        flashError(req, "Chi duoc gan nhom Size, Duong hoac Da.");
                    } else {
                        service.assignGroupToProduct(productId, groupId);
                    }
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
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private boolean legacyModifierUiEnabled() {
        return false;
    }

    private void showWorkspace(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String idParam = req.getParameter("groupId");
        int groupId = (idParam == null || idParam.isBlank()) ? 0 : Integer.parseInt(idParam);
        ModifierGroup group;
        if (groupId == 0) {
            group = new ModifierGroup();
        } else {
            group = service.getModifierGroup(groupId);
            if (group == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!service.isChoiceGroup(group.getName())) {
                flashError(req, "Nhom nay khong thuoc Size, Duong, Da.");
                resp.sendRedirect(req.getContextPath() + "/admin/modifier");
                return;
            }
            req.setAttribute("options", service.getModifierOptions(groupId));
            req.setAttribute("impactsByOption", service.getImpactsByOptionMap(groupId));
            req.setAttribute("ingredients", ingredientService.getIngredientList());
        }
        req.setAttribute("group", group);
        req.setAttribute("pageTitle", groupId == 0 ? "Them nhom tuy chon" : "Nhom: " + group.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/modifier-group.jsp").forward(req, resp);
    }

    private void showAssign(HttpServletRequest req, HttpServletResponse resp, int productId) throws Exception {
        Product product = productService.getProduct(productId);
        if (product == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("product", product);
        req.setAttribute("assigned", service.getProductChoiceGroups(productId));
        req.setAttribute("allGroups", service.getChoiceGroups());
        req.setAttribute("pageTitle", "Gan tuy chon: " + product.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/modifier-assign.jsp").forward(req, resp);
    }

    private void saveGroup(HttpServletRequest req, HttpServletResponse resp, String ctx) throws Exception {
        ModifierGroup group = bindGroup(req);
        group.setName(canonicalGroupName(group.getName()));
        String error = validateGroup(group);
        if (error != null) {
            flashError(req, error);
            resp.sendRedirect(group.getModifierGroupId() == 0
                    ? ctx + "/admin/modifier?view=group"
                    : workspace(ctx, group.getModifierGroupId(), null));
            return;
        }
        if (group.getModifierGroupId() == 0) {
            int newId = service.createModifierGroup(group);
            flashOk(req, "Da tao nhom. Hay them cac muc chon ben duoi.");
            resp.sendRedirect(workspace(ctx, newId, null));
        } else {
            service.updateModifierGroup(group);
            flashOk(req, "Da luu cau hinh nhom.");
            resp.sendRedirect(workspace(ctx, group.getModifierGroupId(), null));
        }
    }

    private void addOption(HttpServletRequest req, HttpServletResponse resp, String ctx) throws Exception {
        int groupId = Integer.parseInt(req.getParameter("groupId"));
        String name = trim(req.getParameter("name"));
        if (name == null || name.isBlank()) {
            flashError(req, "Ten muc chon khong duoc de trong.");
            resp.sendRedirect(workspace(ctx, groupId, null));
            return;
        }
        ModifierOption option = new ModifierOption();
        option.setModifierGroupId(groupId);
        option.setName(name);
        option.setPriceDelta(decimal(req.getParameter("priceDelta")));
        option.setActive(true);
        service.createModifierOption(option);
        flashOk(req, "Da them muc chon \"" + name + "\".");
        resp.sendRedirect(workspace(ctx, groupId, null));
    }

    private void updateOption(HttpServletRequest req, HttpServletResponse resp, String ctx) throws Exception {
        int groupId = Integer.parseInt(req.getParameter("groupId"));
        int optionId = Integer.parseInt(req.getParameter("optionId"));
        String name = trim(req.getParameter("name"));
        if (name == null || name.isBlank()) {
            flashError(req, "Ten muc chon khong duoc de trong.");
            resp.sendRedirect(workspace(ctx, groupId, optionId));
            return;
        }
        ModifierOption option = new ModifierOption();
        option.setModifierOptionId(optionId);
        option.setModifierGroupId(groupId);
        option.setName(name);
        option.setPriceDelta(decimal(req.getParameter("priceDelta")));
        option.setActive(req.getParameter("active") != null);
        service.updateModifierOption(option);
        flashOk(req, "Da luu muc chon.");
        resp.sendRedirect(workspace(ctx, groupId, optionId));
    }

    private void addImpact(HttpServletRequest req, HttpServletResponse resp, String ctx) throws Exception {
        int groupId = Integer.parseInt(req.getParameter("groupId"));
        int optionId = Integer.parseInt(req.getParameter("optionId"));
        String ingredientParam = req.getParameter("ingredientId");
        if (ingredientParam == null || ingredientParam.isBlank()) {
            flashError(req, "Hay chon nguyen lieu.");
            resp.sendRedirect(workspace(ctx, groupId, optionId));
            return;
        }
        BigDecimal qty = decimal(req.getParameter("qtyDelta"));
        if (qty.signum() == 0) {
            flashError(req, "Luong thay doi phai khac 0.");
            resp.sendRedirect(workspace(ctx, groupId, optionId));
            return;
        }
        boolean added = service.saveModifierImpact(optionId, Integer.parseInt(ingredientParam), qty);
        if (!added) flashError(req, "Nguyen lieu nay da co trong muc chon.");
        else flashOk(req, "Da them dinh muc nguyen lieu.");
        resp.sendRedirect(workspace(ctx, groupId, optionId));
    }

    private ModifierGroup bindGroup(HttpServletRequest req) {
        ModifierGroup group = new ModifierGroup();
        String id = req.getParameter("modifierGroupId");
        if (id != null && !id.isBlank()) group.setModifierGroupId(Integer.parseInt(id));
        group.setName(trim(req.getParameter("name")));
        group.setRequired(req.getParameter("required") != null);
        group.setMinSelect(intval(req.getParameter("minSelect"), 0));
        group.setMaxSelect(intval(req.getParameter("maxSelect"), 1));
        return group;
    }

    private String validateGroup(ModifierGroup group) {
        if (group.getName() == null || group.getName().isBlank()) return "Ten nhom khong duoc de trong.";
        if (!service.isChoiceGroup(group.getName())) return "Chi duoc cau hinh 3 nhom: Size, Duong, Da.";
        if (group.getMaxSelect() < 1) return "So luong chon toi da phai >= 1.";
        if (group.getMinSelect() < 0) return "So luong chon toi thieu khong duoc am.";
        if (group.getMinSelect() > group.getMaxSelect()) return "So chon toi thieu khong duoc lon hon toi da.";
        if (group.isRequired() && group.getMinSelect() < 1) return "Nhom bat buoc phai cho chon toi thieu >= 1.";
        return null;
    }

    private String workspace(String ctx, int groupId, Integer optionId) {
        String url = ctx + "/admin/modifier?view=group&groupId=" + groupId;
        return optionId == null ? url : url + "#opt-" + optionId;
    }

    private void flashOk(HttpServletRequest req, String msg) { req.getSession().setAttribute("flashOk", msg); }
    private void flashError(HttpServletRequest req, String msg) { req.getSession().setAttribute("flashError", msg); }

    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        Object ok = session.getAttribute("flashOk");
        if (ok != null) {
            req.setAttribute("flashOk", ok);
            session.removeAttribute("flashOk");
        }
        Object error = session.getAttribute("flashError");
        if (error != null) {
            req.setAttribute("flashError", error);
            session.removeAttribute("flashError");
        }
    }

    private int intval(String s, int def) {
        try { return s == null || s.isBlank() ? def : Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private BigDecimal decimal(String s) {
        try { return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private String trim(String s) { return s == null ? null : s.trim(); }

    private String canonicalGroupName(String name) {
        String n = trim(name);
        if (n == null) return null;
        String normalized = n.toLowerCase().replace("\u0111", "d").replaceAll("\\s+", "");
        if ("size".equals(normalized)) return "Size";
        if ("duong".equals(normalized)) return "\u0110\u01b0\u1eddng";
        if ("da".equals(normalized)) return "\u0110\u00e1";
        return n;
    }
}
