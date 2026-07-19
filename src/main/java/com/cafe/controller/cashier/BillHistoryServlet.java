package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.model.Bill;
import com.cafe.model.CashierShift;
import com.cafe.model.User;
import com.cafe.service.cashier.BillingService;
import com.cafe.service.cashier.CashierShiftService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** C6 · BillHistoryServlet → /cashier/history. list (lọc theo ca) | view | void (kèm lý do + log). */
@WebServlet("/cashier/history")
public class BillHistoryServlet extends HttpServlet {

    private final BillingService service = new BillingService();
    private final CashierShiftService shiftService = new CashierShiftService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        try {
            if ("view".equals(req.getParameter("action")) && req.getParameter("billId") != null) {
                req.setAttribute("bill", service.getBill(Integer.parseInt(req.getParameter("billId"))));
                req.setAttribute("pageTitle", "Chi tiết hoá đơn");
                req.getRequestDispatcher("/WEB-INF/views/cashier/bill-view.jsp").forward(req, resp);
            } else {
                // Mặc định lọc theo CA hiện tại; "scope=branch" để xem toàn chi nhánh.
                CashierShift shift = u != null ? shiftService.getCurrentShift(u.getUserId()) : null;
                boolean byShift = shift != null && !"branch".equals(req.getParameter("scope"));
                List<Bill> bills;
                if (byShift) {
                    bills = service.getBillHistoryByShift(shift.getCashierShiftId());
                    req.setAttribute("scopeLabel", "Ca hiện tại (#" + shift.getCashierShiftId() + ")");
                } else {
                    bills = service.getBillHistory(branchId);
                    req.setAttribute("scopeLabel", "Toàn chi nhánh");
                }
                // R4 · lọc theo hình thức thanh toán (CASH/TRANSFER/QR_BANK) nếu có
                req.setAttribute("bills", filterByMethod(bills, req.getParameter("method")));
                req.setAttribute("hasOpenShift", shift != null);
                req.setAttribute("pageTitle", "Lịch sử hoá đơn");
                req.getRequestDispatcher("/WEB-INF/views/cashier/bill-history.jsp").forward(req, resp);
            }
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        User u = SessionUtil.currentUser(req);
        Integer userId = u != null ? u.getUserId() : null;
        try {
            String action = req.getParameter("action");
            if ("void".equals(action) || "refund".equals(action)) {
                String reason = req.getParameter("reason");
                if (reason == null || reason.isBlank()) {
                    req.getSession().setAttribute("flashError", "Phải nhập lý do khi huỷ/hoàn hoá đơn.");
                } else {
                    int billId = Integer.parseInt(req.getParameter("billId"));
                    boolean refund = "refund".equals(action);
                    boolean ok = refund
                            ? service.refundBill(billId, reason.trim(), userId)
                            : service.voidBill(billId, reason.trim(), userId);
                    if (refund) {
                        req.getSession().setAttribute(ok ? "flashOk" : "flashError",
                                ok ? "Đã hoàn hoá đơn đã thanh toán (đã ghi log lý do)."
                                   : "Không hoàn được (hoá đơn chưa thanh toán hoặc đã hoàn?).");
                    } else {
                        req.getSession().setAttribute(ok ? "flashOk" : "flashError",
                                ok ? "Đã huỷ hoá đơn (đã ghi log lý do)." : "Không huỷ được (đã thanh toán?).");
                    }
                }
            }
            resp.sendRedirect(req.getContextPath() + "/cashier/history");
        } catch (Exception e) { throw new ServletException(e); }
    }

    /** R4 · Lọc danh sách bill theo hình thức thanh toán; rỗng → giữ nguyên. */
    private List<Bill> filterByMethod(List<Bill> bills, String method) {
        if (method == null || method.isBlank()) return bills;
        List<Bill> out = new ArrayList<>();
        for (Bill b : bills) if (method.equals(b.getPaymentMethod())) out.add(b);
        return out;
    }
}
