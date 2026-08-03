package com.cafe.controller.customer;

import com.cafe.web.support.CsrfUtil;
import com.cafe.model.DiningTable;
import com.cafe.model.OrderItem;
import com.cafe.service.customer.QrOrderService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** C8 · QrTrackServlet → /qr/track?t={tableId}. Khách theo dõi trạng thái món (AJAX) + gọi NV / xin bill. */
@WebServlet("/qr/track")
public class QrTrackServlet extends HttpServlet {

    private final QrOrderService qrService;

    public QrTrackServlet() { this(new QrOrderService()); }
    QrTrackServlet(QrOrderService qrService) {
        this.qrService = java.util.Objects.requireNonNull(qrService);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer tableId = ownTableId(req, req.getParameter("t"));
        if (tableId == null) { resp.sendError(403, "Bàn không hợp lệ — vui lòng quét lại mã QR tại bàn."); return; }
        try {
            DiningTable table = qrService.getTable(tableId);
            if (table == null) { resp.sendError(404); return; }
            boolean tableClosed = !"OCCUPIED".equals(table.getStatus());
            if ("status".equals(req.getParameter("action"))) {
                // AJAX polling — trả JSON nhẹ
                resp.setContentType("application/json;charset=UTF-8");
                resp.setHeader("X-Table-Closed", String.valueOf(tableClosed));
                StringBuilder sb = new StringBuilder("[");
                List<OrderItem> items = qrService.getTableStatuses(tableId);
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
            req.setAttribute("table", table);
            req.setAttribute("tableClosed", tableClosed);
            req.setAttribute("tableId", tableId);
            req.setAttribute("items", qrService.getTableStatuses(tableId));
            req.setAttribute("cancellableOrders", qrService.getCancellableOrders(tableId));
            req.getRequestDispatcher("/WEB-INF/views/customer/track.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        Integer own = ownTableId(req, req.getParameter("tableId"));
        if (own == null) { resp.sendError(403, "Bàn không hợp lệ — vui lòng quét lại mã QR tại bàn."); return; }
        int tableId = own;
        String action = req.getParameter("action");
        try {
            DiningTable table = qrService.getTable(tableId);
            if (table == null || !"OCCUPIED".equals(table.getStatus())) {
                req.getSession().setAttribute("qrFlash", "Bàn đã đóng.");
                resp.sendRedirect(req.getContextPath() + "/qr/track?t=" + tableId);
                return;
            }
            int branchId = table.getBranchId();
            if ("callStaff".equals(action)) {
                qrService.callStaff(tableId, branchId);
                req.getSession().setAttribute("qrFlash", "Đã gọi nhân viên — vui lòng chờ trong giây lát.");
            } else if ("requestBill".equals(action)) {
                qrService.requestBill(tableId, branchId);
                req.getSession().setAttribute("qrFlash", "Đã gửi yêu cầu thanh toán tới quầy.");
            } else if ("cancel".equals(action)) {
                String oid = req.getParameter("orderId");
                boolean ok = oid != null && qrService.cancelOrder(tableId, Integer.parseInt(oid));
                req.getSession().setAttribute("qrFlash", ok
                        ? "Đã huỷ đơn (các món chưa pha)."
                        : "Không thể huỷ — đơn đã được pha. Vui lòng gọi nhân viên.");
            }
            resp.sendRedirect(req.getContextPath() + "/qr/track?t=" + tableId);
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** Bàn mà HTTP session ẩn danh của khách đang gắn sau khi quét QR ở /qr/menu. */
    private static Integer ownTableId(HttpServletRequest req, String requested) {
        jakarta.servlet.http.HttpSession s = req.getSession(false);
        Object own = s == null ? null : s.getAttribute("qrTableId");
        if (!(own instanceof Integer tableId)) return null;
        if (requested == null || requested.isBlank()) return tableId;
        try { return Integer.parseInt(requested) == tableId ? tableId : null; }
        catch (NumberFormatException e) { return null; }
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
