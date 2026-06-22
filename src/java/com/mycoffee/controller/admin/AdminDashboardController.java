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

        // AuthFilter đã lo đăng nhập. Ở đây chỉ check RoleID.
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        
        int roleId = (user != null) ? user.getRoleId() : 0;

        // Chỉ Admin (RoleID = 1) mới được vào
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

        // Forward tới trang JSP admin dashboard
        request.getRequestDispatcher("/views/admin/admin_dashboard.jsp")
               .forward(request, response);
    }
}

