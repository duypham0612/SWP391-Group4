package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.service.cashier.CashierShiftService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * R2 · CashierDashboardServlet → /cashier/dashboard.
 * Bảng điều khiển thu ngân: doanh thu hôm nay + số đơn đã thực hiện (số HĐ đã thu).
 */
@WebServlet("/cashier/dashboard")
public class CashierDashboardServlet extends HttpServlet {

    private final CashierShiftService service = new CashierShiftService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("todayRevenue", service.getTodayRevenue(branchId));
            req.setAttribute("todayBillCount", service.getTodayBillCount(branchId));
            req.setAttribute("pageTitle", "Bảng điều khiển");
            req.getRequestDispatcher("/WEB-INF/views/cashier/dashboard.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }
}
