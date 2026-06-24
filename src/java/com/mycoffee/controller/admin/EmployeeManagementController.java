package com.mycoffee.controller.admin;

import com.mycoffee.model.User;
import com.mycoffee.service.EmployeeService;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "EmployeeManagementController", urlPatterns = {"/admin-employees"})
public class EmployeeManagementController extends HttpServlet {

    private final EmployeeService employeeService = new EmployeeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "toggleStatus":
                try {
                    int id = Integer.parseInt(request.getParameter("id"));
                    boolean status = Boolean.parseBoolean(request.getParameter("status"));

                    // BẢO VỆ TẦNG BACKEND: Chặn không cho phép tự khóa tài khoản Admin tối cao (RoleID = 1)
                    User checkUser = employeeService.getUserById(id);
                    if (checkUser != null && checkUser.getRoleId() == 1) {
                        request.getSession().setAttribute("errorMessage", "Không thể thay đổi trạng thái hoạt động của Admin tối cao!");
                        response.sendRedirect(request.getContextPath() + "/admin-employees");
                        return;
                    }

                    boolean isUpdated = employeeService.toggleEmployeeStatus(id, status);
                    if (isUpdated) {
                        request.getSession().setAttribute("successMessage", status ? "Mở khóa tài khoản thành công!" : "Đã khóa tài khoản thành công!");
                    } else {
                        request.getSession().setAttribute("errorMessage", "Cập nhật trạng thái thất bại hoặc tài khoản không tồn tại!");
                    }
                } catch (NumberFormatException e) {
                    request.getSession().setAttribute("errorMessage", "Mã số tài khoản không hợp lệ!");
                }
                response.sendRedirect(request.getContextPath() + "/admin-employees");
                break;

            case "delete":
                try {
                    int id = Integer.parseInt(request.getParameter("id"));
                    
                    // BẢO VỆ TẦNG BACKEND: Chặn không cho xóa Admin (RoleID = 1)
                    User checkUser = employeeService.getUserById(id);
                    if (checkUser != null && checkUser.getRoleId() == 1) {
                        request.getSession().setAttribute("errorMessage", "Mối nguy hại bảo mật! Không được phép xóa Admin tối cao.");
                        response.sendRedirect(request.getContextPath() + "/admin-employees");
                        return;
                    }

                    boolean isDeleted = employeeService.deleteEmployee(id);
                    if (isDeleted) {
                        request.getSession().setAttribute("successMessage", "Xóa tài khoản thành công khỏi hệ thống!");
                    } else {
                        request.getSession().setAttribute("errorMessage", "Xóa thất bại. Tài khoản không tồn tại hoặc lỗi hệ thống!");
                    }
                } catch (NumberFormatException e) {
                    request.getSession().setAttribute("errorMessage", "Mã số tài khoản không hợp lệ!");
                }
                response.sendRedirect(request.getContextPath() + "/admin-employees");
                break;

            case "edit":
                try {
                    int id = Integer.parseInt(request.getParameter("id"));
                    User emp = employeeService.getUserById(id);
                    if (emp != null) {
                        request.setAttribute("employeeToEdit", emp);
                    }
                } catch (NumberFormatException e) {
                    request.setAttribute("errorMessage", "ID nhân viên không hợp lệ.");
                }
                forwardToUpdateList(request, response);
                break;

            case "list":
            default:
                forwardToUpdateList(request, response);
                break;
        }
    }

    private void forwardToUpdateList(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        List<User> employeeList = employeeService.getAllEmployees();
        request.setAttribute("employeeList", employeeList);
        
        String successMsg = (String) request.getSession().getAttribute("successMessage");
        String errorMsg = (String) request.getSession().getAttribute("errorMessage");
        
        if (successMsg != null) {
            request.setAttribute("successMessage", successMsg);
            request.getSession().removeAttribute("successMessage");
        }
        if (errorMsg != null) {
            request.setAttribute("errorMessage", errorMsg);
            request.getSession().removeAttribute("errorMessage");
        }

        request.getRequestDispatcher("/views/admin/employee_management.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        if ("add".equals(action)) {
            String username = request.getParameter("username");
            String fullName = request.getParameter("fullName");
            String email = request.getParameter("email");
            String phone = request.getParameter("phone");
            int roleId = Integer.parseInt(request.getParameter("roleId"));

            if (roleId == 1) {
                request.getSession().setAttribute("errorMessage", "Hành động bị từ chối! Không được phép tạo thêm quyền Admin.");
                response.sendRedirect(request.getContextPath() + "/admin-employees");
                return;
            }

            String error = employeeService.checkDuplicate(username, email, phone);
            if (error != null) {
                request.getSession().setAttribute("errorMessage", error);
                response.sendRedirect(request.getContextPath() + "/admin-employees");
                return;
            }

            User newEmp = new User();
            newEmp.setUsername(username);
            newEmp.setPassword("123456"); 
            newEmp.setFullName(fullName);
            newEmp.setEmail(email);
            newEmp.setPhone(phone);
            newEmp.setRoleId(roleId);

            boolean isAdded = employeeService.addEmployee(newEmp);
            if (isAdded) {
                request.getSession().setAttribute("successMessage", "Thêm mới thành công! Mật khẩu mặc định là 123456.");
            } else {
                request.getSession().setAttribute("errorMessage", "Thêm mới thất bại! Vui lòng kiểm tra lại.");
            }

        // FIX TẠI ĐÂY: Chấp nhận cả "update" hoặc "edit" gửi từ form JSP lên hệ thống
        } else if ("update".equals(action) || "edit".equals(action)) {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String fullName = request.getParameter("fullName");
            String email = request.getParameter("email");
            String phone = request.getParameter("phone");
            int roleId = Integer.parseInt(request.getParameter("roleId"));
            String newPassword = request.getParameter("newPassword"); 

            User existingUser = employeeService.getUserById(userId);
            if (existingUser != null && existingUser.getRoleId() == 1) {
                if (roleId != 1) {
                    request.getSession().setAttribute("errorMessage", "Lỗi nghiêm trọng: Không được phép đổi quyền Admin gốc!");
                    response.sendRedirect(request.getContextPath() + "/admin-employees");
                    return;
                }
            } else {
                if (roleId == 1) {
                    request.getSession().setAttribute("errorMessage", "Hành động trái phép: Không thể nâng cấp tài khoản này lên Admin!");
                    response.sendRedirect(request.getContextPath() + "/admin-employees");
                    return;
                }
            }

            String error = employeeService.checkDuplicateForUpdate(userId, email, phone);
            if (error != null) {
                request.getSession().setAttribute("errorMessage", error);
                response.sendRedirect(request.getContextPath() + "/admin-employees");
                return;
            }

            User updatedEmp = new User();
            updatedEmp.setUserId(userId);
            updatedEmp.setFullName(fullName);
            updatedEmp.setEmail(email);
            updatedEmp.setPhone(phone);
            updatedEmp.setRoleId(roleId);
            updatedEmp.setPassword(newPassword); 

            boolean isUpdated = employeeService.updateEmployee(updatedEmp);
            if (isUpdated) {
                request.getSession().setAttribute("successMessage", "Cập nhật thông tin tài khoản thành công!");
            } else {
                request.getSession().setAttribute("errorMessage", "Cập nhật thông tin thất bại!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin-employees");
    }
}
