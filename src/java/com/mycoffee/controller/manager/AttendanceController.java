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
        
        AttendanceDAO dao = new AttendanceDAO();
        int branchId = 1; // Mặc định chi nhánh 1
        
        List<Attendance> attendanceList = dao.getTodayAttendanceByBranch(branchId);
        
        request.setAttribute("danhSachChamCong", attendanceList);
        request.getRequestDispatcher("manager_attendance.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}