package com.cafe.controller.customer;

import com.cafe.common.CsrfUtil;
import com.cafe.service.shared.OrderService;
import com.cafe.service.customer.QrOrderService;
import com.cafe.model.DiningTable;
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
            TableSession session;
            if (qrCode == null || qrCode.isBlank()) {
                HttpSession existing = req.getSession(false);
                Integer sessionId = existing == null ? null : (Integer) existing.getAttribute("qrSessionId");
                session = sessionId == null ? null : qrService.getSession(sessionId);
                if (session == null || !"OPEN".equals(session.getStatus())) {
                    forwardInvalid(req, resp, "Phiên bàn đã kết thúc",
                            "Bàn không còn nhận thêm món. Vui lòng quét lại mã QR tại bàn hoặc nhờ nhân viên hỗ trợ.");
                    return;
                }
            } else {
                QrOrderService.ScanResult scan = qrService.scan(qrCode);
                if (scan.getStatus() == QrOrderService.ScanResult.Status.INVALID_QR) {
                    forwardInvalid(req, resp, "Mã QR không hợp lệ",
                            "Không tìm thấy bàn ứng với mã này. Vui lòng quét lại mã QR dán tại bàn hoặc nhờ nhân viên hỗ trợ.");
                    return;
                }
                if (scan.getStatus() == QrOrderService.ScanResult.Status.TABLE_NOT_OPEN) {
                    // Bàn chưa được thu ngân mở → khách chưa đặt món được, chỉ xin quầy mở bàn.
                    DiningTable t = scan.getTable();
                    HttpSession s = req.getSession();
                    s.removeAttribute("qrSessionId");
                    s.setAttribute("qrPendingTableId", t.getDiningTableId());
                    s.setAttribute("qrBranchId", t.getBranchId());
                    CsrfUtil.getToken(req);
                    req.setAttribute("table", t);
                    req.setAttribute("qrCode", qrCode);
                    req.getRequestDispatcher("/WEB-INF/views/customer/table-closed.jsp").forward(req, resp);
                    return;
                }
                session = scan.getSession();
            }

            // gắn phiên ẩn danh vào HTTP session của khách + seed CSRF cho form ghi
            HttpSession s = req.getSession();
            s.setAttribute("qrSessionId", session.getTableSessionId());
            s.setAttribute("qrBranchId", session.getBranchId());
            s.removeAttribute("qrPendingTableId");
            CsrfUtil.getToken(req);

            req.setAttribute("table", session);
            req.setAttribute("sessionId", session.getTableSessionId());
            req.setAttribute("menu", qrService.getMenu(session.getBranchId()));
            req.getRequestDispatcher("/WEB-INF/views/customer/menu.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Form "xin mở bàn" gửi urlencoded; đặt món gửi JSON — tách nhánh trước khi đọc body. */
    private static boolean isRequestOpen(HttpServletRequest req) {
        return "requestOpen".equals(req.getParameter("action"));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) {
            if (isRequestOpen(req)) { resp.sendError(403, "CSRF"); return; }
            resp.setContentType("application/json;charset=UTF-8");
            resp.setStatus(403); resp.getWriter().write("{\"error\":\"CSRF\"}"); return;
        }
        HttpSession s = req.getSession(false);
        Integer branchId = s == null ? null : (Integer) s.getAttribute("qrBranchId");

        // Khách ở bàn CHƯA mở: chỉ được xin quầy mở bàn, không có đường nào đặt món.
        if (isRequestOpen(req)) {
            Integer tableId = s == null ? null : (Integer) s.getAttribute("qrPendingTableId");
            String qrCode = req.getParameter("qrCode");
            if (tableId == null || branchId == null) {
                resp.sendRedirect(menuLocation(req, qrCode)); return;
            }
            try {
                qrService.requestTableOpen(branchId, tableId, req.getParameter("tableNumber"));
                s.setAttribute("qrFlash", "Đã báo quầy — nhân viên sẽ mở bàn cho bạn trong giây lát.");
            } catch (Exception e) { throw new ServletException(e); }
            resp.sendRedirect(menuLocation(req, qrCode));
            return;
        }

        resp.setContentType("application/json;charset=UTF-8");
        Integer sessionId = s == null ? null : (Integer) s.getAttribute("qrSessionId");
        if (sessionId == null || branchId == null) {
            resp.setStatus(400); resp.getWriter().write("{\"error\":\"Phiên không hợp lệ, quét lại QR.\"}"); return;
        }
        try {
            // Thu ngân có thể đã chốt bill / đóng bàn trong lúc tab của khách còn mở.
            if (!qrService.isSessionOrderable(sessionId)) {
                resp.setStatus(409);
                resp.getWriter().write("{\"error\":\"Bàn đã được thanh toán hoặc đóng. Vui lòng quét lại QR hoặc gọi nhân viên.\"}");
                return;
            }
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

    private static String menuLocation(HttpServletRequest req, String qrCode) {
        return req.getContextPath() + "/qr/menu"
                + (qrCode == null || qrCode.isBlank() ? "" : "?t="
                + java.net.URLEncoder.encode(qrCode, java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void forwardInvalid(HttpServletRequest req, HttpServletResponse resp,
                                       String title, String message)
            throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        req.setAttribute("invalidTitle", title);
        req.setAttribute("invalidMessage", message);
        req.getRequestDispatcher("/WEB-INF/views/customer/invalid.jsp").forward(req, resp);
    }
}
