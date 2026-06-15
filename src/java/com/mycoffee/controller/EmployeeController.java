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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        // ==================== 1. THÊM MỚI NHÂN VIÊN ====================
        if ("insert".equals(action)) {
            String username = request.getParameter("username"); 
            String fullName = request.getParameter("fullName");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String roleIdStr = request.getParameter("roleId");
            String phone = request.getParameter("phone");
            
            try {
                int roleId = Integer.parseInt(roleIdStr.trim());
                
                // Gọi hàm nạp đầy đủ thông tin độc lập sang DAO
                String result = dao.addEmployeeDetailed(username, fullName, email, password, roleId, phone);
                
                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?success=true");
                } else if ("DUPLICATE_USERNAME".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=duplicateUser");
                } else if ("DUPLICATE_EMAIL".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=duplicateEmail");
                } else if ("DUPLICATE_PHONE".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=duplicatePhone");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=systemError");
                }

            } catch (NumberFormatException e) {
                System.out.println("❌ LỖI: Mã vai trò không hợp lệ!");
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=invalidRole");
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=systemError");
            }
            return;
        }

        // ==================== 2. CẬP NHẬT NHÂN VIÊN ====================
        if ("update".equals(action)) {
            // Khai báo và hứng đầy đủ dữ liệu từ form sửa gửi lên
            String idStr = request.getParameter("id");
            String username = request.getParameter("username"); 
            String fullName = request.getParameter("fullName");
            String email = request.getParameter("email");
            String phone = request.getParameter("phone");
            String roleIdStr = request.getParameter("roleId");
            String status = request.getParameter("status");

            try {
                int id = Integer.parseInt(idStr.trim());
                int roleId = Integer.parseInt(roleIdStr.trim());
                boolean isActive = "Đang làm".equals(status) || "1".equals(status) || "true".equalsIgnoreCase(status);

                // Gọi hàm xử lý cập nhật có lọc trùng thông minh từ DAO
                String checkResult = dao.updateEmployeeDetailed(id, username, fullName, email, phone, roleId, isActive);
                
                if ("SUCCESS".equals(checkResult)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?success=updated");
                } else if ("DUPLICATE_USERNAME".equals(checkResult)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateDuplicateUser");
                } else if ("DUPLICATE_EMAIL".equals(checkResult)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateDuplicateEmail");
                } else if ("DUPLICATE_PHONE".equals(checkResult)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateDuplicatePhone");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed");
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed");
            }
            return;
        }

        // Mặc định không khớp action nào thì redirect về danh sách chính
        response.sendRedirect(request.getContextPath() + "/admin/employees");
    }
}