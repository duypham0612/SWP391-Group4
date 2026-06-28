package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.cashier.CashierShiftService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;

/** C1 · CashierShiftServlet → /cashier/shift. open | close | report. */
@WebServlet("/cashier/shift")
public class CashierShiftServlet extends HttpServlet {

    private final CashierShiftService service = new CashierShiftService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User u = SessionUtil.currentUser(req);
        int cashierId = u != null ? u.getUserId() : 0;
        try {
            String action = req.getParameter("action");
            if ("report".equals(action) && req.getParameter("shiftId") != null) {
                req.setAttribute("shift", service.getShiftReport(Integer.parseInt(req.getParameter("shiftId"))));
            } else {
                req.setAttribute("current", service.getCurrentShift(cashierId));
            }
            req.setAttribute("history", service.getShiftList(InventoryDashboardServlet.branchId(req)));
            req.setAttribute("pageTitle", "Ca thu ngân");
            req.getRequestDispatcher("/WEB-INF/views/cashier/shift.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        int cashierId = u != null ? u.getUserId() : 0;
        int branchId = InventoryDashboardServlet.branchId(req);
        String action = req.getParameter("action");
        String ctx = req.getContextPath();
        try {
            if ("open".equals(action)) {
                BigDecimal opening = parseMoney(req.getParameter("openingCash"));
                service.openShift(branchId, cashierId, opening);
                resp.sendRedirect(ctx + "/cashier/shift");
            } else if ("close".equals(action)) {
                int shiftId = Integer.parseInt(req.getParameter("shiftId"));
                BigDecimal closing = parseMoney(req.getParameter("closingCash"));
                service.closeShift(shiftId, closing);
                resp.sendRedirect(ctx + "/cashier/shift?action=report&shiftId=" + shiftId);
            } else {
                resp.sendRedirect(ctx + "/cashier/shift");
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    private BigDecimal parseMoney(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}
