package com.cafe.service.cashier;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.*;
import com.cafe.dao.shared.*;
import com.cafe.model.*;
import com.cafe.service.shared.VoucherService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/** Chốt thanh toán và đóng phiên/bàn trong một transaction. */
public final class PaymentService {
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "TRANSFER", "QR_BANK");
    private final BillingRepository repository;

    public PaymentService() { this(new BillingRepository()); }
    PaymentService(BillingRepository repository) {
        this.repository = java.util.Objects.requireNonNull(repository);
    }

    /**
     * Thanh toán 1 bill trong ca hiện tại. CASH lưu snapshot làm tròn/khách đưa/tiền thối;
     * phương thức không tiền mặt quyết toán đúng TotalAmount.
     */
    public PaymentResult payBill(int billId, String method, Integer shiftId,
                                 BigDecimal cashTendered) throws SQLException {
        if (shiftId == null || !isSupportedPaymentMethod(method)) return PaymentResult.notPaid();
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                CashierShift shift = repository.cashierShiftDao.findOpenByIdForUpdate(c, shiftId);
                if (shift == null) {
                    c.rollback();
                    return PaymentResult.notPaid();
                }
                Bill bill = repository.billDao.findByIdForUpdate(c, billId);
                if (bill == null || bill.getBranchId() != shift.getBranchId()
                        || !"UNPAID".equals(bill.getStatus())) {
                    c.rollback();
                    return PaymentResult.notPaid();
                }
                List<BillItem> payableItems = repository.billItemDao.findByBill(c, billId);
                if (payableItems.isEmpty() || payableItems.stream().anyMatch(item -> !"SERVED".equals(item.getStatus()))
                        || bill.getTotalAmount() == null
                        || bill.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    c.rollback();
                    return PaymentResult.notPaid();
                }
                if (bill.getVoucherId() != null) {
                    Voucher voucher = repository.voucherDao.findByIdForUpdate(c, bill.getVoucherId());
                    String voucherError = VoucherService.validateVoucherRecord(voucher, bill.getBranchId(), bill.getSubtotal());
                    if (voucherError != null) { c.rollback(); return PaymentResult.notPaid(); }
                    // Reserve lượt ngay trong transaction. Hết lượt thì không được phép
                    // chốt bill đã giảm giá.
                    if (!repository.voucherDao.incrementUsed(c, bill.getVoucherId())) {
                        c.rollback();
                        return PaymentResult.notPaid();
                    }
                }
                BigDecimal adjustment = BigDecimal.ZERO.setScale(2);
                BigDecimal paidAmount = bill.getTotalAmount();
                BigDecimal tendered = null;
                BigDecimal change = null;
                if ("CASH".equals(method)) {
                    CashPaymentCalculator.CashSettlement settlement =
                            CashPaymentCalculator.settle(bill.getTotalAmount(), cashTendered);
                    adjustment = settlement.roundingAdjustment();
                    paidAmount = settlement.paidAmount();
                    tendered = settlement.cashTendered();
                    change = settlement.cashChange();
                }

                int rows = repository.billDao.markPaid(c, billId, method, shiftId,
                        adjustment, paidAmount, tendered, change);
                if (rows == 0) { c.rollback(); return PaymentResult.notPaid(); }

                repository.outboxEventDao.insert(c, EventType.PAYMENT_COMPLETED, String.valueOf(billId), bill.getBranchId(),
                        paymentPayload(billId, method, bill.getTotalAmount(), adjustment,
                                paidAmount, tendered, change));

                // Nếu phiên không còn bill UNPAID → đóng phiên + trả bàn EMPTY
                if (bill.getTableSessionId() != null
                        && repository.billDao.findUnpaidBySession(c, bill.getTableSessionId()).isEmpty()) {
                    TableSession s = repository.sessionDao.findById(c, bill.getTableSessionId());
                    if (s != null && "OPEN".equals(s.getStatus())) {
                        repository.sessionDao.updateStatus(c, s.getTableSessionId(), "CLOSED", true);
                        repository.tableDao.updateStatus(c, s.getDiningTableId(), "EMPTY");
                    }
                    repository.outboxEventDao.markBillRequestProcessed(c, bill.getTableSessionId());
                }
                c.commit();
                return new PaymentResult(true, paidAmount, tendered, change, adjustment);
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    private String paymentPayload(int billId, String method, BigDecimal total,
                                  BigDecimal adjustment, BigDecimal paidAmount,
                                  BigDecimal tendered, BigDecimal change) {
        return "{\"billId\":" + billId
                + ",\"method\":\"" + method
                + "\",\"total\":" + total.toPlainString()
                + ",\"roundingAdjustment\":" + adjustment.toPlainString()
                + ",\"paidAmount\":" + paidAmount.toPlainString()
                + ",\"cashTendered\":" + jsonNumber(tendered)
                + ",\"cashChange\":" + jsonNumber(change) + "}";
    }

    private String jsonNumber(BigDecimal value) {
        return value == null ? "null" : value.toPlainString();
    }

    public static boolean isSupportedPaymentMethod(String method) {
        return method != null && PAYMENT_METHODS.contains(method);
    }

    public record PaymentResult(
            boolean paid,
            BigDecimal paidAmount,
            BigDecimal cashTendered,
            BigDecimal cashChange,
            BigDecimal roundingAdjustment) {

        private static PaymentResult notPaid() {
            return new PaymentResult(false, null, null, null, null);
        }
    }


}
