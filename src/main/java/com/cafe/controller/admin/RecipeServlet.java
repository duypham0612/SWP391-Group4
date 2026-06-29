package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Ingredient;
import com.cafe.model.Product;
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

/**
 * A4 · RecipeServlet → /admin/recipe.
 * - ?productId=X : BOM của product (recipe-builder.jsp)
 * - ?preppedId=X : công thức pha sẵn của 1 nguyên liệu PREPPED (prep-recipe.jsp)
 * - không tham số : trang chọn (recipe-products.jsp)
 */
@WebServlet("/admin/recipe")
public class RecipeServlet extends HttpServlet {

    private final RecipeService service = new RecipeService();
    private final ProductService productService = new ProductService();
    private final IngredientService ingredientService = new IngredientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pid = req.getParameter("productId");
        String prepped = req.getParameter("preppedId");
        try {
            if (pid != null && !pid.isBlank()) {
                showProductRecipe(req, resp, Integer.parseInt(pid), null);
            } else if (prepped != null && !prepped.isBlank()) {
                showPrepRecipe(req, resp, Integer.parseInt(prepped), null);
            } else {
                req.setAttribute("products", productService.getProductList());
                req.setAttribute("preppedIngredients", ingredientService.getIngredientListByType("PREPPED"));
                req.setAttribute("pageTitle", "Công thức — chọn");
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
                case "addLine": {
                    int productId = Integer.parseInt(req.getParameter("productId"));
                    BigDecimal qty = decimal(req.getParameter("quantity"));
                    if (qty.signum() <= 0) { showProductRecipe(req, resp, productId, "Số lượng phải > 0."); return; }
                    try { service.addRecipeLine(productId, Integer.parseInt(req.getParameter("ingredientId")), qty); }
                    catch (Exception e) { showProductRecipe(req, resp, productId, "Không thêm được (nguyên liệu đã có trong công thức?)."); return; }
                    resp.sendRedirect(ctx + "/admin/recipe?productId=" + productId);
                    return;
                }
                case "updateLine": {
                    int productId = Integer.parseInt(req.getParameter("productId"));
                    BigDecimal qty = decimal(req.getParameter("quantity"));
                    if (qty.signum() <= 0) { showProductRecipe(req, resp, productId, "Số lượng phải > 0."); return; }
                    service.updateRecipeLine(Integer.parseInt(req.getParameter("lineId")), qty);
                    resp.sendRedirect(ctx + "/admin/recipe?productId=" + productId);
                    return;
                }
                case "deleteLine": {
                    int productId = Integer.parseInt(req.getParameter("productId"));
                    service.removeRecipeLine(Integer.parseInt(req.getParameter("lineId")));
                    resp.sendRedirect(ctx + "/admin/recipe?productId=" + productId);
                    return;
                }
                case "addPrep": {
                    int preppedId = Integer.parseInt(req.getParameter("preppedId"));
                    BigDecimal qty = decimal(req.getParameter("quantity"));
                    BigDecimal yield = decimal(req.getParameter("yieldQty"));
                    if (qty.signum() <= 0 || yield.signum() <= 0) { showPrepRecipe(req, resp, preppedId, "Số lượng & sản lượng phải > 0."); return; }
                    try { service.addPrepLine(preppedId, Integer.parseInt(req.getParameter("rawIngredientId")), qty, yield); }
                    catch (Exception e) { showPrepRecipe(req, resp, preppedId, "Không thêm được (nguyên liệu RAW đã có?)."); return; }
                    resp.sendRedirect(ctx + "/admin/recipe?preppedId=" + preppedId);
                    return;
                }
                case "deletePrep": {
                    int preppedId = Integer.parseInt(req.getParameter("preppedId"));
                    service.removePrepLine(Integer.parseInt(req.getParameter("prepId")));
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
        req.setAttribute("product", p);
        req.setAttribute("lines", service.getProductRecipe(productId));
        req.setAttribute("ingredients", ingredientService.getIngredientList());
        if (error != null) req.setAttribute("errorMsg", error);
        req.setAttribute("pageTitle", "Công thức: " + p.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/recipe-builder.jsp").forward(req, resp);
    }

    private void showPrepRecipe(HttpServletRequest req, HttpServletResponse resp, int preppedId, String error)
            throws Exception {
        Ingredient prepped = ingredientService.getIngredient(preppedId);
        if (prepped == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        req.setAttribute("prepped", prepped);
        req.setAttribute("prepLines", service.getPrepRecipe(preppedId));
        req.setAttribute("rawIngredients", ingredientService.getIngredientListByType("RAW"));
        if (error != null) req.setAttribute("errorMsg", error);
        req.setAttribute("pageTitle", "Công thức pha sẵn: " + prepped.getName());
        req.getRequestDispatcher("/WEB-INF/views/admin/prep-recipe.jsp").forward(req, resp);
    }

    private BigDecimal decimal(String s) {
        try { return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}
