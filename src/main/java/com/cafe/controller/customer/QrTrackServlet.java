package com.cafe.controller.customer;

import com.cafe.common.CsrfUtil;
import com.cafe.model.OrderItem;
import com.cafe.model.TableSession;
import com.cafe.service.customer.QrOrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** C8 · QrTrackServlet → /qr/track?s={sessionId}. Khách theo dõi trạng thái món (AJAX) + gọi NV / xin bill. */
@WebServlet("/qr/track")
public class QrTrackServlet extends HttpServlet {

    private final QrOrderService qrService = new QrOrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String sid = req.getParameter("s");
        if (sid == null || sid.isBlank()) { resp.sendError(400); return; }
        int sessionId = Integer.parseInt(sid);
        try {
            if ("status".equals(req.getParameter("action"))) {
                // AJAX polling — trả JSON nhẹ
                resp.setContentType("application/json;charset=UTF-8");
                StringBuilder sb = new StringBuilder("[");
                List<OrderItem> items = qrService.getSessionStatuses(sessionId);
                for (int i = 0; i < items.size(); i++) {
                    OrderItem it = items.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"name\":\"").append(esc(it.getProductName()))
                      .append("\",\"qty\":").append(it.getQuantity())
                      .append(",\"status\":\"").append(it.getStatus()).append("\"}");
                }
                sb.append("]");
                resp.getWriter().write(sb.toString());
                return;
            }
            TableSession session = qrService.getSession(sessionId);
            if (session == null) { resp.sendError(404); return; }
            CsrfUtil.getToken(req);   // seed token cho nút gọi NV / xin bill / huỷ đơn
            req.setAttribute("session", session);
            req.setAttribute("sessionId", sessionId);
            req.setAttribute("items", qrService.getSessionStatuses(sessionId));
            req.setAttribute("cancellableOrders", qrService.getCancellableOrders(sessionId));   // R5
            req.getRequestDispatcher("/WEB-INF/views/customer/track.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int sessionId = Integer.parseInt(req.getParameter("sessionId"));
        String action = req.getParameter("action");
        try {
            TableSession session = qrService.getSession(sessionId);
            int branchId = session != null ? session.getBranchId() : 0;
            if ("callStaff".equals(action)) {
                qrService.callStaff(sessionId, branchId);
                req.getSession().setAttribute("qrFlash", "Đã gọi nhân viên — vui lòng chờ trong giây lát.");
            } else if ("requestBill".equals(action)) {
                qrService.requestBill(sessionId, branchId);
                req.getSession().setAttribute("qrFlash", "Đã gửi yêu cầu thanh toán tới quầy.");
            } else if ("cancel".equals(action)) {
                String oid = req.getParameter("orderId");
                boolean ok = oid != null && qrService.cancelOrder(Integer.parseInt(oid));
                req.getSession().setAttribute("qrFlash", ok
                        ? "Đã huỷ đơn (các món chưa pha)."
                        : "Không thể huỷ — đơn đã được pha. Vui lòng gọi nhân viên.");
            }
            resp.sendRedirect(req.getContextPath() + "/qr/track?s=" + sessionId);
        } catch (Exception e) { throw new ServletException(e); }
    }

    private String esc(String s) { return s == null ? "" : s.replace("\"", "'"); }
}
