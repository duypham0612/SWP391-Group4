package com.mycoffee.controller.customer;

import com.mycoffee.dao.OrderDAO;
import com.mycoffee.model.Order;
import com.mycoffee.model.OrderDetail;
import com.mycoffee.model.User;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "CustomerPurchaseHistoryController", urlPatterns = {"/customer-purchase-history"})
public class CustomerPurchaseHistoryController extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;
        String filter = request.getParameter("filter");

        if (filter == null || filter.trim().isEmpty()) {
            filter = "all";
        }

        if (user != null && user.getRoleId() == 5) {
            List<Order> orders = orderDAO.getOrdersByCustomerId(user.getUserId());
            Map<Integer, List<OrderDetail>> orderDetailsMap = new HashMap<>();
            for (Order order : orders) {
                orderDetailsMap.put(order.getOrderId(), orderDAO.getOrderDetails(order.getOrderId()));
            }
            request.setAttribute("orders", orders);
            request.setAttribute("orderDetailsMap", orderDetailsMap);
        }

        request.setAttribute("activeFilter", filter);
        request.getRequestDispatcher("customer_purchase_history.jsp").forward(request, response);
    }
}
