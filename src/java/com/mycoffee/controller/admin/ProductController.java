package com.mycoffee.controller.admin;

import com.mycoffee.dao.ProductDAO;
import com.mycoffee.dao.CategoryDAO; 
import com.mycoffee.model.Product;
import com.mycoffee.model.Category; 
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/admin/menu")
public class ProductController extends HttpServlet {
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO(); 
    private final int PAGE_SIZE = 8; 

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String action = request.getParameter("action");
        
        // --- XỬ LÝ XÓA DANH MỤC TRỰC TIẾP ---
        if ("deleteCategory".equals(action)) {
            try {
                int catId = Integer.parseInt(request.getParameter("catId"));
                boolean delSuccess = categoryDAO.deleteCategory(catId);
                if (delSuccess) {
                    response.sendRedirect(request.getContextPath() + "/admin/menu?success=cat_deleted");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/menu?error=cat_failed");
                }
            } catch (Exception e) {
                // Thường lỗi do khóa ngoại nếu danh mục đang được gắn vào món ăn
                response.sendRedirect(request.getContextPath() + "/admin/menu?error=cat_has_products");
            }
            return;
        }

        if ("toggle".equals(action)) {
            int id = Integer.parseInt(request.getParameter("id"));
            boolean status = Boolean.parseBoolean(request.getParameter("status"));
            productDAO.toggleAvailability(id, status);
            String pageStr = request.getParameter("page");
            response.sendRedirect(request.getContextPath() + "/admin/menu?page=" + (pageStr != null ? pageStr : "1"));
            return;
        }
        
        if ("delete".equals(action)) {
            int id = Integer.parseInt(request.getParameter("id"));
            productDAO.deleteProduct(id);
            response.sendRedirect(request.getContextPath() + "/admin/menu?success=deleted");
            return;
        }

        int currentPage = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.trim().isEmpty()) {
            try {
                currentPage = Integer.parseInt(pageParam);
            } catch (NumberFormatException e) {
                currentPage = 1;
            }
        }

        String searchKeyword = request.getParameter("search");
        List<Product> productList;
        int totalProducts;

        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            productList = productDAO.searchProducts(searchKeyword.trim());
            totalProducts = productList.size();
        } else {
            totalProducts = productDAO.getTotalProductsCount();
            productList = productDAO.getProductsByPage(currentPage, PAGE_SIZE);
        }
        
        int totalPages = (int) Math.ceil((double) totalProducts / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
        
        if (totalPages > 999) {
            totalPages = 999;
        }
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        
        int newProductsCount = productDAO.getNewProductsCountThisMonth();

        // --- LUÔN LOAD DANH SÁCH DANH MỤC RA NGOÀI GIAO DIỆN ---
        List<Category> categoryList = categoryDAO.getAllCategories();
        request.setAttribute("categories", categoryList);

        request.setAttribute("products", productList);
        request.setAttribute("currentPage", currentPage);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalProducts", totalProducts);
        request.setAttribute("newProductsCount", newProductsCount);

        // --- NHÁNH KIỂM TRA REQUEST GỌI QUA AJAX TÌM KIẾM ---
        String isAjax = request.getParameter("ajax");
        if ("true".equals(isAjax)) {
            // Thiết lập header trả về dạng HTML gọn gàng cho Ajax bóc tách dữ liệu công cụ tìm kiếm
            response.setContentType("text/html;charset=UTF-8");
            request.getRequestDispatcher("/admin/menu-management.jsp").forward(request, response);
        } else {
            // Request tải trang thông thường
            request.getRequestDispatcher("/admin/menu-management.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "add";
        }

        // --- XỬ LÝ SUBMIT FROM CHO THÊM / SỬA DANH MỤC ---
        if ("addCategory".equals(action) || "editCategory".equals(action)) {
            String categoryName = request.getParameter("categoryName");
            String catDescription = request.getParameter("catDescription");
            
            if (categoryName == null || categoryName.trim().isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/admin/menu?error=cat_failed");
                return;
            }

            if ("addCategory".equals(action)) {
                Category newCat = new Category(categoryName, catDescription);
                boolean success = categoryDAO.insertCategory(newCat); 
                response.sendRedirect(request.getContextPath() + "/admin/menu?success=" + (success ? "cat_added" : "failed"));
            } else {
                int categoryId = Integer.parseInt(request.getParameter("categoryId"));
                Category updateCat = new Category(categoryId, categoryName, catDescription);
                boolean success = categoryDAO.updateCategory(updateCat); 
                response.sendRedirect(request.getContextPath() + "/admin/menu?success=" + (success ? "cat_updated" : "failed"));
            }
            return; 
        }

        // XỬ LÝ NHÁNH SẢN PHẨM CŨ (ADD/EDIT PRODUCTS)
        try {
            String productName = request.getParameter("productName");
            int categoryID = Integer.parseInt(request.getParameter("categoryID"));
            double basePrice = Double.parseDouble(request.getParameter("basePrice"));
            String imageURL = request.getParameter("imageURL");
            String description = request.getParameter("description");
            
            if (basePrice <= 20000) {
                response.sendRedirect(request.getContextPath() + "/admin/menu?error=invalid_price");
                return;
            }
            
            if ("edit".equals(action)) {
                int productId = Integer.parseInt(request.getParameter("productId"));

                if (productDAO.isProductNameExistsForEdit(productName, productId)) {
                    response.sendRedirect(request.getContextPath() + "/admin/menu?error=duplicate_edit");
                    return;
                }

                Product product = new Product();
                product.setProductId(productId);
                product.setProductName(productName);
                product.setCategoryId(categoryID);
                product.setBasePrice(basePrice);
                product.setImageUrl(imageURL);
                product.setDescription(description);

                boolean updateSuccess = productDAO.updateProduct(product);
                if (updateSuccess) {
                    response.sendRedirect(request.getContextPath() + "/admin/menu?success=updated");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/menu?error=failed");
                }
                return; 
            }

            boolean isAvailable = request.getParameter("isAvailable") != null;

            if (productDAO.isProductNameExists(productName)) {
                response.sendRedirect(request.getContextPath() + "/admin/menu?error=duplicate");
                return;
            }

            Product product = new Product();
            product.setProductName(productName);
            product.setCategoryId(categoryID);
            product.setBasePrice(basePrice);
            product.setImageUrl(imageURL);
            product.setDescription(description);
            product.setIsAvailable(isAvailable);

            boolean insertSuccess = productDAO.addProduct(product);
            if (insertSuccess) {
                response.sendRedirect(request.getContextPath() + "/admin/menu?success=added");
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/menu?error=failed");
            }

        } catch (Exception e) {
            System.out.println("===> LỖI TẠI DO_POST (ProductController): " + e.getMessage());
            e.printStackTrace();
            response.getWriter().println("Loi he thong backend: " + e.toString());
        }
    }
}
