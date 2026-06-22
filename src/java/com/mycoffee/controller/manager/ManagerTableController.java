package com.mycoffee.controller.manager;

import com.mycoffee.dao.TableReservationDAO;
import com.mycoffee.model.Table;
import com.mycoffee.model.Reservation;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;

@WebServlet(name = "ManagerTableController", urlPatterns = {"/manager-tables"})
public class ManagerTableController extends HttpServlet {

    private TableReservationDAO dao = new TableReservationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        Integer branchId = (Integer) session.getAttribute("branchId");
        if (branchId == null) {
            branchId = 1; // Fallback chạy thử nghiệm
        }
        String action = request.getParameter("action");

        // Giữ lại quickOpen ở doGet nếu bạn có đường link thẻ <a> nào sử dụng query string trực tiếp
        if ("quickOpen".equals(action)) {
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            dao.updateTableStatus(tableId, "Serving");

            response.sendRedirect(request.getContextPath() + "/manager-tables");
            return;
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
        String action = request.getParameter("action");
        String tableIdStr = request.getParameter("tableId");

        // BẮT CÁC HÀNH ĐỘNG TỪ FORM ẨN (hiddenActionForm) GỬI LÊN
        if ("cancelReservation".equals(action)) {
            // 1. XỬ LÝ HỦY ĐẶT TRƯỚC: Chuyển trạng thái bàn về 'Empty'
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                int tableId = Integer.parseInt(tableIdStr);
                
                // Cập nhật trạng thái bàn về trống
                dao.updateTableStatus(tableId, "Empty");
                
                // (Tùy chọn bổ sung): Nếu trong DB của bạn cần cập nhật phiếu đặt lịch hẹn đó thành 'Cancelled'
                // Bạn có thể viết thêm hàm dưới đây trong TableReservationDAO nếu cần quản lý lịch sử:
                // dao.cancelActiveReservationByTable(tableId);
            }

        } else if ("resetTable".equals(action)) {
            // 2. XỬ LÝ DỌN BÀN & TRẢ VỀ TRỐNG: Chuyển trạng thái bàn về 'Empty'
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                int tableId = Integer.parseInt(tableIdStr);
                
                // Cập nhật trạng thái bàn về trống để đón khách tiếp theo
                dao.updateTableStatus(tableId, "Empty");
            }

        } else if ("quickOpen".equals(action)) {
            // 3. XỬ LÝ MỞ BÀN NGAY (Khi nút bấm submit qua form ẩn POST)
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                int tableId = Integer.parseInt(tableIdStr);
                dao.updateTableStatus(tableId, "Serving");
            }

        } else if ("add".equals(action)) {
            // 4. XỬ LÝ TẠO PHIẾU ĐẶT TRƯỚC (Giữ nguyên logic của bạn)
            String customerName = request.getParameter("customerName");
            String phone = request.getParameter("phone");
            String resTimeStr = request.getParameter("resTime");

            if (tableIdStr != null && !tableIdStr.trim().isEmpty() && resTimeStr != null && !resTimeStr.trim().isEmpty()) {
                try {
                    int tableId = Integer.parseInt(tableIdStr);
                    Timestamp resTime = Timestamp.valueOf(resTimeStr.replace("T", " ") + ":00");

                    Reservation res = new Reservation();
                    res.setBranchID(1);
                    res.setCustomerName(customerName);
                    res.setPhone(phone);
                    res.setTableID(tableId);
                    res.setReservationTime(resTime);
                    res.setStatus("Pending");

                    dao.createNewReservation(res);
                    dao.updateTableStatus(tableId, "Reserved");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else if ("checkin".equals(action)) {
            // 5. XỬ LÝ NHẬN BÀN TỪ LỊCH HẸN (Giữ nguyên logic của bạn)
            String resIdStr = request.getParameter("resId");

            if (resIdStr != null && !resIdStr.isEmpty() && tableIdStr != null && !tableIdStr.isEmpty()) {
                int resId = Integer.parseInt(resIdStr);
                int tableId = Integer.parseInt(tableIdStr);

                dao.updateReservationStatus(resId, "Seated");
                dao.updateTableStatus(tableId, "Serving");
            }
        }

        // Tải lại trang sơ đồ bàn sau khi xử lý thành công để hiển thị màu sắc mới
        response.sendRedirect(request.getContextPath() + "/manager-tables");
    }
}