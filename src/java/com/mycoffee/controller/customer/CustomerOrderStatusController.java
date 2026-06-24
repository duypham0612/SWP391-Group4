package com.mycoffee.controller.customer;

import com.mycoffee.dao.OrderDAO;
import com.mycoffee.model.Order;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "CustomerOrderStatusController", urlPatterns = {"/customer-order-status"})
public class CustomerOrderStatusController extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        int orderId = parseInt(request.getParameter("orderId"), 0);

        if (orderId <= 0) {
            Object lastOrderId = session.getAttribute("lastCustomerOrderId");
            if (lastOrderId instanceof Integer) {
                orderId = (Integer) lastOrderId;
            }
        }

        if (orderId > 0) {
            Order order = orderDAO.getOrderById(orderId);
            request.setAttribute("order", order);
            if (order != null) {
                request.setAttribute("orderDetails", orderDAO.getOrderDetails(orderId));
            }
        }

        request.getRequestDispatcher("customer_order_status.jsp").forward(request, response);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
