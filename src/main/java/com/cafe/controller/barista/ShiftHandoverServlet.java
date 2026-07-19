package com.cafe.controller.barista;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.controller.manager.InventoryDashboardServlet;
import com.cafe.model.User;
import com.cafe.service.barista.HandoverService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** B7 · ShiftHandoverServlet -> /barista/handover. Ghi chú bàn giao ca + số liệu pha hôm nay của cả quán. */
@WebServlet("/barista/handover")
public class ShiftHandoverServlet extends HttpServlet {

    private static final String SELF = "/barista/handover";
    private final HandoverService service = new HandoverService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            // Thanh tan ca ở cuối màn: clockPostUrl trỏ sang /barista/shift vì MyShiftServlet
            // mới là nơi xử lý chấm công. Màn này không tự nhận clockIn/clockOut.
            BaristaShift.expose(req, MyShiftServlet.PATH);
            req.setAttribute("handovers", service.getHandovers(branchId));
            req.setAttribute("kpi", service.getKpi(branchId));
            String brewQuery = textParam(req, "q", 100);
            String brewStatus = allowedParam(req, "status", "READY", "PICKED_UP", "SERVED");
            String brewOrderType = allowedParam(req, "orderType", "DINE_IN", "TAKEAWAY", "DELIVERY");
            int brewPageSize = pageSizeParam(req);
            int requestedBrewPage = positiveIntParam(req, "page", 1);
            HandoverService.BrewHistoryPage brewHistoryPage = service.getBrewHistoryPage(branchId, brewQuery,
                    brewStatus, brewOrderType, requestedBrewPage, brewPageSize);
            req.setAttribute("brewHistory", brewHistoryPage.getItems());
            req.setAttribute("brewHistoryPage", brewHistoryPage);
            req.setAttribute("brewHistoryQuery", brewQuery);
            req.setAttribute("brewHistoryStatus", brewStatus);
            req.setAttribute("brewHistoryOrderType", brewOrderType);
            req.setAttribute("pageTitle", "Bàn giao ca");
            req.getRequestDispatcher("/WEB-INF/views/barista/handover.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        int userId = u != null ? u.getUserId() : 0;
        try {
            if ("create".equals(req.getParameter("action"))) {
                String note = req.getParameter("note");
                if (note != null && !note.isBlank()) {
                    service.createHandover(branchId, note.trim(), userId);
                } else {
                    req.getSession().setAttribute("flashError", "Nội dung bàn giao không được rỗng.");
                }
            }
            resp.sendRedirect(req.getContextPath() + SELF);
        } catch (Exception e) { throw new ServletException(e); }
    }

    private static String textParam(HttpServletRequest req, String name, int maxLength) {
        String value = req.getParameter(name);
        if (value == null) return "";
        value = value.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String allowedParam(HttpServletRequest req, String name, String... allowed) {
        String value = textParam(req, name, 20);
        for (String item : allowed) if (item.equals(value)) return value;
        return "";
    }

    private static int positiveIntParam(HttpServletRequest req, String name, int fallback) {
        try {
            int value = Integer.parseInt(req.getParameter(name));
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int pageSizeParam(HttpServletRequest req) {
        int value = positiveIntParam(req, "pageSize", 10);
        return value == 20 || value == 50 ? value : 10;
    }
}
