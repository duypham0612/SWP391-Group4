package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.barista.PickupService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Màn bàn giao thuộc role Thu ngân; Barista chỉ xem READY tại Quầy pha chế. */
@WebServlet("/cashier/handoff")
public class PickupServlet extends HttpServlet {

    private final PickupService service = new PickupService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            loadBoard(req, branchId);
            req.setAttribute("pageTitle", "Sẵn sàng bàn giao");
            boolean partial = "1".equals(req.getParameter("partial"));
            String view = partial
                    ? "/WEB-INF/views/barista/pickup_cards.jsp"
                    : "/WEB-INF/views/barista/pickup.jsp";
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
        int branchId = InventoryDashboardServlet.branchId(req);
        try {
            if ("pickUp".equals(action)) {
                if (!service.pickUpItem(intParam(req, "orderItemId"), userId, branchId))
                    flashConflict(req);
            } else if ("pickUpAllReady".equals(action)) {
                int done = service.pickUpAllReady(intParam(req, "orderId"), userId, branchId);
                if (done == 0) flashConflict(req);
                else req.getSession().setAttribute("flashOk", "Đã nhận " + done + " dòng món khỏi quầy.");
            } else if ("serve".equals(action)) {
                if (!service.serveItem(intParam(req, "orderItemId"), userId, branchId)) flashConflict(req);
            }
            renderResult(req, resp, branchId);
        } catch (SQLException e) {
            req.getSession().setAttribute("flashError", "Không thể cập nhật bàn giao lúc này. Vui lòng tải lại và thử lại.");
            try { renderResult(req, resp, branchId); }
            catch (SQLException ex) { throw new ServletException(ex); }
        }
    }

    private void renderResult(HttpServletRequest req, HttpServletResponse resp, int branchId)
            throws SQLException, ServletException, IOException {
        if ("1".equals(req.getParameter("ajax"))) {
            loadBoard(req, branchId);
            req.getRequestDispatcher("/WEB-INF/views/barista/pickup_cards.jsp").forward(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/cashier/handoff");
        }
    }

    /** Nạp ticket sẵn lấy + món vừa giao (để hoàn tác) cho cả trang đầy đủ lẫn fragment AJAX. */
    private void loadBoard(HttpServletRequest req, int branchId) throws SQLException {
        req.setAttribute("tickets", service.getReadyTickets(branchId));
        req.setAttribute("pickedUpItems", service.getPickedUpItems(branchId));
    }

    private static void flashConflict(HttpServletRequest req) {
        req.getSession().setAttribute("flashError", "Món vừa được cập nhật bởi thao tác khác — bảng đã làm mới.");
    }

    private static int intParam(HttpServletRequest req, String name) {
        return Integer.parseInt(req.getParameter(name));
    }
}
