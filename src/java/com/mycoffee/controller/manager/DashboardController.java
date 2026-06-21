package com.mycoffee.controller.manager;

import com.mycoffee.dao.AttendanceDAO;
import com.mycoffee.dao.InventoryDAO;
import com.mycoffee.model.Attendance;
import com.mycoffee.model.Inventory;
import com.mycoffee.model.User;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "DashboardController", urlPatterns = {"/manager-dashboard"})
public class DashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // AuthFilter đã chặn Customer (RoleID=4). Kiểm tra thêm chỉ Admin hoặc Manager được vào.
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        int roleId = (user != null) ? user.getRoleId() : 0;

        if (roleId != 1 && roleId != 2) {
            // Employee (RoleID=3) bị chặn khỏi trang này — chuyển sang màn hình POS
            response.sendRedirect(request.getContextPath() + "/pos-tables");
            return;
        }

        // Mặc định lấy chi nhánh 1 — sau này lấy BranchID từ bảng Employees theo user
        int branchId = 1;

        InventoryDAO inventoryDAO = new InventoryDAO();
        AttendanceDAO attendanceDAO = new AttendanceDAO();

        List<Inventory> inventoryList = inventoryDAO.getInventoryByBranch(branchId);
        List<Attendance> attendanceList = attendanceDAO.getTodayAttendanceByBranch(branchId);

        // Tính số nguyên liệu sắp hết hàng (Quantity < MinRequired)
        int lowStockCount = 0;
        for (Inventory item : inventoryList) {
            if (item.getQuantity() < item.getMinRequired()) {
                lowStockCount++;
            }
        }

        request.setAttribute("lowStockCount", lowStockCount);
        request.setAttribute("workingCount", attendanceList.size());
        request.setAttribute("todayAttendance", attendanceList);

        request.getRequestDispatcher("/views/manager/manager_dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
