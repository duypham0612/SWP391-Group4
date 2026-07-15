package com.cafe.controller.barista;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.Constants;
import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.KdsTicket;
import com.cafe.model.OrderItem;
import com.cafe.model.User;
import com.cafe.service.barista.KdsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** B1 · KdsServlet → /barista/kds. Hàng chờ bếp + start/markReady/markReadyAll/bump/cantMake (★ auto-deduct ở markReady). */
@WebServlet("/barista/kds")
public class KdsServlet extends HttpServlet {

    private final KdsService service = new KdsService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            loadBoard(req, branchId);
            req.setAttribute("pageTitle", "Hàng chờ pha");
            boolean partial = "1".equals(req.getParameter("partial"));
            if (!partial) BaristaShift.expose(req, "/barista/kds");   // trực ca: banner + khoá thao tác
            String view = partial
                    ? "/WEB-INF/views/barista/kds_cards.jsp"
                    : "/WEB-INF/views/barista/kds.jsp";
            req.getRequestDispatcher(view).forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        String action = req.getParameter("action");
        if (BaristaShift.guardWrite(req, resp, action, "/barista/kds")) return;   // vào ca / chặn ngoài ca
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("start".equals(action)) {
                service.startItem(intParam(req, "orderItemId"), userId, branchId);
            } else if ("markReady".equals(action)) {
                service.markReady(intParam(req, "orderItemId"), userId, branchId);
            } else if ("markReadyAll".equals(action)) {
                service.markOrderReady(intParam(req, "orderId"), userId, branchId);
            } else if ("bump".equals(action)) {
                service.bump(intParam(req, "orderItemId"), branchId);
            } else if ("cantMake".equals(action)) {
                service.cancelItem(intParam(req, "orderItemId"), req.getParameter("reason"), userId, branchId);
                if ("true".equals(req.getParameter("also86"))) {
                    Integer productId = optIntParam(req, "productId");
                    if (productId != null) service.set86(branchId, productId, true, parseEta(req.getParameter("backInEta")), userId);
                }
            }
            // AJAX: trả lại fragment bảng (summary + lưới) để swap tại chỗ, giữ chỗ cuộn.
            if ("1".equals(req.getParameter("ajax"))) {
                loadBoard(req, branchId);
                req.getRequestDispatcher("/WEB-INF/views/barista/kds_cards.jsp").forward(req, resp);
            } else {
                resp.sendRedirect(req.getContextPath() + "/barista/kds");
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Nạp board 2 cột (Chờ pha | Đang pha) + số liệu tóm tắt cho cả trang đầy đủ lẫn fragment AJAX. */
    private void loadBoard(HttpServletRequest req, int branchId) throws SQLException {
        Map<String, List<KdsTicket>> board = service.getQueueBoard(branchId);
        List<KdsTicket> waiting = board.get("waiting");
        List<KdsTicket> making = board.get("making");
        int waitingItems = 0, makingItems = 0, oldest = 0, overdue = 0;
        for (KdsTicket t : waiting) {
            waitingItems += t.getItemCount();
            oldest = Math.max(oldest, t.getWaitedSeconds());
            if ("crit".equals(t.getAgeTier())) overdue++;   // đơn chờ pha đã quá giờ = backlog trễ
        }
        for (KdsTicket t : making) {
            makingItems += t.getItemCount();
            oldest = Math.max(oldest, t.getWaitedSeconds());
        }
        req.setAttribute("waitingTickets", waiting);
        req.setAttribute("makingTickets", making);
        req.setAttribute("waitingCount", waitingItems);
        req.setAttribute("makingCount", makingItems);
        req.setAttribute("overdueCount", overdue);
        req.setAttribute("oldestDisplay", OrderItem.formatDuration(oldest));
        req.setAttribute("oldestTier", tier(oldest));
        req.setAttribute("kdsWarnMin", Constants.KDS_WARN_SECONDS / 60);
        req.setAttribute("kdsCritMin", Constants.KDS_CRIT_SECONDS / 60);
    }

    private static int intParam(HttpServletRequest req, String name) {
        return Integer.parseInt(req.getParameter(name));
    }

    private static Integer optIntParam(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) return null;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return null; }
    }

    private static java.time.LocalDateTime parseEta(String s) {
        if (s == null || s.isBlank()) return null;
        try { return java.time.LocalDateTime.parse(s); }
        catch (java.time.format.DateTimeParseException e) { return null; }
    }

    private String tier(int seconds) {
        if (seconds >= Constants.KDS_CRIT_SECONDS) return "crit";
        if (seconds >= Constants.KDS_WARN_SECONDS) return "warn";
        return "ok";
    }
}
