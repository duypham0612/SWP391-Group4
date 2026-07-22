package com.cafe.controller.customer;

import com.cafe.common.CsrfUtil;
import com.cafe.common.QrSessionGuard;
import com.cafe.model.Order;
import com.cafe.model.OrderItem;
import com.cafe.model.TableSession;
import com.cafe.service.customer.QrOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Customer order tracking with AJAX status updates and session-bound actions. */
@WebServlet("/qr/track")
public class QrTrackServlet extends HttpServlet {

    private final QrOrderService qrService = new QrOrderService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer sessionId = parseId(req.getParameter("s"));
        boolean ajax = "status".equals(req.getParameter("action"));
        if (sessionId == null || !isBoundToBrowser(req, sessionId)) {
            invalid(req, resp, ajax, "Liên kết theo dõi không hợp lệ. Vui lòng quét lại QR tại bàn.");
            return;
        }
        try {
            TableSession session = qrService.getSession(sessionId);
            if (session == null) {
                invalid(req, resp, ajax, "Phiên QR không tồn tại hoặc đã hết hạn.");
                return;
            }
            if (ajax) {
                writeStatusJson(resp, sessionId);
                return;
            }
            CsrfUtil.getToken(req);
            req.setAttribute("session", session);
            req.setAttribute("sessionId", sessionId);
            req.setAttribute("items", qrService.getSessionStatuses(sessionId));
            req.setAttribute("cancellableOrders", qrService.getCancellableOrders(sessionId));
            req.getRequestDispatcher("/WEB-INF/views/customer/track.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        Integer sessionId = parseId(req.getParameter("sessionId"));
        if (sessionId == null || !isBoundToBrowser(req, sessionId)) {
            resp.sendError(404, "Phiên QR không hợp lệ");
            return;
        }
        String action = req.getParameter("action");
        try {
            if (qrService.getSession(sessionId) == null) {
                resp.sendError(404, "Phiên QR không tồn tại");
                return;
            }
            if ("callStaff".equals(action)) {
                qrService.callStaff(sessionId);
                req.getSession().setAttribute("qrFlash", "Đã gọi nhân viên — vui lòng chờ trong giây lát.");
            } else if ("requestBill".equals(action)) {
                qrService.requestBill(sessionId);
                req.getSession().setAttribute("qrFlash", "Đã gửi yêu cầu thanh toán tới quầy.");
            } else if ("cancel".equals(action)) {
                Integer orderId = parseId(req.getParameter("orderId"));
                boolean ok = orderId != null && qrService.cancelOrder(sessionId, orderId);
                req.getSession().setAttribute("qrFlash", ok
                        ? "Đã huỷ đơn vì các món chưa được pha."
                        : "Không thể huỷ — đơn không thuộc phiên này hoặc đã được pha.");
            } else {
                resp.sendError(400, "Thao tác không hợp lệ");
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/qr/track?s=" + sessionId);
        } catch (IllegalStateException e) {
            req.getSession().setAttribute("qrFlash", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/qr/track?s=" + sessionId);
        } catch (Exception e) { throw new ServletException(e); }
    }

    private void writeStatusJson(HttpServletResponse resp, int sessionId) throws Exception {
        List<Map<String, Object>> itemRows = new ArrayList<>();
        for (OrderItem item : qrService.getSessionStatuses(sessionId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", item.getProductName());
            row.put("qty", item.getQuantity());
            row.put("status", item.getStatus());
            itemRows.add(row);
        }
        List<Integer> cancellableIds = new ArrayList<>();
        for (Order order : qrService.getCancellableOrders(sessionId)) cancellableIds.add(order.getOrderId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", itemRows);
        payload.put("cancellableOrderIds", cancellableIds);
        resp.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(resp.getWriter(), payload);
    }

    private boolean isBoundToBrowser(HttpServletRequest req, int sessionId) {
        HttpSession browserSession = req.getSession(false);
        Object bound = browserSession == null ? null : browserSession.getAttribute("qrSessionId");
        return QrSessionGuard.matches(bound, sessionId);
    }

    private Integer parseId(String value) {
        try { return value == null || value.isBlank() ? null : Integer.parseInt(value); }
        catch (NumberFormatException e) { return null; }
    }

    private void invalid(HttpServletRequest req, HttpServletResponse resp, boolean ajax, String reason)
            throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        if (ajax) {
            resp.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(resp.getWriter(), Map.of("error", reason));
        } else {
            req.setAttribute("invalidReason", reason);
            req.getRequestDispatcher("/WEB-INF/views/customer/invalid.jsp").forward(req, resp);
        }
    }
}
