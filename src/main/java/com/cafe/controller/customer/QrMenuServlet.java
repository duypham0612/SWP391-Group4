package com.cafe.controller.customer;

import com.cafe.common.CsrfUtil;
import com.cafe.service.shared.OrderService;
import com.cafe.service.customer.QrOrderService;
import com.cafe.model.TableSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** C7 · QrMenuServlet → /qr/menu?t={qrCode}. Khách quét QR → menu mobile → đặt món (ẩn danh). */
@WebServlet("/qr/menu")
public class QrMenuServlet extends HttpServlet {

    private final QrOrderService qrService = new QrOrderService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String qrCode = req.getParameter("t");
        try {
            TableSession session = qrService.identifyTable(qrCode);
            if (session == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                req.getRequestDispatcher("/WEB-INF/views/customer/invalid.jsp").forward(req, resp);
                return;
            }
            // gắn phiên ẩn danh vào HTTP session của khách + seed CSRF cho form ghi
            HttpSession s = req.getSession();
            s.setAttribute("qrSessionId", session.getTableSessionId());
            s.setAttribute("qrBranchId", session.getBranchId());
            CsrfUtil.getToken(req);

            req.setAttribute("table", session);
            req.setAttribute("sessionId", session.getTableSessionId());
            req.setAttribute("menu", qrService.getMenu(session.getBranchId()));
            req.getRequestDispatcher("/WEB-INF/views/customer/menu.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!CsrfUtil.isValid(req)) { resp.setStatus(403); resp.getWriter().write("{\"error\":\"CSRF\"}"); return; }
        HttpSession s = req.getSession(false);
        Integer sessionId = s == null ? null : (Integer) s.getAttribute("qrSessionId");
        Integer branchId = s == null ? null : (Integer) s.getAttribute("qrBranchId");
        if (sessionId == null || branchId == null) {
            resp.setStatus(400); resp.getWriter().write("{\"error\":\"Phiên không hợp lệ, quét lại QR.\"}"); return;
        }
        try {
            JsonNode body = mapper.readTree(req.getInputStream());
            List<OrderService.CartLine> lines = new ArrayList<>();
            JsonNode items = body.get("items");
            if (items != null && items.isArray()) {
                for (JsonNode n : items) {
                    OrderService.CartLine line = new OrderService.CartLine();
                    line.productId = n.get("productId").asInt();
                    line.quantity = n.has("quantity") ? n.get("quantity").asInt() : 1;
                    line.note = n.hasNonNull("note") ? n.get("note").asText() : null;
                    JsonNode opts = n.get("optionIds");
                    if (opts != null && opts.isArray()) for (JsonNode o : opts) line.optionIds.add(o.asInt());
                    lines.add(line);
                }
            }
            if (lines.isEmpty()) { resp.setStatus(400); resp.getWriter().write("{\"error\":\"Giỏ trống\"}"); return; }
            int orderId = qrService.placeCustomerOrder(branchId, sessionId, lines);
            resp.getWriter().write("{\"orderId\":" + orderId + ",\"sessionId\":" + sessionId + "}");
        } catch (IllegalArgumentException e) {                 // 86/đơn rỗng… → lỗi client, báo thân thiện
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"" + (e.getMessage() == null ? "Lỗi" : e.getMessage().replace("\"","'")) + "\"}");
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + (e.getMessage() == null ? "Lỗi" : e.getMessage().replace("\"","'")) + "\"}");
        }
    }
}
