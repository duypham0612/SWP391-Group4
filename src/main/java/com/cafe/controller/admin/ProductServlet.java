package com.cafe.controller.admin;

import com.cafe.web.support.CsrfUtil;
import com.cafe.model.Product;
import com.cafe.service.admin.BranchService;
import com.cafe.service.admin.CategoryService;
import com.cafe.service.admin.ProductService;
import com.cafe.service.admin.ProductService.ProductSizeConfig;
import com.cafe.common.BusinessException;
import com.cafe.web.form.FormBindingException;
import com.cafe.web.form.ProductForm;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

/** Admin product management. */
@WebServlet("/admin/product")
public class ProductServlet extends HttpServlet {

    private final ProductService service;
    private final CategoryService categoryService;
    private final BranchService branchService;

    public ProductServlet() {
        this(new ProductService(), new CategoryService(), new BranchService());
    }

    ProductServlet(ProductService service, CategoryService categoryService, BranchService branchService) {
        this.service = Objects.requireNonNull(service, "service");
        this.categoryService = Objects.requireNonNull(categoryService, "categoryService");
        this.branchService = Objects.requireNonNull(branchService, "branchService");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("new".equals(action)) {
                req.setAttribute("product", new Product());
                req.setAttribute("sizeConfig", ProductSizeConfig.defaults());
                forwardForm(req, resp, "Thêm sản phẩm");
            } else if ("edit".equals(action)) {
                Product p = service.getProduct(Integer.parseInt(req.getParameter("id")));
                if (p == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("product", p);
                req.setAttribute("sizeConfig", service.getSizeConfig(p.getProductId()));
                forwardForm(req, resp, "Sửa sản phẩm");
            } else {
                req.setAttribute("products", service.getProductList());
                req.setAttribute("branches", branchService.getBranchListActive());
                req.setAttribute("pageTitle", "Sản phẩm");
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
                req.getSession().setAttribute("flashOk", "Đã cập nhật trạng thái sản phẩm.");
                resp.sendRedirect(ctx + "/admin/product");
                return;
            }
            if ("publishToBranch".equals(action)) {
                int productId = Integer.parseInt(req.getParameter("id"));
                int branchId = Integer.parseInt(req.getParameter("branchId"));
                service.publishToBranch(productId, branchId);
                req.getSession().setAttribute("flashOk", "Đã thêm sản phẩm vào chi nhánh.");
                resp.sendRedirect(ctx + "/admin/product");
                return;
            }
            if ("publishManyToBranch".equals(action)) {
                String[] selected = req.getParameterValues("productIds");
                int branchId = parsePositiveInt(req.getParameter("branchId"));
                if (selected == null || selected.length == 0) {
                    req.getSession().setAttribute("flashError", "Vui lòng chọn ít nhất 1 sản phẩm.");
                    resp.sendRedirect(ctx + "/admin/product");
                    return;
                }
                if (branchId <= 0) {
                    req.getSession().setAttribute("flashError", "Vui lòng chọn chi nhánh.");
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
                    req.getSession().setAttribute("flashError", "Vui lòng chọn ít nhất 1 sản phẩm hợp lệ.");
                    resp.sendRedirect(ctx + "/admin/product");
                    return;
                }
                if (count < productIds.length) {
                    int[] trimmed = new int[count];
                    System.arraycopy(productIds, 0, trimmed, 0, count);
                    productIds = trimmed;
                }
                service.publishManyToBranch(productIds, branchId);
                req.getSession().setAttribute("flashOk", "Đã thêm " + productIds.length + " sản phẩm vào chi nhánh.");
                resp.sendRedirect(ctx + "/admin/product");
                return;
            }
            ProductForm form = ProductForm.from(req);
            Product p = form.product();
            ProductSizeConfig sizeConfig = form.sizeConfig();
            if (p.getProductId() == 0) {
                service.createProduct(p, sizeConfig);
                req.getSession().setAttribute("flashOk", "Đã thêm sản phẩm thành công.");
            } else {
                service.updateProduct(p, sizeConfig);
                req.getSession().setAttribute("flashOk", "Đã cập nhật sản phẩm thành công.");
            }
            resp.sendRedirect(ctx + "/admin/product");
        } catch (BusinessException | FormBindingException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(ctx + "/admin/product");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, String title)
            throws ServletException, IOException {
        try { req.setAttribute("categories", categoryService.getCategoryList()); }
        catch (Exception e) { throw new ServletException(e); }
        req.setAttribute("pageTitle", title);
        req.getRequestDispatcher("/WEB-INF/views/admin/product-form.jsp").forward(req, resp);
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
