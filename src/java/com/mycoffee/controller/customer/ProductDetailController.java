package com.mycoffee.controller.customer;

import com.mycoffee.dao.ProductDAO;
import com.mycoffee.model.Product;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "ProductDetailController", urlPatterns = {"/product-detail"})
public class ProductDetailController extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int productId = parseInt(request.getParameter("id"), 0);
        Product product = productDAO.getAvailableProductById(productId);

        if (product == null) {
            response.sendRedirect(request.getContextPath() + "/menu");
            return;
        }

        request.setAttribute("product", product);
        request.getRequestDispatcher("product_detail.jsp").forward(request, response);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
