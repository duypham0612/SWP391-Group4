package com.cafe.controller.customer;

import com.cafe.web.support.CsrfUtil;
import com.cafe.common.ItemUnavailableException;
import com.cafe.model.CartLine;
import com.cafe.service.customer.QrOrderService;
import com.cafe.web.form.FormBindingException;
import com.cafe.web.form.OrderCartForm;
import com.cafe.model.DiningTable;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** C7 · QrMenuServlet → /qr/menu?t={qrCode}. Khách quét QR → menu mobile → đặt món (ẩn danh). */
@WebServlet("/qr/menu")
public class QrMenuServlet extends HttpServlet {

    private final QrOrderService qrService;
    private final ObjectMapper mapper = new ObjectMapper();

    public QrMenuServlet() { this(new QrOrderService()); }
    QrMenuServlet(QrOrderService qrService) {
        this.qrService = java.util.Objects.requireNonNull(qrService);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String qrCode = req.getParameter("t");
        try {
            DiningTable table;
            if (qrCode == null || qrCode.isBlank()) {
                HttpSession existing = req.getSession(false);
                Integer tableId = existing == null ? null : (Integer) existing.getAttribute("qrTableId");
                table = tableId == null ? null : qrService.getTable(tableId);
                if (table == null || !"OCCUPIED".equals(table.getStatus())) {
                    forwardInvalid(req, resp, "Bàn đã đóng",
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
                    s.removeAttribute("qrTableId");
                    s.setAttribute("qrPendingTableId", t.getDiningTableId());
                    s.setAttribute("qrBranchId", t.getBranchId());
                    CsrfUtil.getToken(req);
                    req.setAttribute("table", t);
                    req.setAttribute("qrCode", qrCode);
                    req.getRequestDispatcher("/WEB-INF/views/customer/table-closed.jsp").forward(req, resp);
                    return;
                }
                table = scan.getTable();
            }

            // gắn phiên ẩn danh vào HTTP session của khách + seed CSRF cho form ghi
            HttpSession s = req.getSession();
            s.setAttribute("qrTableId", table.getDiningTableId());
            s.setAttribute("qrBranchId", table.getBranchId());
            s.removeAttribute("qrPendingTableId");
            CsrfUtil.getToken(req);

            req.setAttribute("table", table);
            req.setAttribute("tableId", table.getDiningTableId());
            req.setAttribute("menu", qrService.getMenu(table.getBranchId()));
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
        Integer tableId = s == null ? null : (Integer) s.getAttribute("qrTableId");
        if (tableId == null || branchId == null) {
            resp.setStatus(400); resp.getWriter().write("{\"error\":\"Bàn không hợp lệ, quét lại QR.\"}"); return;
        }
        try {
            // Thu ngân có thể đã chốt bill / đóng bàn trong lúc tab của khách còn mở.
            if (!qrService.isTableOrderable(tableId, branchId)) {
                resp.setStatus(409);
                resp.getWriter().write("{\"error\":\"Bàn đã được thanh toán hoặc đóng. Vui lòng quét lại QR hoặc gọi nhân viên.\"}");
                return;
            }
            OrderCartForm form = OrderCartForm.fromJson(req, mapper);
            List<CartLine> lines = form.toCartLines();
            int orderId = qrService.placeCustomerOrder(branchId, tableId, lines);
            resp.getWriter().write("{\"orderId\":" + orderId + ",\"tableId\":" + tableId + "}");
        } catch (ItemUnavailableException e) {
            resp.setStatus(409);
            mapper.writeValue(resp.getWriter(), Map.of(
                    "code", "ITEM_UNAVAILABLE",
                    "productId", e.getProductId(),
                    "productName", e.getProductName(),
                    "state", e.getState(),
                    "error", e.getReason()));
        } catch (IllegalArgumentException | FormBindingException e) { // đơn rỗng/dữ liệu sai → lỗi client
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
