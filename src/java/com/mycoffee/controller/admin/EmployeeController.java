package com.mycoffee.controller.admin;

import com.mycoffee.dao.EmployeeDAO;
import com.mycoffee.model.Employee;
import com.mycoffee.model.Role;
import com.mycoffee.model.Shift;
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

        // Lấy trang hiện tại để tái sử dụng khi redirect sau các thao tác GET (như delete, toggle)
        String pageParamForRedirect = request.getParameter("page");
        String pageSuffix = (pageParamForRedirect != null && !pageParamForRedirect.isEmpty()) ? "&page=" + pageParamForRedirect : "";

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
                    response.sendRedirect(request.getContextPath() + "/admin/employees" + (pageSuffix.isEmpty() ? "" : "?" + pageSuffix.substring(1)));
                }
                break;

            case "delete":
                try {
                    int id = Integer.parseInt(request.getParameter("id"));
                    Employee empToDelete = dao.getEmployeeById(id);

                    if (empToDelete != null && empToDelete.getRoleId() == 1) {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?error=cannotDeleteAdmin" + pageSuffix);
                        return;
                    }

                    boolean isDeleted = dao.deleteEmployee(id);
                    if (isDeleted) {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?success=delete" + pageSuffix);
                    } else {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?error=deleteFailed" + pageSuffix);
                    }
                } catch (Exception e) {
                    response.sendRedirect(request.getContextPath() + "/admin/employees?error=system" + pageSuffix);
                }
                break;
                
            case "toggle":
                try {
                    int idToggle = Integer.parseInt(request.getParameter("id"));
                    dao.toggleStatus(idToggle);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                response.sendRedirect(request.getContextPath() + "/admin/employees" + (pageSuffix.isEmpty() ? "" : "?" + pageSuffix.substring(1)));
                break;

            default: // ĐÂY LÀ NƠI XỬ LÝ ACTION "list" HIỂN THỊ CHÍNH ĐÃ ĐƯỢC PHÂN TRANG
                int pageSize = 10; // 1 trang có 10 nhân viên
                int currentPage = 1;

                // Đọc tham số trang hiện tại từ URL query (?page=X)
                String pageParam = request.getParameter("page");
                if (pageParam != null && !pageParam.isEmpty()) {
                    try {
                        currentPage = Integer.parseInt(pageParam);
                        if (currentPage < 1) currentPage = 1;
                    } catch (NumberFormatException e) {
                        currentPage = 1;
                    }
                }

                // Lấy tổng số dòng để thực hiện tính toán số trang
                int totalEmployees = dao.getTotalEmployeeCount();
                int totalPages = (int) Math.ceil((double) totalEmployees / pageSize);
                if (totalPages < 1) totalPages = 1;

                // KHỐNG CHẾ YÊU CẦU: Tối đa chỉ có thể tạo hoặc xem đến 999 trang
                if (totalPages > 999) {
                    totalPages = 999;
                }

                // Bảo vệ hệ thống nếu trang yêu cầu vượt quá giới hạn trang cho phép
                if (currentPage > totalPages) {
                    currentPage = totalPages;
                }

                // Thực hiện gọi hàm lấy danh sách giới hạn 10 dòng từ DB lên thay vì lấy hết
                List<Employee> employeeList = dao.getEmployeesByPage(currentPage, pageSize);
                List<Role> roleList = dao.getAllRoles();
                List<Shift> shiftList = dao.getAllShifts();
                
                // Gửi danh sách dữ liệu sang JSP
                request.setAttribute("employees", employeeList);
                request.setAttribute("roles", roleList);
                request.setAttribute("shifts", shiftList); 
                
                // ĐẨY THÊM CÁC THÔNG SỐ ĐIỀU HƯỚNG PHÂN TRANG SANG FILE JSP
                request.setAttribute("currentPage", currentPage);
                request.setAttribute("totalPages", totalPages);
                request.setAttribute("totalEmployees", totalEmployees);
                request.setAttribute("pageSize", pageSize);
                
                request.getRequestDispatcher("/admin/employee-management.jsp").forward(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        
        // Đọc số trang hiện tại gửi ngầm từ form (nếu có) để quay lại đúng trang cũ
        String pageParam = request.getParameter("page");
        String pageSuffix = (pageParam != null && !pageParam.isEmpty()) ? "&page=" + pageParam : "";

        if ("quickShift".equals(action)) {
            String idStr = request.getParameter("id");
            String shiftIdStr = request.getParameter("shiftId");
            try {
                if (idStr != null && shiftIdStr != null) {
                    int id = Integer.parseInt(idStr.trim());
                    int shiftId = Integer.parseInt(shiftIdStr.trim());

                    boolean isUpdated = dao.updateEmployeeShift(id, shiftId);
                    if (isUpdated) {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?success=updated" + pageSuffix);
                    } else {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed" + pageSuffix);
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed" + pageSuffix);
                return;
            }
        }

        if ("insert".equals(action) || "insert ".equals(action)) { 
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

                if (currentEmp != null && "admin".equalsIgnoreCase(currentEmp.getUsername())) {
                    if (roleId != 1 || !isActive) {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?error=cannotChangeAdminRoot");
                        return;
                    }
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

        if ("quickRole".equals(action)) {
            String idStr = request.getParameter("id");
            String roleIdStr = request.getParameter("roleId");

            try {
                if (idStr != null && roleIdStr != null) {
                    int id = Integer.parseInt(idStr.trim());
                    int roleId = Integer.parseInt(roleIdStr.trim());

                    Employee currentEmp = dao.getEmployeeById(id);
                    if (currentEmp != null && "System Admin".equalsIgnoreCase(currentEmp.getRoleName())) {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?error=cannotChangeAdminRoot" + pageSuffix);
                        return;
                    }
                    if (roleId == 1) {
                        boolean hasAnotherAdmin = dao.checkAdminExistsExcept(id);
                        if (hasAnotherAdmin) {
                            response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateDuplicateAdmin" + pageSuffix);
                            return;
                        }
                    }

                    boolean isUpdated = dao.updateEmployeeRole(id, roleId);
                    if (isUpdated) {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?success=updated" + pageSuffix);
                    } else {
                        response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed" + pageSuffix);
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath() + "/admin/employees?error=updateFailed" + pageSuffix);
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/employees");
    }
}
