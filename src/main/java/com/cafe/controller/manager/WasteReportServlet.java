package com.cafe.controller.manager;

import com.cafe.common.BusinessDay;
import com.cafe.service.manager.WasteReportService;
import com.cafe.service.shared.InventoryService;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/** M · WasteReportServlet → /manager/waste. Manager chỉ xem nhật ký hao hụt/làm lại. */
@WebServlet("/manager/waste")
public class WasteReportServlet extends HttpServlet {
    private final WasteReportService service = new WasteReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        LocalDate todayVn = LocalDate.now(BusinessDay.VN_ZONE);
        WasteReportService.Range range = WasteReportService.resolveRange(
                req.getParameter("from"), req.getParameter("to"), todayVn);
        String logQuery = textParam(req, "q", 100);
        String logWasteType = allowedParam(req, "wasteType", "SPILL", "EXPIRED", "REMAKE", "OTHER");
        String logStatus = allowedParam(req, "status", "ACTIVE", "VOIDED");
        int requestedLogPage = positiveIntParam(req, "page", 1);

        try {
            InventoryService.WasteLogPage p = service.page(branchId, range,
                    logQuery, logWasteType, logStatus, requestedLogPage, pageSize());
            req.setAttribute("summary", service.summarize(branchId, range));
            req.setAttribute("wasteLogPage", p);
            req.setAttribute("logs", p.getLogs());
            req.setAttribute("range", range);
            req.setAttribute("todayDate", todayVn);
            req.setAttribute("last7FromDate", todayVn.minusDays(6));
            req.setAttribute("last30FromDate", todayVn.minusDays(29));
            req.setAttribute("wasteLogQuery", logQuery);
            req.setAttribute("wasteLogWasteType", logWasteType);
            req.setAttribute("wasteLogStatus", logStatus);
            req.setAttribute("openReviews", service.openReviews(branchId));
            req.setAttribute("pageTitle", "Hao hụt & làm lại");
            req.getRequestDispatcher("/WEB-INF/views/manager/waste.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        if (!"resolveReview".equals(req.getParameter("action"))) { resp.sendError(400); return; }
        User user = SessionUtil.currentUser(req);
        try {
            long id = Long.parseLong(req.getParameter("reviewId"));
            boolean ok = service.resolveReview(InventoryDashboardServlet.branchId(req), id,
                    user == null ? 0 : user.getUserId(), req.getParameter("note"));
            req.getSession().setAttribute(ok ? "flashOk" : "flashError", ok ? "Đã xác nhận ngoại lệ." : "Ngoại lệ đã được xử lý.");
            resp.sendRedirect(req.getContextPath() + "/manager/waste");
        } catch (NumberFormatException e) { resp.sendError(400); }
        catch (Exception e) { throw new ServletException(e); }
    }

    private static String textParam(HttpServletRequest req, String name, int maxLength) {
        String value = req.getParameter(name);
        if (blank(value)) return "";
        value = value.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String allowedParam(HttpServletRequest req, String name, String... allowed) {
        String value = textParam(req, name, 20).toUpperCase();
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

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Màn manager ưu tiên nhìn được nhiều dòng hơn quầy pha chế. */
    private static int pageSize() {
        return 10;
    }
}
