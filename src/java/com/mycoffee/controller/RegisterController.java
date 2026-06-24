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
        
        // Thiết lập bảng mã UTF-8 chống lỗi font Tiếng Việt
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");

        // 1. Validate kiểm tra rỗng các trường bắt buộc
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            fullName == null || fullName.trim().isEmpty() ||
            email == null || email.trim().isEmpty()) {
            
            request.setAttribute("error", "Vui lòng điền đầy đủ các thông tin bắt buộc.");
            keepOldInputValues(request, username, fullName, email, phone);
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // 2. Sử dụng checkDuplicateFields từ UserDAO để lấy thông báo lỗi chi tiết (Định dạng SĐT + Trùng lặp)
        String duplicateError = userDAO.checkDuplicateFields(username.trim(), email.trim(), phone);
        if (duplicateError != null) {
            request.setAttribute("error", duplicateError); // Trả về câu lỗi chính xác từ DB (ví dụ: "Địa chỉ email đã tồn tại!")
            keepOldInputValues(request, username, fullName, email, phone);
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        // 3. Đóng gói dữ liệu vào đối tượng Model mới
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPassword(password); // Nên mã hóa password nếu hệ thống có tầng mã hóa băm (BCrypt/MD5)
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim());
        newUser.setPhone(phone != null ? phone.trim() : null);

        // 4. Gọi hàm xử lý đăng ký (Transaction tạo cả bên bảng Users và Customers)
        boolean isSuccess = userDAO.registerUser(newUser);

        if (isSuccess) {
            // Chuyển hướng sang trang đăng nhập kèm cờ thông báo đăng ký thành công
            response.sendRedirect(request.getContextPath() + "/login?registered=success");
        } else {
            request.setAttribute("error", "Hệ thống gặp sự cố khi tạo tài khoản. Vui lòng thử lại sau!");
            keepOldInputValues(request, username, fullName, email, phone);
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }

    /**
     * Hàm phụ trợ giữ lại dữ liệu cũ người dùng đã điền, tránh việc phải nhập lại từ đầu khi lỗi.
     */
    private void keepOldInputValues(HttpServletRequest request, String username, String fullName, String email, String phone) {
        request.setAttribute("oldUsername", username);
        request.setAttribute("oldFullName", fullName);
        request.setAttribute("oldEmail", email);
        request.setAttribute("oldPhone", phone);
    }
}