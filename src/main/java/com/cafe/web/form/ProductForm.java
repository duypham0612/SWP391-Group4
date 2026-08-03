package com.cafe.web.form;

import com.cafe.model.Product;
import com.cafe.service.admin.ProductService.ProductSizeConfig;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;

/** Mapper cú pháp của form sản phẩm; không quyết định giá/category hợp lệ. */
public record ProductForm(Product product, ProductSizeConfig sizeConfig) {
    public static ProductForm from(HttpServletRequest request) {
        Product product = new Product();
        product.setProductId(FormValues.optionalInt(request.getParameter("productId"), "Mã sản phẩm"));
        product.setCategoryId(FormValues.optionalInt(request.getParameter("categoryId"), "Danh mục"));
        product.setName(FormValues.trim(request.getParameter("name")));
        product.setBasePrice(FormValues.decimal(request.getParameter("basePrice"), "Giá bán"));
        product.setImageUrl(FormValues.trim(request.getParameter("imageUrl")));
        product.setActive(request.getParameter("active") != null);
        int prepMinutes = request.getParameter("prepMinutes") == null
                || request.getParameter("prepMinutes").isBlank()
                ? 12 : FormValues.optionalInt(request.getParameter("prepMinutes"), "Thời gian pha");
        try { product.setPrepSeconds(Math.multiplyExact(prepMinutes, 60)); }
        catch (ArithmeticException e) { throw new FormBindingException("Thời gian pha quá lớn."); }

        ProductSizeConfig sizes = ProductSizeConfig.defaults();
        sizes.setSizeMDelta(money(request, "sizeMDelta", "Giá tăng size M"));
        sizes.setSizeLDelta(money(request, "sizeLDelta", "Giá tăng size L"));
        return new ProductForm(product, sizes);
    }

    private static BigDecimal money(HttpServletRequest request, String name, String label) {
        return FormValues.decimal(request.getParameter(name), label);
    }
}
