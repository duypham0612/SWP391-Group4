package com.mycoffee.controller.admin;

import com.mycoffee.dao.DashboardDAO;
import com.mycoffee.model.DashboardStats;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "AdminDashboardController", urlPatterns = {"/admin-dashboard"})
public class AdminDashboardController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        // Gọi DAO lấy dữ liệu động từ SQL Server
        DashboardDAO dao = new DashboardDAO();
        DashboardStats stats = dao.getDashboardData();
        
        // Đẩy dữ liệu vào Request Attribute để JSP hứng
        request.setAttribute("stats", stats);
        
        // Chuyển tiếp (Forward) sang trang giao diện jsp theo đúng cây thư mục của bạn
        request.getRequestDispatcher("/views/admin/admin_dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
