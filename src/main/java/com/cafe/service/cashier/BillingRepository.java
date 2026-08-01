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

final class BillingRepository {
    final BillDao billDao;
    final BillItemDao billItemDao;
    final CashierShiftDao cashierShiftDao;
    final OrderItemDao orderItemDao;
    final VoucherDao voucherDao;
    final TableSessionDao sessionDao;
    final DiningTableDao tableDao;
    final OrderDao orderDao;
    final VoucherService voucherService;
    final OutboxEventDao outboxEventDao;

    BillingRepository() {
        this(new BillDao(), new BillItemDao(), new CashierShiftDao(), new OrderItemDao(),
                new VoucherDao(), new TableSessionDao(), new DiningTableDao(), new OrderDao(),
                new VoucherService(), new OutboxEventDao());
    }

    BillingRepository(BillDao billDao, BillItemDao billItemDao, CashierShiftDao cashierShiftDao,
                      OrderItemDao orderItemDao, VoucherDao voucherDao, TableSessionDao sessionDao,
                      DiningTableDao tableDao, OrderDao orderDao, VoucherService voucherService,
                      OutboxEventDao outboxEventDao) {
        this.billDao = java.util.Objects.requireNonNull(billDao);
        this.billItemDao = java.util.Objects.requireNonNull(billItemDao);
        this.cashierShiftDao = java.util.Objects.requireNonNull(cashierShiftDao);
        this.orderItemDao = java.util.Objects.requireNonNull(orderItemDao);
        this.voucherDao = java.util.Objects.requireNonNull(voucherDao);
        this.sessionDao = java.util.Objects.requireNonNull(sessionDao);
        this.tableDao = java.util.Objects.requireNonNull(tableDao);
        this.orderDao = java.util.Objects.requireNonNull(orderDao);
        this.voucherService = java.util.Objects.requireNonNull(voucherService);
        this.outboxEventDao = java.util.Objects.requireNonNull(outboxEventDao);
    }

    /** Recompute đúng phạm vi: bill có phiên → no-drift toàn phiên; bill lẻ (takeaway) → 1 bill. */
    void recomputeForBill(Connection c, int billId) throws SQLException {
        Bill b = billDao.findById(c, billId);
        if (b == null) return;
        if (b.getTableSessionId() != null) recomputeSession(c, b.getTableSessionId());
        else recomputeWithVoucher(c, billId, b.getVoucherId());
    }

    /** Recompute toàn phiên, voucher suy ra từ các bill hiện có (bill đầu giữ tham chiếu). */
    void recomputeSession(Connection c, int sessionId) throws SQLException {
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
    void recomputeSession(Connection c, int sessionId, Integer voucherId) throws SQLException {
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
            requireAmountUpdate(c, b.getBillId(), subs.get(i), discParts.get(i), vatParts.get(i), total, vid);
        }
    }

    /** Tính lại subtotal/discount/vat/total cho bill lẻ (không thuộc phiên), có thể đổi voucher. */
    void recomputeWithVoucher(Connection c, int billId, Integer voucherId) throws SQLException {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (BillItem bi : billItemDao.findByBill(c, billId)) subtotal = subtotal.add(bi.getAmount());

        BigDecimal discount = BigDecimal.ZERO;
        if (voucherId != null) {
            Voucher v = voucherDao.findById(c, voucherId);
            if (v != null) discount = BillCalculator.computeDiscount(v.getDiscountType(), v.getDiscountValue(), subtotal);
        }
        BigDecimal vat = BillCalculator.computeVat(subtotal.subtract(discount));
        BigDecimal total = BillCalculator.computeTotal(subtotal, discount);
        requireAmountUpdate(c, billId, subtotal, discount, vat, total, voucherId);
    }

    BigDecimal voucherBaseAmount(Connection c, Bill bill) throws SQLException {
        if (bill.getTableSessionId() == null) {
            return billItemDao.findByBill(c, bill.getBillId()).stream()
                    .map(BillItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Bill sessionBill : billDao.findUnpaidBySession(c, bill.getTableSessionId())) {
            for (BillItem item : billItemDao.findByBill(c, sessionBill.getBillId())) {
                total = total.add(item.getAmount());
            }
        }
        return total;
    }

    private void requireAmountUpdate(Connection c, int billId, BigDecimal subtotal,
                                     BigDecimal discount, BigDecimal vat, BigDecimal total,
                                     Integer voucherId) throws SQLException {
        if (billDao.updateAmounts(c, billId, subtotal, discount, vat, total, voucherId) != 1) {
            throw new BusinessException("Hoá đơn vừa được chốt bởi thao tác khác. Vui lòng tải lại.");
        }
    }
}
