package com.cafe.controller.admin;

import com.cafe.common.CsrfUtil;
import com.cafe.model.Product;
import com.cafe.service.admin.BranchService;
import com.cafe.service.admin.CategoryService;
import com.cafe.service.admin.ProductService;
import com.cafe.service.admin.ProductService.ProductSizeConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** Admin product management. */
@WebServlet("/admin/product")
public class ProductServlet extends HttpServlet {

    private final ProductService service = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final BranchService branchService = new BranchService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("product", new Product());
                req.setAttribute("sizeConfig", ProductSizeConfig.defaults());
                forwardForm(req, resp, "Them san pham");
            } else if ("edit".equals(action)) {
                Product p = service.getProduct(Integer.parseInt(req.getParameter("id")));
                if (p == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("product", p);
                req.setAttribute("sizeConfig", service.getSizeConfig(p.getProductId()));
                forwardForm(req, resp, "Sua san pham");
            } else {
                req.setAttribute("products", service.getProductList());
                req.setAttribute("branches", branchService.getBranchListActive());
                req.setAttribute("pageTitle", "San pham");
                req.getRequestDispatcher("/WEB-INF/views/admin/product-list.jsp").forward(req, resp);
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
            if ("toggleActive".equals(action)) {
                service.toggleActive(Integer.parseInt(req.getParameter("id")));
                resp.sendRedirect(ctx + "/admin/product");
                return;
            }
            if ("publishToBranch".equals(action)) {
                int productId = Integer.parseInt(req.getParameter("id"));
                int branchId = Integer.parseInt(req.getParameter("branchId"));
                service.publishToBranch(productId, branchId);
                resp.sendRedirect(ctx + "/admin/product");
                return;
            }
            if ("publishManyToBranch".equals(action)) {
                String[] selected = req.getParameterValues("productIds");
                int branchId = parsePositiveInt(req.getParameter("branchId"));
                if (selected == null || selected.length == 0) {
                    req.getSession().setAttribute("flashError", "Vui long chon it nhat 1 san pham.");
                    resp.sendRedirect(ctx + "/admin/product");
                    return;
                }
                if (branchId <= 0) {
                    req.getSession().setAttribute("flashError", "Vui long chon chi nhanh.");
                    resp.sendRedirect(ctx + "/admin/product");
                    return;
                }
                int[] productIds = new int[selected.length];
                int count = 0;
                for (String raw : selected) {
                    int productId = parsePositiveInt(raw);
                    if (productId > 0) productIds[count++] = productId;
                }
                if (count == 0) {
                    req.getSession().setAttribute("flashError", "Vui long chon it nhat 1 san pham hop le.");
                    resp.sendRedirect(ctx + "/admin/product");
                    return;
                }
                if (count < productIds.length) {
                    int[] trimmed = new int[count];
                    System.arraycopy(productIds, 0, trimmed, 0, count);
                    productIds = trimmed;
                }
                service.publishManyToBranch(productIds, branchId);
                req.getSession().setAttribute("flashOk", "Da them " + productIds.length + " san pham vao chi nhanh.");
                resp.sendRedirect(ctx + "/admin/product");
                return;
            }
            Product p = bind(req);
            ProductSizeConfig sizeConfig = bindSizeConfig(req);
            String error = validate(p);
            if (error == null) error = validateSizeConfig(sizeConfig);
            if (error != null) {
                req.setAttribute("product", p);
                req.setAttribute("sizeConfig", sizeConfig);
                req.setAttribute("errorMsg", error);
                forwardForm(req, resp, p.getProductId() == 0 ? "Them san pham" : "Sua san pham");
                return;
            }
            if (p.getProductId() == 0) service.createProduct(p, sizeConfig); else service.updateProduct(p, sizeConfig);
            resp.sendRedirect(ctx + "/admin/product");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private Product bind(HttpServletRequest req) {
        Product p = new Product();
        String id = req.getParameter("productId");
        if (id != null && !id.isBlank()) p.setProductId(Integer.parseInt(id));
        p.setCategoryId(parsePositiveInt(req.getParameter("categoryId")));
        p.setName(trim(req.getParameter("name")));
        String price = req.getParameter("basePrice");
        try { p.setBasePrice(price == null || price.isBlank() ? BigDecimal.ZERO : new BigDecimal(price.trim())); }
        catch (NumberFormatException e) { p.setBasePrice(BigDecimal.valueOf(-1)); }
        p.setImageUrl(trim(req.getParameter("imageUrl")));
        p.setActive(req.getParameter("active") != null);
        String prep = req.getParameter("prepMinutes");
        try { p.setPrepSeconds(prep == null || prep.isBlank() ? 720 : Integer.parseInt(prep.trim()) * 60); }
        catch (NumberFormatException e) { p.setPrepSeconds(-1); }
        return p;
    }

    private ProductSizeConfig bindSizeConfig(HttpServletRequest req) {
        ProductSizeConfig cfg = ProductSizeConfig.defaults();
        cfg.setSizeMDelta(parseMoney(req.getParameter("sizeMDelta")));
        cfg.setSizeLDelta(parseMoney(req.getParameter("sizeLDelta")));
        return cfg;
    }

    private String validate(Product p) {
        if (p.getName() == null || p.getName().isBlank()) return "Ten san pham khong duoc de trong.";
        if (p.getCategoryId() <= 0) return "Vui long chon danh muc.";
        if (p.getBasePrice() == null || p.getBasePrice().signum() < 0) return "Gia phai la so >= 0.";
        if (p.getPrepSeconds() < 60) return "Thoi gian pha chuan phai la so phut >= 1.";
        return null;
    }

    private String validateSizeConfig(ProductSizeConfig cfg) {
        if (cfg.getSizeMDelta().signum() < 0 || cfg.getSizeLDelta().signum() < 0) return "Gia tang size phai >= 0.";
        return null;
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        try { req.setAttribute("categories", categoryService.getCategoryList()); }
        catch (Exception e) { throw new ServletException(e); }
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(req, resp);
    }

    private String trim(String s) { return s == null ? null : s.trim(); }

    private BigDecimal parseMoney(String raw) {
        try {
            if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
            return new BigDecimal(raw.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(-1);
        }
    }

    private int parsePositiveInt(String raw) {
        try {
            if (raw == null || raw.isBlank()) return 0;
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
