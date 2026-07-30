package com.cafe.service.cashier;
import com.cafe.service.shared.VoucherService;

import com.cafe.common.BillCalculator;
import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.cashier.BillItemDao;
import com.cafe.dao.cashier.CashierShiftDao;
import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.dao.shared.OutboxEventDao;
import com.cafe.dao.cashier.TableSessionDao;
import com.cafe.dao.shared.OrderDao;
import com.cafe.dao.shared.VoucherDao;
import com.cafe.dao.cashier.VoucherRedemptionDao;
import com.cafe.model.Bill;
import com.cafe.model.BillItem;
import com.cafe.model.CashierShift;
import com.cafe.model.OrderItem;
import com.cafe.model.Order;
import com.cafe.model.TableSession;
import com.cafe.model.Voucher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * C5/C6 · BillingService — ★ Dynamic Bill Splitting + thanh toán (Contract #3: payment.completed).
 * Voucher 1 nguồn (payment.Voucher qua VoucherService.validateVoucher).
 */
public class BillingService {

    private static final Set<String> PAYMENT_METHODS =
            Set.of("CASH", "TRANSFER", "QR_BANK");

    private final BillDao billDao = new BillDao();
    private final BillItemDao billItemDao = new BillItemDao();
    private final CashierShiftDao cashierShiftDao = new CashierShiftDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final VoucherDao voucherDao = new VoucherDao();
    private final VoucherRedemptionDao redemptionDao = new VoucherRedemptionDao();
    private final TableSessionDao sessionDao = new TableSessionDao();
    private final DiningTableDao tableDao = new DiningTableDao();
    private final OrderDao orderDao = new OrderDao();
    private final VoucherService voucherService = new VoucherService();
    private final OutboxEventDao outboxEventDao = new OutboxEventDao();

    /**
     * Dựng/đồng bộ bill cho phiên: đảm bảo mọi dòng đơn (chưa thuộc bill nào, không CANCELLED)
     * đều nằm trên 1 bill mặc định UNPAID. Trả về danh sách bill (kèm dòng) của phiên.
     */
    public List<Bill> buildSessionBill(int sessionId, int branchId, Integer shiftId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Bill> bills = billDao.findUnpaidBySession(c, sessionId);
                Integer defaultBillId = bills.isEmpty() ? null : bills.get(0).getBillId();

                for (OrderItem it : orderItemDao.findBySession(c, sessionId)) {
                    if ("CANCELLED".equals(it.getStatus())) continue;
                    if (billItemDao.existsForOrderItem(c, it.getOrderItemId())) continue;
                    if (defaultBillId == null) defaultBillId = billDao.insert(c, branchId, sessionId, shiftId);
                    billItemDao.insert(c, defaultBillId, it.getOrderItemId(), it.getLineTotal());
                }
                recomputeSession(c, sessionId);   // no-drift: VAT/discount tính 1 lần rồi phân bổ
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
            return getSessionBills(sessionId);
        }
    }

    /**
     * Dựng bill cho một đơn mang đi không có phiên bàn. Idempotent: mở lại checkout không tạo bill
     * trùng vì mỗi OrderItem chỉ được gắn vào một BillItem.
     */
    public List<Bill> buildTakeawayBill(int orderId, int branchId, Integer shiftId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Order order = orderDao.findById(c, orderId);
                if (order == null || order.getBranchId() != branchId
                        || order.getTableSessionId() != null
                        || !"TAKEAWAY".equals(order.getOrderType())
                        || !"COMPLETED".equals(order.getStatus())) {
                    c.rollback();
                    return List.of();
                }

                List<Bill> bills = billDao.findByOrder(c, orderId);
                Integer defaultBillId = null;
                Integer voucherId = null;
                for (Bill bill : bills) {
                    if ("UNPAID".equals(bill.getStatus())) {
                        defaultBillId = bill.getBillId();
                        voucherId = bill.getVoucherId();
                        break;
                    }
                }

                boolean changed = false;
                for (OrderItem item : orderItemDao.findByOrder(c, orderId)) {
                    if ("CANCELLED".equals(item.getStatus())
                            || billItemDao.existsForOrderItem(c, item.getOrderItemId())) continue;
                    if (defaultBillId == null) {
                        defaultBillId = billDao.insert(c, branchId, null, shiftId);
                    }
                    billItemDao.insert(c, defaultBillId, item.getOrderItemId(), item.getLineTotal());
                    changed = true;
                }
                if (changed) recomputeWithVoucher(c, defaultBillId, voucherId);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        return getOrderBills(orderId);
    }

    /** ★ Tách: chuyển các dòng được chọn sang 1 bill MỚI; recompute cả hai. */
    public void splitItems(int sessionId, int branchId, Integer shiftId, List<Integer> billItemIds) throws SQLException {
        if (billItemIds == null || billItemIds.isEmpty()) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession session = sessionDao.findById(c, sessionId);
                if (session == null || session.getBranchId() != branchId
                        || !"OPEN".equals(session.getStatus())) {
                    throw new IllegalArgumentException("Phiên bàn không thuộc chi nhánh đang thao tác.");
                }
                int newBillId = billDao.insert(c, branchId, sessionId, shiftId);
                for (Integer biId : billItemIds) {
                    BillItem bi = billItemDao.findById(c, biId);
                    if (bi == null) continue;
                    Bill source = billDao.findById(c, bi.getBillId());
                    if (source == null || source.getBranchId() != branchId
                            || source.getTableSessionId() == null
                            || source.getTableSessionId() != sessionId
                            || !"UNPAID".equals(source.getStatus())) {
                        throw new IllegalArgumentException(
                                "Có dòng hoá đơn không thuộc phiên bàn đang thao tác.");
                    }
                    billItemDao.reassign(c, biId, newBillId);
                }
                // void các bill rỗng, rồi recompute toàn phiên no-drift (VAT + discount phân bổ theo tỷ lệ)
                for (Bill b : billDao.findUnpaidBySession(c, sessionId)) {
                    if (billItemDao.countByBill(c, b.getBillId()) == 0) billDao.markVoid(c, b.getBillId());
                }
                recomputeSession(c, sessionId);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Gộp: dồn mọi dòng của các bill vào bill đầu tiên; void các bill rỗng còn lại. */
    public void mergeBills(List<Integer> billIds, int branchId) throws SQLException {
        if (billIds == null || billIds.size() < 2) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int target = billIds.get(0);
                Bill targetBill = billDao.findById(c, target);
                if (targetBill == null || targetBill.getBranchId() != branchId
                        || targetBill.getTableSessionId() == null
                        || !"UNPAID".equals(targetBill.getStatus())) {
                    throw new IllegalArgumentException(
                            "Chỉ được gộp hoá đơn chưa thu của cùng một phiên bàn.");
                }
                for (int i = 1; i < billIds.size(); i++) {
                    int src = billIds.get(i);
                    Bill sourceBill = billDao.findById(c, src);
                    if (sourceBill == null || sourceBill.getBranchId() != branchId
                            || !"UNPAID".equals(sourceBill.getStatus())
                            || !java.util.Objects.equals(
                                    sourceBill.getTableSessionId(), targetBill.getTableSessionId())) {
                        throw new IllegalArgumentException(
                                "Chỉ được gộp hoá đơn chưa thu của cùng một phiên bàn.");
                    }
                    for (BillItem bi : billItemDao.findByBill(c, src)) billItemDao.reassign(c, bi.getBillItemId(), target);
                    billDao.markVoid(c, src);
                }
                recomputeForBill(c, target);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Áp voucher cho 1 bill. Trả về thông điệp lỗi, null nếu OK. */
    public String applyVoucher(int billId, String code, int branchId) throws SQLException {
        Bill bill = getBill(billId);
        if (bill == null) return "Không tìm thấy hoá đơn.";
        if (bill.getBranchId() != branchId) return "Hoá đơn không thuộc chi nhánh đang thao tác.";
        if (!"UNPAID".equals(bill.getStatus())) return "Hoá đơn đã thanh toán/huỷ.";
        String err = voucherService.validateVoucher(code, branchId, bill.getSubtotal());
        if (err != null) return err;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Voucher v = voucherDao.findByCode(c, code);
                Integer vid = v == null ? null : v.getVoucherId();
                if (bill.getTableSessionId() != null) {
                    // voucher áp cho cả tab → tính trên TỔNG phiên rồi phân bổ theo tỷ lệ (no-drift)
                    recomputeSession(c, bill.getTableSessionId(), vid);
                } else {
                    recomputeWithVoucher(c, billId, vid);   // bill takeaway lẻ (không phiên)
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
        return null;
    }

    public String validateBillVoucher(int billId, int branchId) throws SQLException {
        Bill bill = getBill(billId);
        if (bill == null || bill.getVoucherId() == null) return null;
        return voucherService.validateVoucherById(bill.getVoucherId(), branchId, bill.getSubtotal());
    }

    public void removeVoucher(int billId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Bill b = billDao.findById(c, billId);
                if (b == null || b.getBranchId() != branchId
                        || !"UNPAID".equals(b.getStatus())) {
                    throw new IllegalArgumentException(
                            "Hoá đơn không thuộc chi nhánh đang thao tác hoặc đã được chốt.");
                }
                if (b.getTableSessionId() != null) recomputeSession(c, b.getTableSessionId(), null);
                else recomputeWithVoucher(c, billId, null);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
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
                CashierShift shift = cashierShiftDao.findOpenByIdForUpdate(c, shiftId);
                if (shift == null) {
                    c.rollback();
                    return PaymentResult.notPaid();
                }
                Bill bill = billDao.findByIdForUpdate(c, billId);
                if (bill == null || bill.getBranchId() != shift.getBranchId()
                        || !"UNPAID".equals(bill.getStatus())) {
                    c.rollback();
                    return PaymentResult.notPaid();
                }
                if (bill.getVoucherId() != null) {
                    Voucher voucher = voucherDao.findById(c, bill.getVoucherId());
                    String voucherError = VoucherService.validateVoucherRecord(voucher, bill.getBranchId(), bill.getSubtotal());
                    if (voucherError != null) { c.rollback(); return PaymentResult.notPaid(); }
                }
                if (billItemDao.countByBill(c, billId) == 0
                        || bill.getTotalAmount() == null
                        || bill.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    c.rollback();
                    return PaymentResult.notPaid();
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

                int rows = billDao.markPaid(c, billId, method, shiftId,
                        adjustment, paidAmount, tendered, change);
                if (rows == 0) { c.rollback(); return PaymentResult.notPaid(); }

                if (bill.getVoucherId() != null) {
                    voucherDao.incrementUsed(c, bill.getVoucherId());
                    redemptionDao.insert(c, bill.getVoucherId(), billId, bill.getDiscountAmount());
                }
                EventPublisher.publish(c, EventType.PAYMENT_COMPLETED, String.valueOf(billId), bill.getBranchId(),
                        paymentPayload(billId, method, bill.getTotalAmount(), adjustment,
                                paidAmount, tendered, change));

                // Nếu phiên không còn bill UNPAID → đóng phiên + trả bàn EMPTY
                if (bill.getTableSessionId() != null
                        && billDao.findUnpaidBySession(c, bill.getTableSessionId()).isEmpty()) {
                    TableSession s = sessionDao.findById(c, bill.getTableSessionId());
                    if (s != null && "OPEN".equals(s.getStatus())) {
                        sessionDao.updateStatus(c, s.getTableSessionId(), "CLOSED", true);
                        tableDao.updateStatus(c, s.getDiningTableId(), "EMPTY");
                    }
                    outboxEventDao.markBillRequestProcessed(c, bill.getTableSessionId());
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

    /** Huỷ bill chưa thanh toán KÈM LÝ DO — ghi log qua ops.OutboxEvent trong cùng tx. */
    public boolean voidBill(int billId, int branchId, String reason, Integer userId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Bill b = billDao.findById(c, billId);
                if (b == null || b.getBranchId() != branchId) {
                    c.rollback();
                    return false;
                }
                int r = billDao.markVoid(c, billId);
                if (r > 0) {
                    String safeReason = reason == null ? "" : reason.replace("\"", "'");
                    EventPublisher.publish(c, EventType.BILL_VOIDED, String.valueOf(billId), b.getBranchId(),
                            "{\"billId\":" + billId + ",\"by\":" + userId + ",\"reason\":\"" + safeReason + "\"}");
                }
                c.commit();
                return r > 0;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    // ---------- đọc ----------
    public Bill getBill(int billId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            Bill b = billDao.findById(c, billId);
            if (b != null) b.setItems(billItemDao.findByBill(c, billId));
            return b;
        }
    }

    public List<Bill> getSessionBills(int sessionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<Bill> bills = billDao.findBySession(c, sessionId);
            bills.removeIf(b -> "VOID".equals(b.getStatus()));
            for (Bill b : bills) b.setItems(billItemDao.findByBill(c, b.getBillId()));
            return bills;
        }
    }

    public List<Bill> getOrderBills(int orderId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<Bill> bills = billDao.findByOrder(c, orderId);
            for (Bill b : bills) b.setItems(billItemDao.findByBill(c, b.getBillId()));
            return bills;
        }
    }

    /** Đơn mang đi chưa trả tiền, kể cả bill UNPAID đã được mở ở lần checkout trước. */
    public List<Order> getTakeawayOrdersAwaitingPayment(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<Order> orders = orderDao.findTakeawayAwaitingPaymentByBranch(c, branchId);
            for (Order order : orders) order.setItems(orderItemDao.findByOrder(c, order.getOrderId()));
            return orders;
        }
    }

    public List<Bill> getBillHistory(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return billDao.findByBranch(c, branchId, 100); }
    }

    /** C6 · Lịch sử bill trong 1 ca thu ngân. */
    public List<Bill> getBillHistoryByShift(int shiftId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return billDao.findByShift(c, shiftId, 200); }
    }

    // ---------- nội bộ ----------
    /** Recompute đúng phạm vi: bill có phiên → no-drift toàn phiên; bill lẻ (takeaway) → 1 bill. */
    private void recomputeForBill(Connection c, int billId) throws SQLException {
        Bill b = billDao.findById(c, billId);
        if (b == null) return;
        if (b.getTableSessionId() != null) recomputeSession(c, b.getTableSessionId());
        else recomputeWithVoucher(c, billId, b.getVoucherId());
    }

    /** Recompute toàn phiên, voucher suy ra từ các bill hiện có (bill đầu giữ tham chiếu). */
    private void recomputeSession(Connection c, int sessionId) throws SQLException {
        Integer vid = null;
        for (Bill b : billDao.findUnpaidBySession(c, sessionId)) {
            if (b.getVoucherId() != null) { vid = b.getVoucherId(); break; }
        }
        recomputeSession(c, sessionId, vid);
    }

    /**
     * ★ No-drift: tính discount + VAT MỘT LẦN trên TỔNG phiên, rồi phân bổ theo tỷ lệ (largest-remainder)
     * cho từng bill → Σ(discount)·Σ(vat)·Σ(total) các bill == bản tính trên cả tab (không lệch ±0.01).
     * Voucher: discount chia theo tỷ lệ subtotal; tham chiếu VoucherId chỉ gắn ở bill đầu (đếm lượt dùng 1 lần).
     */
    private void recomputeSession(Connection c, int sessionId, Integer voucherId) throws SQLException {
        List<Bill> bills = billDao.findUnpaidBySession(c, sessionId);
        if (bills.isEmpty()) return;

        List<BigDecimal> subs = new java.util.ArrayList<>();
        BigDecimal sessionSubtotal = BigDecimal.ZERO;
        for (Bill b : bills) {
            BigDecimal s = BigDecimal.ZERO;
            for (BillItem bi : billItemDao.findByBill(c, b.getBillId())) s = s.add(bi.getAmount());
            subs.add(s);
            sessionSubtotal = sessionSubtotal.add(s);
        }

        BigDecimal sessionDiscount = BigDecimal.ZERO;
        if (voucherId != null) {
            Voucher v = voucherDao.findById(c, voucherId);
            if (v != null) sessionDiscount = BillCalculator.computeDiscount(v.getDiscountType(), v.getDiscountValue(), sessionSubtotal);
        }
        List<BigDecimal> discParts = BillCalculator.allocateByWeight(sessionDiscount, subs);

        List<BigDecimal> nets = new java.util.ArrayList<>();
        BigDecimal sessionNet = BigDecimal.ZERO;
        for (int i = 0; i < bills.size(); i++) {
            BigDecimal net = subs.get(i).subtract(discParts.get(i));
            if (net.signum() < 0) net = BigDecimal.ZERO;
            nets.add(net);
            sessionNet = sessionNet.add(net);
        }
        BigDecimal sessionVat = BillCalculator.computeVat(sessionNet);
        List<BigDecimal> vatParts = BillCalculator.allocateByWeight(sessionVat, nets);

        for (int i = 0; i < bills.size(); i++) {
            Bill b = bills.get(i);
            BigDecimal total = nets.get(i).add(vatParts.get(i)).setScale(2, java.math.RoundingMode.HALF_UP);
            Integer vid = (i == 0) ? voucherId : null;   // gắn voucher ở bill đầu → incrementUsed đúng 1 lần
            billDao.updateAmounts(c, b.getBillId(), subs.get(i), discParts.get(i), vatParts.get(i), total, vid);
        }
    }

    /** Tính lại subtotal/discount/vat/total cho bill lẻ (không thuộc phiên), có thể đổi voucher. */
    private void recomputeWithVoucher(Connection c, int billId, Integer voucherId) throws SQLException {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (BillItem bi : billItemDao.findByBill(c, billId)) subtotal = subtotal.add(bi.getAmount());

        BigDecimal discount = BigDecimal.ZERO;
        if (voucherId != null) {
            Voucher v = voucherDao.findById(c, voucherId);
            if (v != null) discount = BillCalculator.computeDiscount(v.getDiscountType(), v.getDiscountValue(), subtotal);
        }
        BigDecimal vat = BillCalculator.computeVat(subtotal.subtract(discount));
        BigDecimal total = BillCalculator.computeTotal(subtotal, discount);
        billDao.updateAmounts(c, billId, subtotal, discount, vat, total, voucherId);
    }
}
