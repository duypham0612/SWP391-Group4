package com.cafe.controller.cashier;

import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.QrLink;
import com.cafe.web.support.SessionUtil;
import com.cafe.model.User;
import com.cafe.service.cashier.DiningTableService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** C3 · TableServlet → /cashier/table. Sơ đồ bàn và trạng thái phục vụ. */
@WebServlet("/cashier/table")
public class TableServlet extends HttpServlet {

    private final DiningTableService service;

    public TableServlet() { this(new DiningTableService()); }
    TableServlet(DiningTableService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        try {
            java.util.List<com.cafe.model.DiningTable> tables = service.getFloorMap(branchId);
            req.setAttribute("tables", tables);
            req.setAttribute("openRequests", service.getPendingOpenRequests(branchId));
            req.setAttribute("signals", service.getPendingSignals(branchId));

            // Link khách quét, dựng sẵn ở server để mã QR trên sơ đồ bàn giống hệt trang in.
            String baseUrl = QrLink.absoluteBase(req);
            java.util.Map<Integer, String> menuUrls = new java.util.LinkedHashMap<>();
            for (com.cafe.model.DiningTable t : tables) {
                if (t.getQrCode() != null && !t.getQrCode().isBlank()) {
                    menuUrls.put(t.getDiningTableId(), QrLink.menuUrl(baseUrl, t.getQrCode()));
                }
            }
            req.setAttribute("baseUrl", baseUrl);
            req.setAttribute("menuUrls", menuUrls);
            req.setAttribute("qrTableId", req.getParameter("qr"));   // mở sẵn mã QR của bàn vừa mở
            req.setAttribute("pageTitle", "Sơ đồ bàn");
            req.getRequestDispatcher("/WEB-INF/views/cashier/table-map.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        String action = req.getParameter("action");
        String ctx = req.getContextPath();
        try {
            if ("openTable".equals(action)) {
                int tableId = Integer.parseInt(req.getParameter("tableId"));
                service.openTable(branchId, tableId, userId);
                // Bàn được mở giống hệt nhau ở cả hai kiểu — chỉ khác chỗ thu ngân được đưa tới:
                // "qr" thì quay về sơ đồ bàn và bật sẵn mã QR để đưa khách quét;
                // còn lại mở thẳng POS để thu ngân bấm món hộ khách tại quầy.
                if ("qr".equals(req.getParameter("mode"))) {
                    resp.sendRedirect(ctx + "/cashier/table?qr=" + tableId);
                } else {
                    resp.sendRedirect(ctx + "/cashier/pos?tableId=" + tableId);
                }
                return;
            } else if ("closeTable".equals(action)) {
                boolean closed = service.closeTableIfNoUnpaidOrders(
                        Integer.parseInt(req.getParameter("tableId")), branchId);
                if (!closed) {
                    req.getSession().setAttribute("flashError",
                            "Bàn còn món chưa thanh toán — huỷ món ở Đơn đến hoặc thu tiền trước khi đóng bàn.");
                    req.getSession().setAttribute("flashErrorHref", ctx + "/cashier/inbox");
                }
            } else if ("ackSignal".equals(action)) {
                boolean acknowledged = service.acknowledgeSignals(
                        branchId, Integer.parseInt(req.getParameter("tableId")));
                if (!acknowledged) {
                    req.getSession().setAttribute("flashError",
                            "Tín hiệu không còn hợp lệ hoặc không thuộc chi nhánh hiện tại.");
                }
            } else if ("setStatus".equals(action)) {
                boolean updated = service.setTableStatus(branchId,
                        Integer.parseInt(req.getParameter("tableId")), req.getParameter("status"));
                if (!updated) {
                    req.getSession().setAttribute("flashError",
                            "Bàn không tồn tại hoặc không thuộc chi nhánh hiện tại.");
                }
            } else if ("merge".equals(action)) {
                boolean merged = service.mergeTables(branchId,
                        Integer.parseInt(req.getParameter("sourceTableId")),
                        Integer.parseInt(req.getParameter("targetTableId")));
                if (!merged) {
                    req.getSession().setAttribute("flashError",
                            "Không thể gộp: hai bàn phải thuộc cùng chi nhánh và còn đang phục vụ.");
                }
            }
            resp.sendRedirect(ctx + "/cashier/table");
        } catch (Exception e) { throw new ServletException(e); }
    }
}
