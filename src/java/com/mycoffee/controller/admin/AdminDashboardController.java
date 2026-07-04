package com.mycoffee.controller.admin;
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        // 1. KIỂM TRA PHÂN QUYỀN (Logic an toàn từ HEAD)
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        int roleId = (user != null) ? user.getRoleId() : 0;

        // Chỉ Admin (RoleID = 1) mới được vào
        if (roleId != User.ROLE_ADMIN) {
            if (roleId == User.ROLE_BRANCH_MANAGER) {
                response.sendRedirect(request.getContextPath() + "/manager-dashboard");
            } else if (roleId == User.ROLE_EMPLOYEE) {
                response.sendRedirect(request.getContextPath() + "/pos-tables");
            } else if (roleId == User.ROLE_CUSTOMER) {
                response.sendRedirect(request.getContextPath() + "/menu");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }
        request.getRequestDispatcher("/views/admin/admin_dashboard.jsp")
               .forward(request, response);
    }
}

