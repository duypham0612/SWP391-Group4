package com.cafe.controller.barista;

import com.cafe.model.Product;
import com.cafe.model.Recipe;
import com.cafe.service.shared.CatalogReadService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** B6 · RecipeLookupServlet → /barista/recipe. Tra cứu công thức + tác động modifier (read-only). */
@WebServlet("/barista/recipe")
public class RecipeLookupServlet extends HttpServlet {

    // Danh sách món nằm cạnh khung công thức nên chỉ chiếm nửa màn: 5 dòng/trang vừa hết
    // khung, barista không phải cuộn trong lúc pha. Lọc + tìm theo tên vẫn là đường chính
    // để thu hẹp danh sách, phân trang chỉ dùng khi duyệt lần lượt.
    private static final int PAGE_SIZE = 5;

    private final CatalogReadService catalogReadService;

    public RecipeLookupServlet() { this(new CatalogReadService()); }
    RecipeLookupServlet(CatalogReadService catalogReadService) {
        this.catalogReadService = java.util.Objects.requireNonNull(catalogReadService);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String q = trimToNull(req.getParameter("q"));
            Integer categoryId = parseFilter(req.getParameter("categoryId"));
            String recipeState = parseRecipeState(req.getParameter("recipeState"));
            int page = parsePage(req.getParameter("page"));
            boolean branchOnly = parseBranchOnly(req);
            Integer branchId = branchOnly ? com.cafe.web.support.BranchContext.requireBranchId(req) : null;

            CatalogReadService.ProductPage productPage = catalogReadService.getRecipeProductPage(
                    q, categoryId, recipeState, branchId, page, PAGE_SIZE);
            req.setAttribute("products", productPage.getItems());
            req.setAttribute("total", productPage.getTotal());
            req.setAttribute("page", productPage.getPage());
            req.setAttribute("totalPages", productPage.getTotalPages());
            req.setAttribute("categories", catalogReadService.getRecipeFilterCategories());
            req.setAttribute("q", q);
            req.setAttribute("fCategoryId", categoryId);
            req.setAttribute("fRecipeState", recipeState);
            req.setAttribute("fBranchOnly", branchOnly);

            String pid = req.getParameter("productId");
            if (pid != null && !pid.isBlank()) {
                Integer productId = parseFilter(pid);
                if (productId != null) {
                    Product selected = catalogReadService.getRecipeProductForLookup(
                            productId, q, categoryId, recipeState, branchId);
                    if (selected == null) {
                        // Không tiết lộ món ngoài phạm vi CN / bộ lọc qua productId đoán được.
                        req.setAttribute("recipeLookupNotice",
                                "Món được chọn không còn thuộc phạm vi tra cứu hiện tại.");
                    } else {
                        req.setAttribute("selected", selected);
                        List<Recipe> recipe = catalogReadService.getRecipeForProduct(productId);
                        req.setAttribute("recipe", recipe);
                        List<CatalogReadService.OptionImpactRow> impacts =
                                catalogReadService.getModifierImpactsForProduct(productId);
                        Set<Integer> baseIngredientIds = new HashSet<>();
                        for (Recipe r : recipe) baseIngredientIds.add(r.getIngredientId());
                        boolean hasExtraIngredient = false;
                        for (CatalogReadService.OptionImpactRow im : impacts) {
                            im.setInBaseRecipe(baseIngredientIds.contains(im.getIngredientId()));
                            if (!im.isInBaseRecipe()) hasExtraIngredient = true;
                        }
                        req.setAttribute("impacts", impacts);
                        req.setAttribute("hasExtraIngredient", hasExtraIngredient);
                        // Định mức pha sẵn cho từng nguyên liệu PREPPED trong công thức.
                        List<PrepSection> preps = new ArrayList<>();
                        for (Recipe r : recipe) {
                            if ("PREPPED".equalsIgnoreCase(r.getIngredientType())) {
                                PrepSection ps = new PrepSection();
                                ps.name = r.getIngredientName();
                                ps.unit = r.getIngredientUnit();
                                ps.lines = catalogReadService.getPrepRecipe(r.getIngredientId());
                                ps.sharedYield = r.getPrepYieldQty();
                                if (!ps.lines.isEmpty()) preps.add(ps);
                            }
                        }
                        req.setAttribute("preps", preps);
                    }
                } else {
                    req.setAttribute("recipeLookupNotice", "Mã món không hợp lệ.");
                }
            }

            req.setAttribute("pageTitle", "Tra cứu công thức");
            req.getRequestDispatcher("/WEB-INF/views/barista/recipe.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private Integer parseFilter(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parsePage(String value) {
        Integer parsed = parseFilter(value);
        return parsed == null ? 1 : parsed;
    }

    private String parseRecipeState(String value) {
        if ("HAS".equals(value) || "NONE".equals(value)) return value;
        return null;
    }

    private boolean parseBranchOnly(HttpServletRequest req) {
        if (req.getParameter("filter") == null) return true;
        return "1".equals(req.getParameter("branchOnly"));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    /** Nhóm định mức pha sẵn của 1 nguyên liệu PREPPED (cho view). */
    public static class PrepSection {
        public String name;
        public String unit;
        public List<Recipe> lines = List.of();
        public java.math.BigDecimal sharedYield;
        public String getName() { return name; }
        public String getUnit() { return unit; }
        public List<Recipe> getLines() { return lines; }

        /**
         * Sản lượng 1 mẻ để hiển thị ở đầu thẻ thay vì lặp lại trên từng dòng nguyên liệu.
         *
         * YieldQty là thuộc tính duy nhất của header nên mọi dòng nguyên liệu dùng cùng sản lượng.
         */
        public java.math.BigDecimal getSharedYield() {
            return sharedYield;
        }
    }
}
