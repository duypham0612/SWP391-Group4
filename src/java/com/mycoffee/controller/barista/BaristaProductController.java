package com.mycoffee.controller.barista;

import com.mycoffee.dao.BaristaDAO;
import com.mycoffee.model.ProductAvailability;
import com.mycoffee.model.User;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Màn 4: Quản lý sản phẩm / Báo tạm hết món (bật-tắt IsAvailable). */
@WebServlet(name = "BaristaProductController", urlPatterns = {"/barista-products"})
public class BaristaProductController extends HttpServlet {

    private final BaristaDAO dao = new BaristaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRoleId() > 4) {
            response.sendRedirect("login");
            return;
        }
        List<ProductAvailability> list = dao.getAllProductsAvailability();
        int avail = 0, out = 0;
        for (ProductAvailability p : list) { if (p.isAvailable()) avail++; else out++; }
        request.setAttribute("productList", list);
        request.setAttribute("availCount", avail);
        request.setAttribute("outCount", out);
        request.getRequestDispatcher("/views/barista/barista_products.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRoleId() > 4) {
            response.sendRedirect("login");
            return;
        }
        try {
            int productId = Integer.parseInt(request.getParameter("productId"));
            boolean available = "1".equals(request.getParameter("available"));
            dao.setProductAvailable(productId, available);
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.sendRedirect(request.getContextPath() + "/barista-products");
    }
}
