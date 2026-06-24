package com.mycoffee.controller;

import com.mycoffee.dao.UserDAO;
import com.mycoffee.model.User;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "RegisterController", urlPatterns = {"/register"})
public class RegisterController extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Hiển thị trang đăng ký
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Thiết lập bảng mã UTF-8
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");

        username = trimToNull(username);
        fullName = trimToNull(fullName);
        email = trimToNull(email);
        phone = trimToNull(phone);

        if (username == null || password == null || password.isEmpty() || fullName == null) {
            forwardWithError(request, response,
                    "Vui lòng nhập đầy đủ các trường bắt buộc.",
                    username, fullName, email, phone);
            return;
        }

        if (username.length() < 3 || username.length() > 50) {
            forwardWithError(request, response,
                    "Tên đăng nhập phải có từ 3 đến 50 ký tự.",
                    username, fullName, email, phone);
            return;
        }

        if (password.length() < 6 || password.length() > 255) {
            forwardWithError(request, response,
                    "Mật khẩu phải có ít nhất 6 ký tự.",
                    username, fullName, email, phone);
            return;
        }

        if (fullName.length() > 100
                || (email != null && email.length() > 100)
                || (phone != null && phone.length() > 15)) {
            forwardWithError(request, response,
                    "Thông tin đăng ký vượt quá độ dài cho phép.",
                    username, fullName, email, phone);
            return;
        }

        // Kiểm tra xem Username, Email hoặc Phone đã tồn tại chưa
        if (userDAO.isUserExists(username, email, phone)) {
            forwardWithError(request, response,
                    "Tên đăng nhập, Email hoặc Số điện thoại đã được sử dụng!",
                    username, fullName, email, phone);
            return;
        }

        // Tạo đối tượng User mới
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPhone(phone);

        // Lưu vào CSDL
        boolean isSuccess = userDAO.registerUser(newUser);

        if (isSuccess) {
            // Chuyển hướng sang trang đăng nhập kèm cờ thông báo đăng ký thành công
            response.sendRedirect(request.getContextPath() + "/login?registered=success");
        } else {
            forwardWithError(request, response,
                    "Đăng ký thất bại! Vui lòng thử lại sau.",
                    username, fullName, email, phone);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void forwardWithError(HttpServletRequest request, HttpServletResponse response,
                                  String error, String username, String fullName,
                                  String email, String phone)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("oldUsername", username);
        request.setAttribute("oldFullName", fullName);
        request.setAttribute("oldEmail", email);
        request.setAttribute("oldPhone", phone);
        request.getRequestDispatcher("register.jsp").forward(request, response);
    }
}
