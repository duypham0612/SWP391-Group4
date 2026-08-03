package com.cafe.controller.cashier;

import com.cafe.web.support.CsrfUtil;
import com.cafe.web.support.SessionUtil;
import com.cafe.common.Constants;
import com.cafe.common.VietQrUtil;
import com.cafe.model.Bill;
import com.cafe.model.CashierShift;
import com.cafe.model.DiningTable;
import com.cafe.model.User;
import com.cafe.service.cashier.BillingService;
import com.cafe.service.cashier.CashPaymentCalculator;
import com.cafe.service.cashier.CashierShiftService;
import com.cafe.service.cashier.DiningTableService;
import com.cafe.web.viewmodel.PaymentViewModelAssembler;
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
import java.util.Objects;

/** C5 · CheckoutServlet → /cashier/checkout. showBill | splitBill | mergeBill | pay. */
@WebServlet("/cashier/checkout")
public class CheckoutServlet extends HttpServlet {

    private final BillingService billingService;
    private final CashierShiftService shiftService;
    private final DiningTableService tableService;
    private final PaymentViewModelAssembler paymentAssembler;

    public CheckoutServlet() {
        this(new BillingService(), new CashierShiftService(), new DiningTableService(),
                new PaymentViewModelAssembler());
    }

    CheckoutServlet(BillingService billingService, CashierShiftService shiftService,
                    DiningTableService tableService, PaymentViewModelAssembler paymentAssembler) {
        this.billingService = Objects.requireNonNull(billingService, "billingService");
        this.shiftService = Objects.requireNonNull(shiftService, "shiftService");
        this.tableService = Objects.requireNonNull(tableService, "tableService");
        this.paymentAssembler = Objects.requireNonNull(paymentAssembler, "paymentAssembler");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        User u = SessionUtil.currentUser(req);
        try {
            String tid = req.getParameter("tableId");
            String oid = req.getParameter("orderId");
            CashierShift shift = u != null
                    ? shiftService.getCurrentShift(u.getUserId(), branchId) : null;
            req.setAttribute("shift", shift);
            if (oid != null && !oid.isBlank()) {
                int orderId = Integer.parseInt(oid);
                Integer shiftId = shift != null ? shift.getCashierShiftId() : null;
                List<Bill> bills = billingService.buildTakeawayBill(orderId, branchId, shiftId);
                if (bills.isEmpty()) {
                    req.getSession().setAttribute("flashError",
                            "Đơn mang đi chưa hoàn tất bàn giao hoặc không còn hợp lệ để thanh toán.");
                    resp.sendRedirect(req.getContextPath() + "/cashier/inbox");
                    return;
                }
                req.setAttribute("bills", bills);
                attachPaymentData(req, bills);
                req.setAttribute("orderId", orderId);
                req.setAttribute("takeawayCheckout", true);
            } else if (tid != null && !tid.isBlank()) {
                int tableId = Integer.parseInt(tid);
                DiningTable table = billingService.getOpenTableForCheckout(tableId, branchId);
                if (table == null) {
                    req.getSession().setAttribute("flashError", "Bàn trống, không thể thanh toán.");
                    resp.sendRedirect(req.getContextPath() + "/cashier/table");
                    return;
                }
                Integer shiftId = shift != null ? shift.getCashierShiftId() : null;
                List<Bill> bills = billingService.buildTableBill(tableId, branchId, shiftId);
                req.setAttribute("table", table);
                req.setAttribute("bills", bills);
                attachPaymentData(req, bills);
                req.setAttribute("tableId", tableId);
            } else {
                // Chưa chọn: liệt kê bàn đang phục vụ và cả đơn mang đi chưa có bill.
                req.setAttribute("openTables", tableService.getOpenTables(branchId));
                req.setAttribute("takeawayOrders",
                        billingService.getTakeawayOrdersAwaitingPayment(branchId));
            }
            req.setAttribute("vietQrBankName", Constants.VIETQR_BANK_NAME);
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
        int branchId = com.cafe.web.support.BranchContext.requireBranchId(req);
        User u = SessionUtil.currentUser(req);
        String action = req.getParameter("action");
        String tableId = req.getParameter("tableId");
        String orderId = req.getParameter("orderId");
        String back = checkoutBack(req, tableId, orderId);
        try {
            if ("splitBill".equals(action) && hasText(tableId)) {
                CashierShift shift = u != null
                        ? shiftService.getCurrentShift(u.getUserId(), branchId) : null;
                Integer shiftId = shift != null ? shift.getCashierShiftId() : null;
            billingService.splitItems(Integer.parseInt(tableId), branchId, shiftId, intList(req.getParameterValues("orderItemId")));
            } else if ("mergeBill".equals(action) && hasText(tableId)) {
                billingService.mergeBills(
                        intList(req.getParameterValues("billId")), branchId);
            } else if ("pay".equals(action)) {
                int billId = Integer.parseInt(req.getParameter("billId"));
                String err = billingService.validatePayable(billId, branchId);
                if (err != null) {
                    req.getSession().setAttribute("flashError", err);
                } else {
                    CashierShift shift = u != null
                            ? shiftService.getCurrentShift(u.getUserId(), branchId) : null;
                    Integer shiftId = shift != null ? shift.getCashierShiftId() : null;
                    String method = req.getParameter("method");
                    BigDecimal cashTendered = "CASH".equals(method)
                            ? parseMoney(req.getParameter("cashTendered"))
                            : null;
                    BillingService.PaymentResult result =
                            billingService.payBill(billId, method, shiftId, cashTendered);
                    if (!result.paid()) {
                        req.getSession().setAttribute("flashError", "Hoá đơn không thể thanh toán.");
                    } else {
                        req.getSession().setAttribute("flashOk", paymentAssembler.successMessage(method, result));
                        if (hasText(orderId)) {
                            back = req.getContextPath() + "/cashier/history";
                        } else if (hasText(tableId)) {
                            DiningTable table = tableService.getTable(Integer.parseInt(tableId));
                            if (table == null || !"OCCUPIED".equals(table.getStatus())) {
                                back = req.getContextPath() + "/cashier/table";
                            }
                        }
                    }
                }
            } else if ("void".equals(action)) {
                String reason = req.getParameter("reason");
                if (reason == null || reason.isBlank()) {
                    req.getSession().setAttribute("flashError", "Phải nhập lý do khi huỷ hoá đơn.");
                } else {
                    Integer uid = u != null ? u.getUserId() : null;
                    billingService.voidBill(
                            Integer.parseInt(req.getParameter("billId")),
                            branchId, reason.trim(), uid);
                }
            }
            resp.sendRedirect(back);
        } catch (IllegalArgumentException e) {
            req.getSession().setAttribute("flashError", e.getMessage());
            resp.sendRedirect(back);
        } catch (Exception e) { throw new ServletException(e); }
    }

    private List<Integer> intList(String[] vals) {
        List<Integer> out = new ArrayList<>();
        if (vals != null) for (String v : vals) if (v != null && !v.isBlank()) out.add(Integer.parseInt(v.trim()));
        return out;
    }

    private String checkoutBack(HttpServletRequest req, String tableId, String orderId) {
        if (hasText(orderId)) return req.getContextPath() + "/cashier/checkout?orderId=" + orderId;
        if (hasText(tableId)) return req.getContextPath() + "/cashier/checkout?tableId=" + tableId;
        return req.getContextPath() + "/cashier/checkout";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    private void attachPaymentData(HttpServletRequest req, List<Bill> bills) {
        req.setAttribute("qrPayloads", buildQrPayloads(bills));
        Map<Integer, BigDecimal> payable = new LinkedHashMap<>();
        Map<Integer, BigDecimal> adjustment = new LinkedHashMap<>();
        if (bills != null) {
            for (Bill bill : bills) {
                if (!"UNPAID".equals(bill.getStatus())
                        || bill.getTotalAmount() == null
                        || bill.getTotalAmount().signum() <= 0) continue;
                CashPaymentCalculator.CashQuote quote =
                        CashPaymentCalculator.quote(bill.getTotalAmount());
                payable.put(bill.getBillId(), quote.paidAmount());
                adjustment.put(bill.getBillId(), quote.roundingAdjustment());
            }
        }
        req.setAttribute("cashPayableAmounts", payable);
        req.setAttribute("cashRoundingAdjustments", adjustment);
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Phải nhập số tiền khách đưa.");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tiền khách đưa không hợp lệ.");
        }
    }

}
