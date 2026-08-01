package com.cafe.controller.admin;

import com.cafe.common.BusinessException;
import com.cafe.web.support.CsrfUtil;
import com.cafe.model.Ingredient;
import com.cafe.model.PrepRecipe;
import com.cafe.model.PrepRecipeIngredient;
import com.cafe.model.Product;
import com.cafe.model.ProductRecipe;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.admin.ProductService;
import com.cafe.service.admin.RecipeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Admin recipe management. */
@WebServlet("/admin/recipe")
public class RecipeServlet extends HttpServlet {

    private final RecipeService service;
    private final ProductService productService;
    private final IngredientService ingredientService;

    public RecipeServlet() { this(new RecipeService(), new ProductService(), new IngredientService()); }
    RecipeServlet(RecipeService service, ProductService productService, IngredientService ingredientService) {
        this.service = java.util.Objects.requireNonNull(service);
        this.productService = java.util.Objects.requireNonNull(productService);
        this.ingredientService = java.util.Objects.requireNonNull(ingredientService);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pid = req.getParameter("productId");
        String preppedId = req.getParameter("preppedId");
        try {
            if (pid != null && !pid.isBlank()) {
                showProductRecipe(req, resp, Integer.parseInt(pid), null);
            } else if (preppedId != null && !preppedId.isBlank()) {
                showPrepRecipe(req, resp, Integer.parseInt(preppedId), null);
            } else {
                req.setAttribute("products", productService.getProductList());
                req.setAttribute(
                        "preppedIngredients", ingredientService.getIngredientListByType("PREPPED"));
                req.setAttribute("pageTitle", "Công thức");
                req.getRequestDispatcher("/WEB-INF/views/admin/recipe-products.jsp").forward(req, resp);
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
                case "addLines": {
                    int productId = Integer.parseInt(req.getParameter("productId"));
                    try {
                        int added = service.addRecipeLines(productId, bindRecipeLines(req));
                        req.getSession().setAttribute(
                                "flashOk", "Đã thêm " + added + " nguyên liệu vào công thức.");
                    } catch (BusinessException e) {
                        showProductRecipe(req, resp, productId, e.getMessage());
                        return;
                    }
                    resp.sendRedirect(ctx + "/admin/recipe?productId=" + productId);
                    return;
                }
                case "updateLine": {
                    int productId = Integer.parseInt(req.getParameter("productId"));
                    BigDecimal qty = decimal(req.getParameter("quantity"));
                    try {
                        service.updateRecipeLine(
                                productId, Integer.parseInt(req.getParameter("lineId")), qty);
                    } catch (BusinessException e) {
                        showProductRecipe(req, resp, productId, e.getMessage());
                        return;
                    }
                    req.getSession().setAttribute("flashOk", "Đã cập nhật định mức công thức.");
                    resp.sendRedirect(ctx + "/admin/recipe?productId=" + productId);
                    return;
                }
                case "deleteLine": {
                    int productId = Integer.parseInt(req.getParameter("productId"));
                    try {
                        service.removeRecipeLine(
                                productId, Integer.parseInt(req.getParameter("lineId")));
                        req.getSession().setAttribute("flashOk", "Đã xoá nguyên liệu khỏi công thức.");
                    } catch (BusinessException e) {
                        showProductRecipe(req, resp, productId, e.getMessage());
                        return;
                    }
                    resp.sendRedirect(ctx + "/admin/recipe?productId=" + productId);
                    return;
                }
                case "addPrepLines": {
                    int preppedId = Integer.parseInt(req.getParameter("preppedId"));
                    try {
                        int added = service.addPrepRecipeLines(
                                preppedId, decimal(req.getParameter("yieldQty")), bindRecipeLines(req));
                        req.getSession().setAttribute(
                                "flashOk", "Đã thêm " + added + " nguyên liệu vào công thức pha sẵn.");
                    } catch (BusinessException e) {
                        showPrepRecipe(req, resp, preppedId, e.getMessage());
                        return;
                    }
                    resp.sendRedirect(ctx + "/admin/recipe?preppedId=" + preppedId);
                    return;
                }
                case "updatePrepYield": {
                    int preppedId = Integer.parseInt(req.getParameter("preppedId"));
                    try {
                        service.updatePrepRecipeYield(
                                preppedId, decimal(req.getParameter("yieldQty")));
                        req.getSession().setAttribute("flashOk", "Đã cập nhật sản lượng một mẻ.");
                    } catch (BusinessException e) {
                        showPrepRecipe(req, resp, preppedId, e.getMessage());
                        return;
                    }
                    resp.sendRedirect(ctx + "/admin/recipe?preppedId=" + preppedId);
                    return;
                }
                case "updatePrepLine": {
                    int preppedId = Integer.parseInt(req.getParameter("preppedId"));
                    try {
                        service.updatePrepRecipeLine(
                                preppedId,
                                Integer.parseInt(req.getParameter("lineId")),
                                decimal(req.getParameter("quantity")));
                        req.getSession().setAttribute(
                                "flashOk", "Đã cập nhật định mức công thức pha sẵn.");
                    } catch (BusinessException e) {
                        showPrepRecipe(req, resp, preppedId, e.getMessage());
                        return;
                    }
                    resp.sendRedirect(ctx + "/admin/recipe?preppedId=" + preppedId);
                    return;
                }
                case "deletePrepLine": {
                    int preppedId = Integer.parseInt(req.getParameter("preppedId"));
                    try {
                        service.removePrepRecipeLine(
                                preppedId, Integer.parseInt(req.getParameter("lineId")));
                        req.getSession().setAttribute(
                                "flashOk", "Đã xoá nguyên liệu khỏi công thức pha sẵn.");
                    } catch (BusinessException e) {
                        showPrepRecipe(req, resp, preppedId, e.getMessage());
                        return;
                    }
                    resp.sendRedirect(ctx + "/admin/recipe?preppedId=" + preppedId);
                    return;
                }
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void showProductRecipe(HttpServletRequest req, HttpServletResponse resp, int productId, String error)
            throws Exception {
        Product p = productService.getProduct(productId);
        if (p == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        List<ProductRecipe> lines = service.getProductRecipe(productId);
        Set<Integer> usedIngredientIds = new HashSet<>();
        for (ProductRecipe line : lines) usedIngredientIds.add(line.getIngredientId());
        req.setAttribute("product", p);
        req.setAttribute("lines", lines);
        req.setAttribute("ingredients", ingredientService.getActiveIngredientList().stream()
                .filter(i -> !usedIngredientIds.contains(i.getIngredientId()))
                .toList());
        if (error != null) req.setAttribute("errorMsg", error);
        req.setAttribute("pageTitle", "Công thức: " + p.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/recipe-builder.jsp").forward(req, resp);
    }

    private void showPrepRecipe(HttpServletRequest req, HttpServletResponse resp,
                                int preppedId, String error) throws Exception {
        Ingredient prepped = ingredientService.getIngredient(preppedId);
        if (prepped == null || !"PREPPED".equals(prepped.getIngredientType())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        PrepRecipe recipe = service.getPrepRecipe(preppedId);
        List<PrepRecipeIngredient> lines = recipe == null ? List.of() : recipe.getIngredients();
        Set<Integer> usedIngredientIds = new HashSet<>();
        for (PrepRecipeIngredient line : lines) usedIngredientIds.add(line.getRawIngredientId());
        req.setAttribute("prepped", prepped);
        req.setAttribute("prepRecipe", recipe);
        req.setAttribute("prepLines", lines);
        req.setAttribute("rawIngredients",
                ingredientService.getIngredientListByType("RAW").stream()
                        .filter(i -> !usedIngredientIds.contains(i.getIngredientId()))
                        .toList());
        if (error != null) req.setAttribute("errorMsg", error);
        req.setAttribute("pageTitle", "Công thức pha sẵn: " + prepped.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/prep-recipe.jsp").forward(req, resp);
    }

    private List<RecipeService.RecipeLineInput> bindRecipeLines(HttpServletRequest req) {
        String[] ingredientIds = req.getParameterValues("ingredientId");
        String[] quantities = req.getParameterValues("quantity");
        if (ingredientIds == null || quantities == null || ingredientIds.length != quantities.length) {
            throw new BusinessException("Danh sách nguyên liệu không hợp lệ.");
        }

        List<RecipeService.RecipeLineInput> inputs = new ArrayList<>();
        for (int index = 0; index < ingredientIds.length; index++) {
            int ingredientId = positiveInt(ingredientIds[index]);
            if (ingredientId <= 0) throw new BusinessException("Vui lòng chọn đầy đủ nguyên liệu.");
            inputs.add(new RecipeService.RecipeLineInput(ingredientId, decimal(quantities[index])));
        }
        return inputs;
    }

    private BigDecimal decimal(String s) {
        try {
            if (s == null || s.isBlank()) throw new NumberFormatException();
            return new BigDecimal(s.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new BusinessException("Định mức nguyên liệu không hợp lệ.");
        }
    }

    private int positiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
