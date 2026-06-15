package com.mycoffee.controller;

import com.mycoffee.dao.EmployeeDAO;
import com.mycoffee.model.Employee;
import com.mycoffee.model.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "EmployeeController", urlPatterns = {"/admin/employees"})
public class EmployeeController extends HttpServlet {

    private final EmployeeDAO dao = new EmployeeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) {
            action = "list";
        }

        switch (action) {
            case "new":
                List<Role> rolesNew = dao.getAllRoles();
                request.setAttribute("roles", rolesNew);
                request.getRequestDispatcher("/admin/employee-form.jsp").forward(request, response);
                break;

            case "edit":
                try {
                    int idEdit = Integer.parseInt(request.getParameter("id"));
                    Employee emp = dao.getEmployeeById(idEdit);
                    List<Role> rolesEdit = dao.getAllRoles();

                    request.setAttribute("employee", emp);
                    request.setAttribute("roles", rolesEdit);
                    request.getRequestDispatcher("/admin/employee-form.jsp").forward(request, response);
                } catch (NumberFormatException e) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees");
                }
                break;

            case "toggle":
                try {
                    int idToggle = Integer.parseInt(request.getParameter("id"));
                    dao.toggleStatus(idToggle);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                response.sendRedirect(request.getContextPath() + "/admin/employees");
                break;

            default:
                List<Employee> employeeList = dao.getAllEmployees();
                List<Role> roleList = dao.getAllRoles();
                request.setAttribute("employees", employeeList);
                request.setAttribute("roles", roleList);
                request.getRequestDispatcher("/admin/employee-management.jsp").forward(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");

        // Xử lý thay đổi Role nhanh từ bảng
        if ("quickRole".equals(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                int roleId = Integer.parseInt(request.getParameter("roleId"));
                dao.updateEmployeeRole(id, roleId);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            response.sendRedirect(request.getContextPath() + "/admin/employees");
            return;
        }

        // Lấy dữ liệu từ form
        String name = request.getParameter("fullName");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String roleIdStr = request.getParameter("roleId");

        // ==================== THÊM MỚI NHÂN VIÊN ====================
        if ("insert".equals(action)) {
            try {
                // Debug log (bạn có thể comment sau khi test ổn)
                System.out.println("====== KIỂM TRA DỮ LIỆU ĐẦU VÀO TỪ POPUP ======");
                System.out.println("Họ và tên: " + name);
                System.out.println("Email: " + email);
                System.out.println("Mật khẩu: " + (password != null && !password.isEmpty() ? "[Đã nhận]" : "TRỐNG/NULL"));
                System.out.println("Mã vai trò: " + roleIdStr);
                System.out.println("=============================================");

                // Kiểm tra dữ liệu đầu vào
                if (name == null || name.trim().isEmpty() ||
                    email == null || email.trim().isEmpty() ||
                    password == null || password.trim().isEmpty() ||
                    roleIdStr == null || roleIdStr.trim().isEmpty()) {
                    
                    System.out.println("⚠️ LỖI: Thiếu thông tin bắt buộc!");
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=missingFields");
                    return;
                }

                int roleId = Integer.parseInt(roleIdStr.trim());
                boolean isInserted = dao.addEmployee(name.trim(), email.trim(), password, roleId);

                if (isInserted) {
                    System.out.println("🎉 THÀNH CÔNG: Nhân viên mới đã được thêm vào Database!");
                    response.sendRedirect(request.getContextPath() + "/admin/employees?success=added");
                } else {
                    System.out.println("❌ THẤT BẠI: Không thêm được vào Database (có thể email đã tồn tại)");
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=insertFailed");
                }

            } catch (NumberFormatException e) {
                System.out.println("❌ LỖI: Mã vai trò không hợp lệ!");
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=invalidRole");
            } catch (Exception e) {
                System.out.println("❌ LỖI HỆ THỐNG:");
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=systemError");
            }
            return;
        }

        // ==================== CẬP NHẬT NHÂN VIÊN ====================
        if ("update".equals(action)) {
            try {
                int id = Integer.parseInt(request.getParameter("id"));
                int roleId = Integer.parseInt(roleIdStr);
                boolean isActive = "Đang làm".equals(request.getParameter("status"));

                boolean updated = dao.updateEmployee(id, name, email, roleId, isActive);
                
                if (updated) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?success=updated");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed");
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed");
            }
            return;
        }

        // Mặc định redirect về danh sách
        response.sendRedirect(request.getContextPath() + "/admin/employees");
    }
}
