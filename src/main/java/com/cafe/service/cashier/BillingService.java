package com.cafe.service.cashier;
import com.cafe.service.shared.VoucherService;

import com.cafe.common.BillCalculator;
import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.cashier.BillItemDao;
import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.dao.cashier.TableSessionDao;
import com.cafe.dao.shared.VoucherDao;
import com.cafe.dao.cashier.VoucherRedemptionDao;
import com.cafe.model.Bill;
import com.cafe.model.BillItem;
import com.cafe.model.OrderItem;
import com.cafe.model.TableSession;
import com.cafe.model.Voucher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * C5/C6 · BillingService — ★ Dynamic Bill Splitting + thanh toán (Contract #3: payment.completed).
 * Voucher 1 nguồn (payment.Voucher qua VoucherService.validateVoucher).
 */
public class BillingService {

    private final BillDao billDao = new BillDao();
    private final BillItemDao billItemDao = new BillItemDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final VoucherDao voucherDao = new VoucherDao();
    private final VoucherRedemptionDao redemptionDao = new VoucherRedemptionDao();
    private final TableSessionDao sessionDao = new TableSessionDao();
    private final DiningTableDao tableDao = new DiningTableDao();
    private final VoucherService voucherService = new VoucherService();

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
                // recompute mọi bill UNPAID của phiên
                for (Bill b : billDao.findUnpaidBySession(c, sessionId)) recompute(c, b.getBillId());
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
            return getSessionBills(sessionId);
        }
    }

    /** ★ Tách: chuyển các dòng được chọn sang 1 bill MỚI; recompute cả hai. */
    public void splitItems(int sessionId, int branchId, Integer shiftId, List<Integer> billItemIds) throws SQLException {
        if (billItemIds == null || billItemIds.isEmpty()) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int newBillId = billDao.insert(c, branchId, sessionId, shiftId);
                for (Integer biId : billItemIds) {
                    BillItem bi = billItemDao.findById(c, biId);
                    if (bi != null) billItemDao.reassign(c, biId, newBillId);
                }
                for (Bill b : billDao.findUnpaidBySession(c, sessionId)) {
                    if (billItemDao.countByBill(c, b.getBillId()) == 0) billDao.markVoid(c, b.getBillId());
                    else recompute(c, b.getBillId());
                }
                recompute(c, newBillId);
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Gộp: dồn mọi dòng của các bill vào bill đầu tiên; void các bill rỗng còn lại. */
    public void mergeBills(List<Integer> billIds) throws SQLException {
        if (billIds == null || billIds.size() < 2) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int target = billIds.get(0);
                for (int i = 1; i < billIds.size(); i++) {
                    int src = billIds.get(i);
                    for (BillItem bi : billItemDao.findByBill(c, src)) billItemDao.reassign(c, bi.getBillItemId(), target);
                    billDao.markVoid(c, src);
                }
                recompute(c, target);
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Áp voucher cho 1 bill. Trả về thông điệp lỗi, null nếu OK. */
    public String applyVoucher(int billId, String code, int branchId) throws SQLException {
        Bill bill = getBill(billId);
        if (bill == null) return "Không tìm thấy hoá đơn.";
        if (!"UNPAID".equals(bill.getStatus())) return "Hoá đơn đã thanh toán/huỷ.";
        String err = voucherService.validateVoucher(code, branchId, bill.getSubtotal());
        if (err != null) return err;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Voucher v = voucherDao.findByCode(c, code);
                recomputeWithVoucher(c, billId, v == null ? null : v.getVoucherId());
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
        return null;
    }

    public void removeVoucher(int billId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { recomputeWithVoucher(c, billId, null); c.commit(); }
            catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Thanh toán 1 bill (Contract #3). Chống double-pay. Trả false nếu đã thanh toán. */
    public boolean payBill(int billId, String method) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Bill bill = billDao.findById(c, billId);
                if (bill == null) { c.rollback(); return false; }
                int rows = billDao.markPaid(c, billId, method);
                if (rows == 0) { c.rollback(); return false; }   // đã PAID/VOID

                if (bill.getVoucherId() != null) {
                    voucherDao.incrementUsed(c, bill.getVoucherId());
                    redemptionDao.insert(c, bill.getVoucherId(), billId, bill.getDiscountAmount());
                }
                EventPublisher.publish(c, EventType.PAYMENT_COMPLETED, String.valueOf(billId), bill.getBranchId(),
                        "{\"billId\":" + billId + ",\"method\":\"" + method + "\",\"total\":" + bill.getTotalAmount() + "}");

                // Nếu phiên không còn bill UNPAID → đóng phiên + trả bàn EMPTY
                if (bill.getTableSessionId() != null
                        && billDao.findUnpaidBySession(c, bill.getTableSessionId()).isEmpty()) {
                    TableSession s = sessionDao.findById(c, bill.getTableSessionId());
                    if (s != null && "OPEN".equals(s.getStatus())) {
                        sessionDao.updateStatus(c, s.getTableSessionId(), "CLOSED", true);
                        tableDao.updateStatus(c, s.getDiningTableId(), "EMPTY");
                    }
                }
                c.commit();
                return true;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public boolean voidBill(int billId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try { int r = billDao.markVoid(c, billId); c.commit(); return r > 0; }
            catch (SQLException e) { c.rollback(); throw e; }
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

    public List<Bill> getBillHistory(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return billDao.findByBranch(c, branchId, 100); }
    }

    // ---------- nội bộ ----------
    private void recompute(Connection c, int billId) throws SQLException {
        Bill b = billDao.findById(c, billId);
        recomputeWithVoucher(c, billId, b == null ? null : b.getVoucherId());
    }

    /** Tính lại subtotal/discount/vat/total cho bill, có thể đổi voucher. */
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
