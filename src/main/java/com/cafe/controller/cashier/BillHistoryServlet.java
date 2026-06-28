package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.service.cashier.BillingService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** C6 · BillHistoryServlet → /cashier/history. list | view | void. */
@WebServlet("/cashier/history")
public class BillHistoryServlet extends HttpServlet {

    private final BillingService service = new BillingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("view".equals(req.getParameter("action")) && req.getParameter("billId") != null) {
                req.setAttribute("bill", service.getBill(Integer.parseInt(req.getParameter("billId"))));
                req.setAttribute("pageTitle", "Chi tiết hoá đơn");
                req.getRequestDispatcher("/WEB-INF/views/cashier/bill-view.jsp").forward(req, resp);
            } else {
                req.setAttribute("bills", service.getBillHistory(branchId));
                req.setAttribute("pageTitle", "Lịch sử hoá đơn");
                req.getRequestDispatcher("/WEB-INF/views/cashier/bill-history.jsp").forward(req, resp);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        try {
            if ("void".equals(req.getParameter("action"))) {
                service.voidBill(Integer.parseInt(req.getParameter("billId")));
            }
            resp.sendRedirect(req.getContextPath() + "/cashier/history");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
