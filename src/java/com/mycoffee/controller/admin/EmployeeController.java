package com.mycoffee.controller.admin;

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

            case "delete":
                try {
                int id = Integer.parseInt(request.getParameter("id"));
                EmployeeDAO dao = new EmployeeDAO();

                // 1. Lấy thông tin chi tiết nhân viên trước khi xóa để kiểm tra quyền
                com.mycoffee.model.Employee empToDelete = dao.getEmployeeById(id);

                // 2. Kiểm tra nếu nhân viên này có RoleID là 1 (System Admin) thì KHÔNG CHO XÓA
                if (empToDelete != null && empToDelete.getRoleId() == 1) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=cannotDeleteAdmin");
                    return; // Dừng xử lý ngay lập tức
                }

                // 3. Nếu không phải Admin thì tiến hành xóa bình thường
                boolean isDeleted = dao.deleteEmployee(id);
                if (isDeleted) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?success=delete");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=deleteFailed");
                }
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=system");
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

        // ==================== THÊM MỚI: THAY ĐỔI NHANH CA LÀM VIỆC ====================
        if ("quickShift".equals(action)) {
            String idStr = request.getParameter("id");
            String shiftIdStr = request.getParameter("shiftId");
            try {
                if (idStr != null && shiftIdStr != null) {
                    int id = Integer.parseInt(idStr.trim());
                    int shiftId = Integer.parseInt(shiftIdStr.trim());

                    boolean isUpdated = dao.updateEmployeeShift(id, shiftId);
                    if (isUpdated) {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?success=updated");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed");
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed");
                return;
            }
        }

        // ==================== 1. THÊM MỚI NHÂN VIÊN ====================
        if ("insert ".equals(action)) {
            String username = request.getParameter("username");
            String fullName = request.getParameter("fullName");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String roleIdStr = request.getParameter("roleId");
            String phone = request.getParameter("phone");

            try {
                int roleId = Integer.parseInt(roleIdStr.trim());
                String result = dao.addEmployeeDetailed(username, fullName, email, password, roleId, phone);

                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?success=true");
                } else if ("ADMIN_ALREADY_EXISTS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=duplicateAdmin");
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
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=invalidRole");
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=systemError");
            }
            return;
        }

        // ==================== 2. CẬP NHẬT NHÂN VIÊN CHI TIẾT ====================
        if ("update".equals(action)) {
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

                Employee currentEmp = dao.getEmployeeById(id);
                if (currentEmp != null && "System Admin".equalsIgnoreCase(currentEmp.getRoleName())) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=cannotChangeAdminRoot");
                    return;
                }

                String checkResult = dao.updateEmployeeDetailed(id, username, fullName, email, phone, roleId, isActive);
                if ("SUCCESS".equals(checkResult)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?success=updated");
                } else if ("ADMIN_ALREADY_EXISTS".equals(checkResult)) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateDuplicateAdmin");
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

        // ==================== 3. THAY ĐỔI NHANH CHỨC VỤ ====================
        if ("quickRole".equals(action)) {
            String idStr = request.getParameter("id");
            String roleIdStr = request.getParameter("roleId");

            try {
                if (idStr != null && roleIdStr != null) {
                    int id = Integer.parseInt(idStr.trim());
                    int roleId = Integer.parseInt(roleIdStr.trim());

                    Employee currentEmp = dao.getEmployeeById(id);
                    if (currentEmp != null && "System Admin".equalsIgnoreCase(currentEmp.getRoleName())) {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?error=cannotChangeAdminRoot");
                        return;
                    }
                    if (roleId == 1) {
                        boolean hasAnotherAdmin = dao.checkAdminExistsExcept(id);
                        if (hasAnotherAdmin) {
                            response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateDuplicateAdmin");
                            return;
                        }
                    }

                    boolean isUpdated = dao.updateEmployeeRole(id, roleId);
                    if (isUpdated) {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?success=updated");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed");
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/employees");
    }
}
