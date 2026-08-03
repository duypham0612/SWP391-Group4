package com.cafe.web.form;

import com.cafe.model.Product;
import jakarta.servlet.http.HttpServletRequest;

/** Mapper cú pháp của form sản phẩm; không quyết định giá/category hợp lệ. */
public record ProductForm(Product product) {
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

        return new ProductForm(product);
    }
}
