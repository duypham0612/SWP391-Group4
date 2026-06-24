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

        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser != null) {
            redirectByRole(loggedInUser.getRoleId(), request, response);
            return;
        }

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
            HttpSession session = request.getSession();
            session.setAttribute("user", user);
            redirectByRole(user.getRoleId(), request, response);
        } else {
            request.setAttribute("error", "Tài khoản hoặc mật khẩu không chính xác!");
            request.setAttribute("savedUsername", username);
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    private void redirectByRole(int roleId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();
        switch (roleId) {
            case 1:
                response.sendRedirect(contextPath + "/admin-dashboard");
                break;
            case 2:
                response.sendRedirect(contextPath + "/manager-dashboard");
                break;
            case 3: // Thu ngân (Cashier) vào thẳng màn hình Dashboard của Thu ngân
                response.sendRedirect(contextPath + "/cashier-dashboard");
                break;
            case 4:
                response.sendRedirect(contextPath + "/menu");
                break;
            default:
                response.sendRedirect(contextPath + "/login");
                break;
        }
    }
}