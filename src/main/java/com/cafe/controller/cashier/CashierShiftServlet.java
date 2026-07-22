package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.BusinessDay;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.cashier.CashierDutyService;
import com.cafe.service.cashier.CashierShiftService;
import com.cafe.service.manager.AttendanceService;
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
    private final CashierDutyService dutyService = new CashierDutyService();
    private final AttendanceService attendanceService = new AttendanceService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User u = SessionUtil.currentUser(req);
        int cashierId = u != null ? u.getUserId() : 0;
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            String action = req.getParameter("action");
            if ("report".equals(action) && req.getParameter("shiftId") != null) {
                req.setAttribute("shift", service.getShiftReport(Integer.parseInt(req.getParameter("shiftId"))));
            }
            req.setAttribute("current", service.getCurrentShift(cashierId));
            req.setAttribute("dutyState", dutyService.getDutyState(cashierId, branchId).name());
            req.setAttribute("history", service.getShiftList(branchId));
            req.setAttribute("todayRevenue", service.getTodayRevenue(branchId));      // R1
            req.setAttribute("todayBillCount", service.getTodayBillCount(branchId));  // R1
            if (u != null) {
                req.setAttribute("clockStatus", attendanceService.getMyShiftStatus(u.getUserId(), branchId, BusinessDay.todayVn()));
            }
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
            if ("startDuty".equals(action)) {
                BigDecimal opening = parseMoney(req.getParameter("openingCash"));
                dutyService.startDuty(cashierId, branchId, opening);
                req.getSession().setAttribute("flashOk", "Đã bắt đầu ca trực.");
                resp.sendRedirect(ctx + "/cashier/shift");
            } else if ("closeDuty".equals(action)) {
                int shiftId = Integer.parseInt(req.getParameter("shiftId"));
                BigDecimal closing = parseMoney(req.getParameter("closingCash"));
                dutyService.closeDuty(cashierId, branchId, shiftId, closing);
                req.getSession().setAttribute("flashOk", "Đã kết ca.");
                resp.sendRedirect(ctx + "/cashier/shift?action=report&shiftId=" + shiftId);
            } else if ("clockIn".equals(action)) {
                attendanceService.clockIn(cashierId, branchId);
                req.getSession().setAttribute("flashOk", "Đã vào ca.");
                resp.sendRedirect(ctx + "/cashier/shift");
            } else if ("clockOut".equals(action)) {
                attendanceService.clockOut(cashierId, branchId);
                req.getSession().setAttribute("flashOk", "Đã tan ca.");
                resp.sendRedirect(ctx + "/cashier/shift");
            } else if ("open".equals(action)) {
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
        } catch (IllegalStateException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(ctx + "/cashier/shift");
        } catch (Exception e) { throw new ServletException(e); }
    }

    private BigDecimal parseMoney(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}
