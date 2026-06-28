package com.cafe.controller.barista;

import com.cafe.model.Product;
import com.cafe.model.ProductRecipe;
import com.cafe.service.admin.ProductService;
import com.cafe.service.shared.CatalogReadService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** B6 · RecipeLookupServlet → /barista/recipe. Tra cứu công thức + tác động modifier (read-only). */
@WebServlet("/barista/recipe")
public class RecipeLookupServlet extends HttpServlet {

    private final ProductService productService = new ProductService();
    private final CatalogReadService catalogReadService = new CatalogReadService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String q = req.getParameter("q");
            List<Product> products = productService.getProductList();
            if (q != null && !q.isBlank()) {
                String needle = q.trim().toLowerCase(Locale.ROOT);
                List<Product> filtered = new ArrayList<>();
                for (Product p : products) {
                    if (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(needle)) filtered.add(p);
                }
                products = filtered;
                req.setAttribute("q", q);
            }
            req.setAttribute("products", products);

            String pid = req.getParameter("productId");
            if (pid != null && !pid.isBlank()) {
                int productId = Integer.parseInt(pid);
                Product selected = productService.getProduct(productId);
                req.setAttribute("selected", selected);
                List<ProductRecipe> recipe = catalogReadService.getRecipeForProduct(productId);
                req.setAttribute("recipe", recipe);
                // định mức pha sẵn cho từng nguyên liệu PREPPED trong công thức
                List<CatalogReadService.OptionImpactRow> impacts =
                        catalogReadService.getModifierImpactsForProduct(productId);
                req.setAttribute("impacts", impacts);
                List<PrepSection> preps = new ArrayList<>();
                for (ProductRecipe r : recipe) {
                    if ("PREPPED".equalsIgnoreCase(r.getIngredientType())) {
                        PrepSection ps = new PrepSection();
                        ps.name = r.getIngredientName();
                        ps.lines = catalogReadService.getPrepRecipe(r.getIngredientId());
                        if (!ps.lines.isEmpty()) preps.add(ps);
                    }
                }
                req.setAttribute("preps", preps);
            }

            req.setAttribute("pageTitle", "Tra cứu công thức");
            req.getRequestDispatcher("/WEB-INF/views/barista/recipe.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/barista/recipe");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /** Nhóm định mức pha sẵn của 1 nguyên liệu PREPPED (cho view). */
    public static class PrepSection {
        public String name;
        public List<com.cafe.model.PrepRecipe> lines;
        public String getName() { return name; }
        public List<com.cafe.model.PrepRecipe> getLines() { return lines; }
    }
}
