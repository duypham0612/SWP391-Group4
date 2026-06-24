package com.mycoffee.controller;

import com.mycoffee.dao.UserDAO;
import com.mycoffee.dao.TableDAO;
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
            Object selectedTableId = session.getAttribute("customerTableId");
            if (selectedTableId instanceof Integer
                    && "test".equals(session.getAttribute("customerTableSelectionMode"))) {
                new TableDAO().releaseCustomerSelectedTable((Integer) selectedTableId);
            }
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Nếu user đã đăng nhập rồi thì chuyển hướng thẳng đến trang tương ứng với vai trò
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser != null) {
            redirectByRole(loggedInUser.getRoleId(), request, response);
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
            
            redirectByRole(user.getRoleId(), request, response);
        } else {
            // Đăng nhập THẤT BẠI: Đính kèm lời báo lỗi và quay trở lại trang login.jsp
            request.setAttribute("error", "Tài khoản hoặc mật khẩu không chính xác!");
            request.setAttribute("savedUsername", username); // Lưu lại username để điền lại vào form
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    /**
     * Chuyển hướng người dùng đến trang tương ứng với vai trò (RoleID).
     * RoleID theo DB: 1 = System Admin, 2 = Branch Manager, 3 = Cashier, 4 = Barista, 5 = Customer
     *
     * PHÂN QUYỀN:
     *   - RoleID 1, 2, 3 → Trang quản trị/nhân viên (admin-dashboard, manager-dashboard, pos-tables)
     *   - RoleID 4       → Màn pha chế (barista-board)
     *   - RoleID 5       → Trang khách hàng (menu)
     */
    private void redirectByRole(int roleId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();
        switch (roleId) {
            case User.ROLE_ADMIN:
                response.sendRedirect(contextPath + "/admin-dashboard");
                break;
            case User.ROLE_BRANCH_MANAGER:
                response.sendRedirect(contextPath + "/manager-dashboard");
                break;
            case User.ROLE_EMPLOYEE: // 3 — Thu ngân / quầy POS
                response.sendRedirect(contextPath + "/pos-tables");
                break;
            // LƯU Ý: theo DB thật RoleID 4 = Barista, 5 = Customer.
            // (User.ROLE_CUSTOMER=4 trong model đang LỆCH với DB — cần nhóm thống nhất lại.)
            case 4: // Barista — Nhân viên pha chế
                response.sendRedirect(contextPath + "/barista-board");
                break;
            case 5: // Customer — Khách hàng thành viên
                response.sendRedirect(contextPath + "/menu");
                break;
            default:
                response.sendRedirect(contextPath + "/login");
                break;
        }
    }
}
