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

        HttpSession session = request.getSession(false);
        com.mycoffee.model.User user = (session != null) ? (com.mycoffee.model.User) session.getAttribute("user") : null;
        int roleId = (user != null) ? user.getRoleId() : 0;

        // Chỉ Admin và Manager được phép vào trang này
        if (roleId != 1 && roleId != 2) {
            response.sendRedirect(request.getContextPath() + "/pos-tables");
            return;
        }

        Integer branchId = (session != null) ? (Integer) session.getAttribute("branchId") : null;
        if (branchId == null) {
            branchId = 1; // Fallback chạy thử nghiệm
        }
        String action = request.getParameter("action");

        if ("quickOpen".equals(action)) {
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            dao.updateTableStatus(tableId, "Serving");

            // SỬA: Redirect về URL Servlet một cách an toàn
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

        if ("add".equals(action)) {
            String customerName = request.getParameter("customerName");
            String phone = request.getParameter("phone");

            // ĐỌC DỮ LIỆU DƯỚI DẠNG CHUỖI TRƯỚC
            String tableIdStr = request.getParameter("tableId");
            String resTimeStr = request.getParameter("resTime");

            // BẪY LỖI: Kiểm tra xem các trường quan trọng có bị rỗng hay không
            if (tableIdStr != null && !tableIdStr.trim().isEmpty() && resTimeStr != null && !resTimeStr.trim().isEmpty()) {
                try {
                    int tableId = Integer.parseInt(tableIdStr); // Lúc này mới ép kiểu an toàn
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
                    e.printStackTrace(); // Bắt các lỗi định dạng ngày tháng hoặc ép số nếu có
                }
            }

        } else if ("checkin".equals(action)) {
            String resIdStr = request.getParameter("resId");
            String tableIdStr = request.getParameter("tableId");

            if (resIdStr != null && !resIdStr.isEmpty() && tableIdStr != null && !tableIdStr.isEmpty()) {
                int resId = Integer.parseInt(resIdStr);
                int tableId = Integer.parseInt(tableIdStr);

                dao.updateReservationStatus(resId, "Seated");
                dao.updateTableStatus(tableId, "Serving");
            }
        }

        response.sendRedirect(request.getContextPath() + "/manager-tables");
    }
}
