package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.shared.OrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * C4 · OrderInboxServlet → /cashier/inbox.
 * GIÁM SÁT đơn đang xử lý (COUNTER + QR, cùng OrderService) + VOID đơn sai.
 * KHÔNG chặn luồng: đơn vẫn auto vào KDS như cũ; inbox chỉ theo dõi & huỷ đơn sai.
 */
@WebServlet("/cashier/inbox")
public class OrderInboxServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            req.setAttribute("orders", orderService.getIncomingOrders(branchId));
            req.setAttribute("pageTitle", "Đơn đến (Inbox)");
            req.getRequestDispatcher("/WEB-INF/views/cashier/inbox.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        try {
            if ("void".equals(req.getParameter("action"))) {
                int orderId = Integer.parseInt(req.getParameter("orderId"));
                boolean ok = orderService.voidOrder(orderId, userId);
                req.getSession().setAttribute(ok ? "flashOk" : "flashError",
                        ok ? "Đã huỷ đơn — các món chưa pha chuyển CANCELLED (không đụng tồn)."
                           : "Không thể huỷ — đơn đã được pha (hoặc đã xử lý).");
            }
            resp.sendRedirect(req.getContextPath() + "/cashier/inbox");
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/cashier/inbox");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
