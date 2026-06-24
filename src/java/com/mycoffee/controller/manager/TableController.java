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

@WebServlet(name = "TableController", urlPatterns = {"/manager-tables"})
public class TableController extends HttpServlet {

    private TableReservationDAO dao = new TableReservationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        Integer branchId = (Integer) session.getAttribute("branchId");
        if (branchId == null) {
            branchId = 1; // Mặc định chạy thử nghiệm chi nhánh 1 nếu chưa login/chưa chọn branch
        }
        String action = request.getParameter("action");

        // Đồng bộ hành động quickOpen từ thẻ <a> (Chuyển đổi trạng thái sang Occupied)
        if ("quickOpen".equals(action)) {
            String tableIdStr = request.getParameter("tableId");
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                int tableId = Integer.parseInt(tableIdStr);
                dao.updateTableStatus(tableId, "Occupied");
            }
            response.sendRedirect(request.getContextPath() + "/manager-tables");
            return;
        }

        try {
            // Lấy danh sách bàn và lịch đặt chỗ trong ngày
            List<Table> tableList = dao.getTablesByBranch(branchId);
            List<Reservation> activeReservations = dao.getTodayReservations(branchId);

            // Gửi dữ liệu sang trang JSP với tên thuộc tính thống nhất "tableList"
            request.setAttribute("tableList", tableList);
            request.setAttribute("activeReservations", activeReservations);

            // Điều hướng an toàn sang file giao diện
            request.getRequestDispatcher("/views/manager/manager_tables.jsp").forward(request, response);
        } catch (Exception e) {
            // In lỗi chi tiết ra tab Output của NetBeans để kiểm tra nếu SQL hoặc kết nối DB bị sập
            e.printStackTrace();
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<h3>Hệ thống sơ đồ bàn đang gặp lỗi dữ liệu (DAO/SQL):</h3>");
            response.getWriter().println("<p style='color:red'>" + e.getMessage() + "</p>");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        String tableIdStr = request.getParameter("tableId");

        // Xử lý các hành động gửi lên từ Form ẩn (hoặc modal popup)
        if ("cancelReservation".equals(action)) {
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                int tableId = Integer.parseInt(tableIdStr);
                dao.updateTableStatus(tableId, "Empty");
            }

        } else if ("resetTable".equals(action)) {
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                int tableId = Integer.parseInt(tableIdStr);
                dao.updateTableStatus(tableId, "Empty");
            }

        } else if ("quickOpen".equals(action)) {
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                int tableId = Integer.parseInt(tableIdStr);
                dao.updateTableStatus(tableId, "Occupied"); // Sử dụng từ khóa Occupied đồng bộ
            }

        } else if ("add".equals(action)) {
            String customerName = request.getParameter("customerName");
            String phone = request.getParameter("phone");
            String resTimeStr = request.getParameter("resTime");

            if (tableIdStr != null && !tableIdStr.trim().isEmpty() && resTimeStr != null && !resTimeStr.trim().isEmpty()) {
                try {
                    int tableId = Integer.parseInt(tableIdStr);
                    
                    // Chuẩn hóa chuỗi thời gian nhận từ datetime-local sang chuẩn Timestamp của SQL Server
                    String formattedDateTime = resTimeStr.replace("T", " ");
                    if (formattedDateTime.length() == 16) { 
                        formattedDateTime += ":00"; // Tự động bù giây nếu trình duyệt cắt bớt
                    }
                    Timestamp resTime = Timestamp.valueOf(formattedDateTime);

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
            String resIdStr = request.getParameter("resId");

            if (resIdStr != null && !resIdStr.isEmpty() && tableIdStr != null && !tableIdStr.isEmpty()) {
                int resId = Integer.parseInt(resIdStr);
                int tableId = Integer.parseInt(tableIdStr);

                dao.updateReservationStatus(resId, "Seated");
                dao.updateTableStatus(tableId, "Occupied"); // Đưa trạng thái bàn về hoạt động Occupied
            }
        }

        // Kỹ thuật Anti-F5: Chuyển hướng Redirect sau khi kết thúc yêu cầu POST để tránh nhân bản dữ liệu
        response.sendRedirect(request.getContextPath() + "/manager-tables");
    }
}