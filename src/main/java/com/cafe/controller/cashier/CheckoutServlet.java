package com.cafe.controller.cashier;
import com.cafe.controller.manager.InventoryDashboardServlet;

import com.cafe.common.CsrfUtil;
import com.cafe.common.SessionUtil;
import com.cafe.common.Constants;
import com.cafe.common.VietQrUtil;
import com.cafe.model.Bill;
import com.cafe.model.CashierShift;
import com.cafe.model.TableSession;
import com.cafe.model.User;
import com.cafe.service.cashier.BillingService;
import com.cafe.service.cashier.CashierShiftService;
import com.cafe.service.cashier.TableSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** C5 · CheckoutServlet → /cashier/checkout. ★ showBill | applyVoucher | splitBill | mergeBill | pay. */
@WebServlet("/cashier/checkout")
public class CheckoutServlet extends HttpServlet {

    private final BillingService billingService = new BillingService();
    private final CashierShiftService shiftService = new CashierShiftService();
    private final TableSessionService tableSessionService = new TableSessionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        try {
            String sid = req.getParameter("sessionId");
            CashierShift shift = u != null ? shiftService.getCurrentShift(u.getUserId()) : null;
            req.setAttribute("shift", shift);
            if (sid != null && !sid.isBlank()) {
                int sessionId = Integer.parseInt(sid);
                TableSession tableSession = tableSessionService.getSession(sessionId);
                if (tableSession == null || !"OPEN".equals(tableSession.getStatus())
                        || tableSession.getBranchId() != branchId) {
                    req.getSession().setAttribute("flashError", "Bàn trống, không thể thanh toán.");
                    resp.sendRedirect(req.getContextPath() + "/cashier/table");
                    return;
                }
                Integer shiftId = shift != null ? shift.getCashierShiftId() : null;
                List<Bill> bills = billingService.buildSessionBill(sessionId, branchId, shiftId);
                req.setAttribute("session", tableSession);
                req.setAttribute("bills", bills);
                req.setAttribute("qrPayloads", buildQrPayloads(bills));
                req.setAttribute("sessionId", sessionId);
            } else {
                // chưa chọn phiên: liệt kê các phiên đang mở để chọn
                req.setAttribute("openSessions", tableSessionService.getOpenSessions(branchId));
            }
            req.setAttribute("vietQrAccountName", Constants.VIETQR_ACCOUNT_NAME);
            req.setAttribute("vietQrAccountNo", Constants.VIETQR_ACCOUNT_NO);
            req.setAttribute("pageTitle", "Thanh toán");
            req.getRequestDispatcher("/WEB-INF/views/cashier/checkout.jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!CsrfUtil.isValid(req)) { resp.sendError(403, "CSRF"); return; }
        int branchId = InventoryDashboardServlet.branchId(req);
        User u = SessionUtil.currentUser(req);
        String action = req.getParameter("action");
        String sessionId = req.getParameter("sessionId");
        String back = req.getContextPath() + "/cashier/checkout?sessionId=" + sessionId;
        try {
            if ("applyVoucher".equals(action)) {
                int billId = Integer.parseInt(req.getParameter("billId"));
                String err = billingService.applyVoucher(billId, req.getParameter("code"), branchId);
                if (err != null) req.getSession().setAttribute("flashError", err);
            } else if ("removeVoucher".equals(action)) {
                billingService.removeVoucher(Integer.parseInt(req.getParameter("billId")));
            } else if ("splitBill".equals(action)) {
                CashierShift shift = u != null ? shiftService.getCurrentShift(u.getUserId()) : null;
                Integer shiftId = shift != null ? shift.getCashierShiftId() : null;
                billingService.splitItems(Integer.parseInt(sessionId), branchId, shiftId, intList(req.getParameterValues("billItemId")));
            } else if ("mergeBill".equals(action)) {
                billingService.mergeBills(intList(req.getParameterValues("billId")));
            } else if ("pay".equals(action)) {
                int billId = Integer.parseInt(req.getParameter("billId"));
                String err = validatePayable(billId, branchId);
                if (err != null) {
                    req.getSession().setAttribute("flashError", err);
                } else {
                    boolean ok = billingService.payBill(billId, req.getParameter("method"));
                    if (!ok) {
                        req.getSession().setAttribute("flashError", "Hoá đơn không thể thanh toán.");
                    } else if (sessionId != null && !sessionId.isBlank()) {
                        TableSession session = tableSessionService.getSession(Integer.parseInt(sessionId));
                        if (session == null || !"OPEN".equals(session.getStatus())) {
                            back = req.getContextPath() + "/cashier/table";
                        }
                    }
                }
            } else if ("void".equals(action)) {
                String reason = req.getParameter("reason");
                if (reason == null || reason.isBlank()) {
                    req.getSession().setAttribute("flashError", "Phải nhập lý do khi huỷ hoá đơn.");
                } else {
                    Integer uid = u != null ? u.getUserId() : null;
                    billingService.voidBill(Integer.parseInt(req.getParameter("billId")), reason.trim(), uid);
                }
            }
            resp.sendRedirect(back);
        } catch (Exception e) { throw new ServletException(e); }
    }

    private List<Integer> intList(String[] vals) {
        List<Integer> out = new ArrayList<>();
        if (vals != null) for (String v : vals) if (v != null && !v.isBlank()) out.add(Integer.parseInt(v.trim()));
        return out;
    }

    private Map<Integer, String> buildQrPayloads(List<Bill> bills) {
        Map<Integer, String> payloads = new LinkedHashMap<>();
        if (bills == null) return payloads;
        for (Bill bill : bills) {
            if ("UNPAID".equals(bill.getStatus())) {
                payloads.put(bill.getBillId(),
                        VietQrUtil.buildPayload(bill.getTotalAmount(), "CAFE BILL " + bill.getBillId()));
            }
        }
        return payloads;
    }

    private String validatePayable(int billId, int branchId) throws Exception {
        Bill bill = billingService.getBill(billId);
        if (bill == null || bill.getBranchId() != branchId) return "Không tìm thấy hoá đơn.";
        if (!"UNPAID".equals(bill.getStatus())) return "Hoá đơn đã được thanh toán hoặc đã huỷ.";
        if (bill.getItems() == null || bill.getItems().isEmpty()) return "Hoá đơn chưa có món, không thể thanh toán.";
        BigDecimal total = bill.getTotalAmount() == null ? BigDecimal.ZERO : bill.getTotalAmount();
        if (total.compareTo(BigDecimal.ZERO) <= 0) return "Tổng tiền hoá đơn phải lớn hơn 0.";
        return null;
    }
}
