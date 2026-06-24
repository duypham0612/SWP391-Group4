package com.mycoffee.controller;

import com.mycoffee.dao.UserDAO;
import com.mycoffee.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "LoginController", urlPatterns = {"/login"})
public class LoginController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        String action = request.getParameter("action");
        if ("logout".equals(action)) {
            session.invalidate();
            response.sendRedirect("login");
            return;
        }

        // Nếu user đã đăng nhập rồi thì chuyển hướng thẳng đến trang tương ứng với vai trò
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser != null) {
            if (loggedInUser.getRoleId() == 2) {
                response.sendRedirect("manager-dashboard");
            } else if (loggedInUser.getRoleId() == 4) {
                response.sendRedirect("barista");
            } else {
                response.sendRedirect("menu");
            }
            return;
        }
        
        // Forward sang trang giao diện login.jsp
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Thiết lập UTF-8
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        // 1. Nhận thông số từ Form Đăng nhập
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        
        // 2. Khởi tạo DAO để kiểm tra đăng nhập
        UserDAO dao = new UserDAO();
        User user = dao.checkLogin(username, password);
        
        if (user != null) {
            // Đăng nhập THÀNH CÔNG: Lưu user vào Session và chuyển hướng dựa theo Vai trò (RoleID)
            HttpSession session = request.getSession();
            session.setAttribute("user", user);

            if (user.getRoleId() == 2) {
                response.sendRedirect("manager-dashboard");
            } else if (user.getRoleId() == 4) {
                response.sendRedirect("barista");
            } else {
                response.sendRedirect("menu");
            }
        } else {
            // Đăng nhập THẤT BẠI: Đính kèm lời báo lỗi và quay trở lại trang login.jsp
            request.setAttribute("error", "Tài khoản hoặc mật khẩu không chính xác!");
            request.setAttribute("savedUsername", username); // Lưu lại username để điền lại vào form
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
}
