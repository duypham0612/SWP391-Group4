package com.cafe.web.form;

import com.cafe.model.Category;
import jakarta.servlet.http.HttpServletRequest;

/** Mapper cú pháp của form danh mục. */
public record CategoryForm(Category category) {
    public static CategoryForm from(HttpServletRequest request) {
        Category category = new Category();
        category.setCategoryId(FormValues.optionalInt(request.getParameter("categoryId"), "Mã danh mục"));
        category.setName(FormValues.trim(request.getParameter("name")));
        category.setSortOrder(FormValues.optionalInt(request.getParameter("sortOrder"), "Thứ tự"));
        category.setActive(request.getParameter("active") != null);
        return new CategoryForm(category);
    }
}
