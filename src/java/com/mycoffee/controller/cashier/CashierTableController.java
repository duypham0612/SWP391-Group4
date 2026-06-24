package com.mycoffee.controller.cashier;

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

@WebServlet(name = "CashierTableController", urlPatterns = {"/cashier-tables"})
public class CashierTableController extends HttpServlet {

    private TableReservationDAO dao = new TableReservationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        com.mycoffee.model.User user = (session != null) ? (com.mycoffee.model.User) session.getAttribute("user") : null;
        if (user == null || user.getRoleId() != 3) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Integer branchId = (session != null) ? (Integer) session.getAttribute("branchId") : null;
        if (branchId == null) branchId = 1;

        String action = request.getParameter("action");

        if ("quickOpen".equals(action)) {
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            dao.updateTableStatus(tableId, "Serving");
            response.sendRedirect(request.getContextPath() + "/cashier-tables");
            return;
        }

        if ("quickFree".equals(action)) {
            int tableId = Integer.parseInt(request.getParameter("tableId"));
            dao.updateTableStatus(tableId, "Empty");
            response.sendRedirect(request.getContextPath() + "/cashier-tables");
            return;
        }

        List<Table> tableList = dao.getTablesByBranch(branchId);
        List<Reservation> activeReservations = dao.getTodayReservations(branchId);

        request.setAttribute("tableList", tableList);
        request.setAttribute("activeReservations", activeReservations);

        request.getRequestDispatcher("/views/cashier/cashier_tables.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        if ("add".equals(action)) {
            String customerName = request.getParameter("customerName");
            String phone = request.getParameter("phone");
            String tableIdStr = request.getParameter("tableId");
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
                } catch (Exception e) {}
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

        response.sendRedirect(request.getContextPath() + "/cashier-tables");
    }
}