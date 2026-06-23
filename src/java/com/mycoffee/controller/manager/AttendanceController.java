package com.mycoffee.controller.manager;

import com.mycoffee.dao.AttendanceDAO;
import com.mycoffee.model.Attendance;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@jakarta.servlet.annotation.WebServlet(name = "AttendanceController", urlPatterns = {"/manager-attendance"})
public class AttendanceController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // AuthFilter đã lo phần login. Ở đây check role: chỉ Admin/Manager mới được xem
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        com.mycoffee.model.User user = (session != null) ? (com.mycoffee.model.User) session.getAttribute("user") : null;
        int roleId = (user != null) ? user.getRoleId() : 0;

        if (roleId != 1 && roleId != 2) {
            response.sendRedirect(request.getContextPath() + "/pos-tables");
            return;
        }

        AttendanceDAO dao = new AttendanceDAO();
        // Tạm thời fix chi nhánh = 1. Sau này lấy theo bảng Employees
        int branchId = 1; 

        List<Attendance> attendanceList = dao.getTodayAttendanceByBranch(branchId);
        
        request.setAttribute("danhSachChamCong", attendanceList);
        request.getRequestDispatcher("/views/manager/manager_attendance.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}