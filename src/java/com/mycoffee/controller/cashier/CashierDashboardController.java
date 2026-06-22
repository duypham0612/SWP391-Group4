package com.mycoffee.controller.cashier;

import com.mycoffee.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "CashierDashboardController", urlPatterns = {"/cashier-dashboard"})
public class CashierDashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        // Ở đây bạn có thể gọi thêm DAO/Service để đếm số đơn hàng, số bàn đang phục vụ...
        // Tạm thời mình truyền các số liệu giả lập để hiển thị UI
        request.setAttribute("servingTables", 5);
        request.setAttribute("pendingOrders", 12);
        request.setAttribute("todayRevenue", 2550000.0);

        request.getRequestDispatcher("cashier_dashboard.jsp").forward(request, response);
    }
}