package com.mycoffee.controller.manager;

import com.mycoffee.dao.AttendanceDAO;
import com.mycoffee.model.Attendance;
import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession; // Bổ sung import HttpSession để xử lý ca/chi nhánh động

@jakarta.servlet.annotation.WebServlet(name = "AttendanceController", urlPatterns = {"/manager-attendance"})
public class AttendanceController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        AttendanceDAO dao = new AttendanceDAO();
        
        // TỐI ƯU: Lấy mã chi nhánh động từ Session của Quản lý đang đăng nhập
        HttpSession session = request.getSession();
        int branchId = 1; // Mặc định dự phòng chi nhánh 1 (Cầu Giấy) nếu không tìm thấy session
        
        // Cách 1: Nếu bạn lưu trực tiếp thuộc tính "branchId" vào session khi login thành công
        if (session.getAttribute("branchId") != null) {
            branchId = (int) session.getAttribute("branchId");
        }
        /* // Cách 2: Nếu bạn lưu cả object User vào session, hãy mở đoạn comment này ra và sử dụng:
        else if (session.getAttribute("user") != null) {
            com.mycoffee.model.User loginUser = (com.mycoffee.model.User) session.getAttribute("user");
            // Giả định model User của bạn có phương thức getBranchId() kết nối từ bảng Employees sang
            branchId = loginUser.getBranchId(); 
        }
        */
        
        List<Attendance> attendanceList = dao.getTodayAttendanceByBranch(branchId);
        
        // Đẩy danh sách sang file JSP dưới tên biến "danhSachChamCong"
        request.setAttribute("danhSachChamCong", attendanceList);
        request.getRequestDispatcher("manager_attendance.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // 1. Cấu hình nhận tiếng Việt từ form (tránh lỗi font chữ khi sửa trạng thái)
        request.setCharacterEncoding("UTF-8");
        
        // 2. Lấy hành động (action) được gửi từ giao diện JSP lên
        String action = request.getParameter("action");
        AttendanceDAO dao = new AttendanceDAO();
        
        try {
            if ("checkin".equals(action)) {
                // CHỨC NĂNG: Thêm lượt chấm công (CREATE)
                int empId = Integer.parseInt(request.getParameter("employeeId"));
                int shiftId = Integer.parseInt(request.getParameter("shiftId"));
                // Trạng thái mặc định ban đầu là "Đang làm việc"
                dao.insertAttendance(empId, shiftId, "Đang làm việc");
                
            } else if ("checkout".equals(action)) {
                // CHỨC NĂNG BỔ SUNG: Nhân viên/Quản lý bấm nút Check-out kết thúc ca nhanh (UPDATE GIỜ RA)
                int attId = Integer.parseInt(request.getParameter("attendanceId"));
                dao.checkOut(attId);
                
            } else if ("update".equals(action)) {
                // CHỨC NĂNG: Sửa trạng thái tùy chọn từ Modal (UPDATE)
                int attId = Integer.parseInt(request.getParameter("attendanceId"));
                String status = request.getParameter("status");
                dao.updateStatus(attId, status);
                
            } else if ("delete".equals(action)) {
                // CHỨC NĂNG: Xóa bản ghi lỗi (DELETE)
                int attId = Integer.parseInt(request.getParameter("attendanceId"));
                dao.deleteAttendance(attId);
            }
        } catch (Exception e) {
            System.out.println("Loi xu ly dữ liệu trong AttendanceController: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 3. Sau khi xử lý xong (Thêm/Sửa/Xóa/Check-out), dùng sendRedirect để load lại trang qua hàm doGet
        response.sendRedirect("manager-attendance");
    }
}
