package com.mycoffee.controller.cashier;

import com.mycoffee.dao.OrderDAO;
import com.mycoffee.dao.ProductDAO;
import com.mycoffee.model.Order;
import com.mycoffee.model.OrderDetail;
import com.mycoffee.model.Product;
import com.mycoffee.model.User;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "PosOrderController", urlPatterns = {"/pos"})
public class PosOrderController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null || (user.getRoleId() > 3)) {
            response.sendRedirect("login"); return;
        }

        String action = request.getParameter("action");
        OrderDAO orderDAO = new OrderDAO();
        int branchId = 1;

        if ("create".equals(action)) {
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            int orderId = orderDAO.createNewOrder(branchId, tableId, user.getUserId());
            response.sendRedirect("pos?action=view&orderId=" + orderId);

        } else if ("add_item".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            int productId = Integer.parseInt(request.getParameter("productId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));
            double price = Double.parseDouble(request.getParameter("price"));

            orderDAO.addOrUpdateOrderDetail(orderId, productId, quantity, price);
            response.sendRedirect("pos?action=view&orderId=" + orderId);

        } else if ("view".equals(action)) {
            int orderId = Integer.parseInt(request.getParameter("orderId"));

            // Lấy Dữ liệu Đơn hàng & Thực đơn
            Order order = orderDAO.getOrderById(orderId);
            List<OrderDetail> orderDetails = orderDAO.getOrderDetails(orderId);
            List<Product> products = new ProductDAO().getAllAvailableProducts();

            request.setAttribute("order", order);
            request.setAttribute("orderDetails", orderDetails);
            request.setAttribute("products", products);

            request.getRequestDispatcher("pos_screen.jsp").forward(request, response);
        } else {
            response.sendRedirect("pos-tables");
        }
    }
}
