package com.mycoffee.controller.cashier;

import com.mycoffee.dao.OrderDAO;
import com.mycoffee.model.Order;
import com.mycoffee.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "CashierOrderController", urlPatterns = {"/cashier-orders"})
public class CashierOrderController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null || user.getRoleId() != 3) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        OrderDAO orderDAO = new OrderDAO();
        List<Order> orderList = orderDAO.getTodayOrders(1); // Mặc định Branch = 1

        request.setAttribute("orderList", orderList);
        request.getRequestDispatcher("/views/cashier/cashier_orders.jsp").forward(request, response);
    }
}