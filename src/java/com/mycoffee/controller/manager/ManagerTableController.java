package com.mycoffee.controller.manager;

import com.mycoffee.dao.TableReservationDAO;
import com.mycoffee.model.Table;
import com.mycoffee.model.Reservation;
import com.mycoffee.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

@WebServlet(name = "ManagerTableController", urlPatterns = {"/manager-tables"})
public class ManagerTableController extends HttpServlet {

    private final TableReservationDAO dao = new TableReservationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        int roleId = (user != null) ? user.getRoleId() : 0;

        // Chỉ Admin (1) và Manager (2) được phép vào trang này
        if (roleId != 1 && roleId != 2) {
            response.sendRedirect(request.getContextPath() + "/pos-tables");
            return;
        }

        Integer branchId = (session != null) ? (Integer) session.getAttribute("branchId") : null;
        if (branchId == null) {
            branchId = 1; // Fallback chạy thử nghiệm nếu session chưa có
        }

        // Đổ dữ liệu lên giao diện
        List<Table> tableList = dao.getTablesByBranch(branchId);
        List<Reservation> activeReservations = dao.getTodayReservations(branchId);

        request.setAttribute("tableList", tableList);
        request.setAttribute("activeReservations", activeReservations);

        request.getRequestDispatcher("/views/manager/manager_tables.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        
        HttpSession session = request.getSession(false);
        Integer branchId = (session != null) ? (Integer) session.getAttribute("branchId") : null;
        if (branchId == null) {
            branchId = 1; // Fallback an toàn
        }

        String action = request.getParameter("action");
        String tableIdStr = request.getParameter("tableId");

        try {
            if (action != null) {
                switch (action) {
                    case "add": // TẠO PHIẾU ĐẶT BÀN TRƯỚC
                        String customerName = request.getParameter("customerName");
                        String phone = request.getParameter("phone");
                        String resTimeStr = request.getParameter("resTime");

                        if (tableIdStr != null && !tableIdStr.trim().isEmpty() && resTimeStr != null && !resTimeStr.trim().isEmpty()) {
                            int tableId = Integer.parseInt(tableIdStr);
                            
                            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                                    .appendPattern("yyyy-MM-dd'T'HH:mm")
                                    .optionalStart()
                                    .appendPattern(":ss")
                                    .optionalEnd()
                                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                                    .toFormatter();
                            
                            LocalDateTime localDateTime = LocalDateTime.parse(resTimeStr, formatter);
                            Timestamp resTime = Timestamp.valueOf(localDateTime);

                            Reservation res = new Reservation();
                            res.setBranchID(branchId);
                            res.setCustomerName(customerName);
                            res.setPhone(phone);
                            res.setTableID(tableId);
                            res.setReservationTime(resTime);
                            res.setStatus("Pending");

                            dao.createNewReservation(res);
                            dao.updateTableStatus(tableId, "Reserved"); // Chuyển sang màu Vàng (Đã đặt)
                        }
                        break;

                    case "checkin": // NHẬN BÀN TỪ LỊCH HẸN
                        String resIdStr = request.getParameter("resId");
                        if (resIdStr != null && tableIdStr != null) {
                            int resId = Integer.parseInt(resIdStr);
                            int tableId = Integer.parseInt(tableIdStr);

                            dao.updateReservationStatus(resId, "Seated");
                            dao.updateTableStatus(tableId, "Serving"); // Chuyển sang màu Đỏ (Đang phục vụ)
                        }
                        break;

                    case "quickOpen": // ✅ NÚT: MỞ BÀN NGAY (Đã chuyển từ doGet xuống doPost đúng kỹ thuật)
                        if (tableIdStr != null && !tableIdStr.isEmpty()) {
                            int tableId = Integer.parseInt(tableIdStr);
                            dao.updateTableStatus(tableId, "Serving"); // Ghi "Serving" để file JSP nhận diện lên màu Đỏ
                        }
                        break;

                    case "resetTable": // ✅ NÚT: DỌN BÀN & TRẢ VỀ TRỐNG
                        if (tableIdStr != null && !tableIdStr.isEmpty()) {
                            int tableId = Integer.parseInt(tableIdStr);
                            dao.updateTableStatus(tableId, "Empty"); // Đưa về trống để lên màu Xanh Lá
                        }
                        break;

                    case "cancelReservation": // ✅ NÚT: HỦY LỊCH ĐẶT TRƯỚC
                        if (tableIdStr != null && !tableIdStr.isEmpty()) {
                            int tableId = Integer.parseInt(tableIdStr);
                            dao.updateTableStatus(tableId, "Empty"); // Trả trạng thái bàn về Trống
                            // Lưu ý: Nếu muốn đổi cả trạng thái phiếu đặt bàn trong DB thành 'Cancelled', bạn có thể gọi thêm hàm cập nhật bảng Reservation tại đây.
                        }
                        break;
                }
            }
        } catch (Exception e) {
            getServletContext().log("Lỗi xử lý hành động tại ManagerTableController: ", e);
            e.printStackTrace();
        }

        // Sau khi xử lý xong xuôi, bắt buộc chuyển hướng trang để load lại giao diện mới
        response.sendRedirect(request.getContextPath() + "/manager-tables");
    }
}