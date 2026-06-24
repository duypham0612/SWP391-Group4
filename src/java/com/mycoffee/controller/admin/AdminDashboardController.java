package com.mycoffee.controller.admin;

import com.mycoffee.dao.DashboardDAO;
import com.mycoffee.model.DashboardStats;
import com.mycoffee.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "AdminDashboardController", urlPatterns = {"/admin-dashboard"})
public class AdminDashboardController extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        // 1. KIỂM TRA PHÂN QUYỀN (Logic an toàn từ HEAD)
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        int roleId = (user != null) ? user.getRoleId() : 0;

        // Chỉ Admin (RoleID = 1) mới được vào dashboard này
        if (roleId != 1) {
            if (roleId == 2) {
                response.sendRedirect(request.getContextPath() + "/manager-dashboard");
            } else if (roleId == 3) {
                response.sendRedirect(request.getContextPath() + "/pos-tables");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        // 2. LẤY DỮ LIỆU THỐNG KÊ (Logic cập nhật từ Master)
        DashboardDAO dao = new DashboardDAO();
        DashboardStats stats = dao.getDashboardData();
        
        // Đẩy dữ liệu vào Request Attribute để trang JSP hiển thị
        request.setAttribute("stats", stats);
        
        // 3. FORWARD SANG VIEW
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