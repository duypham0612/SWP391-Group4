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
        
        String action = request.getParameter("action");

        if ("quickOpen".equals(action)) {
            String tableIdStr = request.getParameter("tableId");
            if (tableIdStr != null && !tableIdStr.isEmpty()) {
                try {
                    int tableId = Integer.parseInt(tableIdStr);
                    dao.updateTableStatus(tableId, "Serving");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
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
        
        HttpSession session = request.getSession(false);
        Integer branchId = (session != null) ? (Integer) session.getAttribute("branchId") : null;
        if (branchId == null) {
            branchId = 1; // Fallback an toàn
        }

        String action = request.getParameter("action");

        if ("add".equals(action)) {
            String customerName = request.getParameter("customerName");
            String phone = request.getParameter("phone");
            String tableIdStr = request.getParameter("tableId");
            String resTimeStr = request.getParameter("resTime");

            if (tableIdStr != null && !tableIdStr.trim().isEmpty() && resTimeStr != null && !resTimeStr.trim().isEmpty()) {
                try {
                    int tableId = Integer.parseInt(tableIdStr);
                    
                    // Xử lý đọc DateTime từ datetime-local một cách chuẩn xác, chống crash lỗi định dạng
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
                    res.setBranchID(branchId); // Tối ưu: Lấy động theo chi nhánh của Manager đang đăng nhập
                    res.setCustomerName(customerName);
                    res.setPhone(phone);
                    res.setTableID(tableId);
                    res.setReservationTime(resTime);
                    res.setStatus("Pending");

                    dao.createNewReservation(res);
                    dao.updateTableStatus(tableId, "Reserved");
                } catch (Exception e) {
                    // Ghi log lỗi để debug thay vì làm sập ứng dụng công cộng
                    getServletContext().log("Lỗi định dạng dữ liệu khi đặt bàn: ", e);
                }
            }

        } else if ("checkin".equals(action)) {
            String resIdStr = request.getParameter("resId");
            String tableIdStr = request.getParameter("tableId");

            if (resIdStr != null && !resIdStr.isEmpty() && tableIdStr != null && !tableIdStr.isEmpty()) {
                try {
                    int resId = Integer.parseInt(resIdStr);
                    int tableId = Integer.parseInt(tableIdStr);

                    dao.updateReservationStatus(resId, "Seated");
                    dao.updateTableStatus(tableId, "Serving");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/manager-tables");
    }
}