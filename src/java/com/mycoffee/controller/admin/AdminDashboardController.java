package com.mycoffee.controller.admin;

import com.mycoffee.dao.DashboardDAO;
import com.mycoffee.model.Dashboard;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "AdminDashboardController", urlPatterns = {"/admin/admin-dashboard"})
public class AdminDashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Lấy số liệu tổng hợp động từ Database thông qua DAO
        DashboardDAO dashboardDAO = new DashboardDAO();
        Dashboard dashboardData = dashboardDAO.getDashboardStats();

        // 2. Đính kèm đối tượng dữ liệu vào Request Attribute
        request.setAttribute("dashboard", dashboardData);

        // 3. Chuyển tiếp (Forward) quyền hiển thị sang file giao diện JSP tương ứng
        request.getRequestDispatcher("/admin/admin-dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
