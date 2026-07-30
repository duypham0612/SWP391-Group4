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
        Integer sessionId = ownSessionId(req, req.getParameter("s"));
        if (sessionId == null) { resp.sendError(403, "Phiên không hợp lệ — vui lòng quét lại mã QR tại bàn."); return; }
        try {
            TableSession session = qrService.getSession(sessionId);
            if (session == null) { resp.sendError(404); return; }
            boolean sessionClosed = !"OPEN".equals(session.getStatus());
            if ("status".equals(req.getParameter("action"))) {
                // AJAX polling — trả JSON nhẹ
                resp.setContentType("application/json;charset=UTF-8");
                resp.setHeader("X-Session-Closed", String.valueOf(sessionClosed));
                StringBuilder sb = new StringBuilder("[");
                List<OrderItem> items = qrService.getSessionStatuses(sessionId);
                for (int i = 0; i < items.size(); i++) {
                    OrderItem it = items.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"name\":\"").append(esc(it.getProductName()))
                      .append("\",\"qty\":").append(it.getQuantity())
                      .append(",\"status\":\"").append(it.getStatus()).append("\"")
                      .append(",\"issueReason\":\"").append(esc(it.getIssueReason())).append("\"}");
                }
                sb.append("]");
                resp.getWriter().write(sb.toString());
                return;
            }
            CsrfUtil.getToken(req);   // seed token cho nút gọi NV / xin bill / huỷ đơn
            req.setAttribute("session", session);
            req.setAttribute("sessionClosed", sessionClosed);
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
        Integer own = ownSessionId(req, req.getParameter("sessionId"));
        if (own == null) { resp.sendError(403, "Phiên không hợp lệ — vui lòng quét lại mã QR tại bàn."); return; }
        int sessionId = own;
        String action = req.getParameter("action");
        try {
            TableSession session = qrService.getSession(sessionId);
            if (session == null || !"OPEN".equals(session.getStatus())) {
                req.getSession().setAttribute("qrFlash", "Phiên bàn đã kết thúc.");
                resp.sendRedirect(req.getContextPath() + "/qr/track?s=" + sessionId);
                return;
            }
            int branchId = session != null ? session.getBranchId() : 0;
            if ("callStaff".equals(action)) {
                qrService.callStaff(sessionId, branchId);
                req.getSession().setAttribute("qrFlash", "Đã gọi nhân viên — vui lòng chờ trong giây lát.");
            } else if ("requestBill".equals(action)) {
                qrService.requestBill(sessionId, branchId);
                req.getSession().setAttribute("qrFlash", "Đã gửi yêu cầu thanh toán tới quầy.");
            } else if ("cancel".equals(action)) {
                String oid = req.getParameter("orderId");
                boolean ok = oid != null && qrService.cancelOrder(sessionId, Integer.parseInt(oid));
                req.getSession().setAttribute("qrFlash", ok
                        ? "Đã huỷ đơn (các món chưa pha)."
                        : "Không thể huỷ — đơn đã được pha. Vui lòng gọi nhân viên.");
            }
            resp.sendRedirect(req.getContextPath() + "/qr/track?s=" + sessionId);
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Phiên bàn mà HTTP session của khách này đang gắn (do quét QR ở /qr/menu) — xem QrSessionPolicy. */
    private static Integer ownSessionId(HttpServletRequest req, String requested) {
        jakarta.servlet.http.HttpSession s = req.getSession(false);
        return QrSessionPolicy.resolve(s == null ? null : s.getAttribute("qrSessionId"), requested);
    }

    private String esc(String s) {
        return s == null ? "" : s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
