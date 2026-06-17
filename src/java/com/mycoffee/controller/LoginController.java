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
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // KIỂM TRA ĐĂNG NHẬP: Phải kiểm tra null trước khi gọi getRoleId()
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser != null) {
            if (loggedInUser.getRoleId() == 1) {
                response.sendRedirect(request.getContextPath() + "/admin/admin-dashboard");
            } else {
                response.sendRedirect(request.getContextPath() + "/menu");
            }
            return; // Dừng lại ở đây, không forward sang login.jsp nữa
        }
        
        // Nếu chưa đăng nhập thì mới forward sang trang login
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        UserDAO dao = new UserDAO();
        User user = dao.checkLogin(username, password);

        if (user != null) {
            if (!user.isIsActive()) {
                request.setAttribute("error", "Tài khoản bị vô hiệu hóa!");
                request.setAttribute("savedUsername", username);
                request.getRequestDispatcher("login.jsp").forward(request, response);
                return;
            }

            HttpSession session = request.getSession();
            session.setAttribute("user", user);

            if (user.getRoleId() == 1) {
                response.sendRedirect(request.getContextPath() + "/admin/admin-dashboard");
            } else {
                response.sendRedirect(request.getContextPath() + "/menu");
            }
        } else {
            request.setAttribute("error", "Tài khoản hoặc mật khẩu không chính xác!");
            request.setAttribute("savedUsername", username);
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
}