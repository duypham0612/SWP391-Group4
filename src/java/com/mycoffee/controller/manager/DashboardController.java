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
        
        // 1. Kiểm tra quyền đăng nhập
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        // 2. Mặc định lấy chi nhánh của quản lý (Giả sử BranchID = 1 nếu chưa gán cứng)
        int branchId = 1; 

        // 3. Khởi tạo DAO và truy vấn dữ liệu cho Dashboard
        InventoryDAO inventoryDAO = new InventoryDAO();
        AttendanceDAO attendanceDAO = new AttendanceDAO();

        List<Inventory> inventoryList = inventoryDAO.getInventoryByBranch(branchId);
        List<Attendance> attendanceList = attendanceDAO.getTodayAttendanceByBranch(branchId);

        // Tính toán số lượng nguyên liệu sắp hết hàng (Quantity < MinRequired)
        int lowStockCount = 0;
        for (Inventory item : inventoryList) {
            if (item.getQuantity() < item.getMinRequired()) {
                lowStockCount++;
            }
        }

        // 4. Gửi dữ liệu sang trang JSP
        request.setAttribute("lowStockCount", lowStockCount);
        request.setAttribute("workingCount", attendanceList.size());
        request.setAttribute("todayAttendance", attendanceList);
        
        // Forward sang trang giao diện manager_dashboard.jsp
        request.getRequestDispatcher("/views/manager/manager_dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
