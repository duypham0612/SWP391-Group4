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

/** Tạo, tách/gộp bill và áp voucher trong transaction của use case. */
public final class BillCreationService {
    private final BillingRepository repository;
    private final BillingQueryService queryService;

    public BillCreationService() { this(new BillingRepository()); }
    BillCreationService(BillingRepository repository) {
        this(repository, new BillingQueryService(repository));
    }
    BillCreationService(BillingRepository repository, BillingQueryService queryService) {
        this.repository = java.util.Objects.requireNonNull(repository);
        this.queryService = java.util.Objects.requireNonNull(queryService);
    }

    /**
     * Dựng/đồng bộ bill cho phiên: đảm bảo mọi dòng đơn (chưa thuộc bill nào, không CANCELLED)
     * đều nằm trên 1 bill mặc định UNPAID. Trả về danh sách bill (kèm dòng) của phiên.
     */
    public List<Bill> buildSessionBill(int sessionId, int branchId, Integer shiftId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Bill> bills = repository.billDao.findUnpaidBySession(c, sessionId);
                Integer defaultBillId = bills.isEmpty() ? null : bills.get(0).getBillId();

                for (OrderItem it : repository.orderItemDao.findBySession(c, sessionId)) {
                    if ("CANCELLED".equals(it.getStatus())) continue;
                    if (repository.billItemDao.existsForOrderItem(c, it.getOrderItemId())) continue;
                    if (defaultBillId == null) defaultBillId = repository.billDao.insert(c, branchId, sessionId, shiftId);
                    repository.billItemDao.insert(c, defaultBillId, it.getOrderItemId(), it.getLineTotal());
                }
                repository.recomputeSession(c, sessionId);   // no-drift: VAT/discount tính 1 lần rồi phân bổ
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
            return queryService.getSessionBills(sessionId);
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
                Order order = repository.orderDao.findById(c, orderId);
                if (order == null || order.getBranchId() != branchId
                        || order.getTableSessionId() != null
                        || !"TAKEAWAY".equals(order.getOrderType())
                        || !"COMPLETED".equals(order.getStatus())) {
                    c.rollback();
                    return List.of();
                }

                List<Bill> bills = repository.billDao.findByOrder(c, orderId);
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
                for (OrderItem item : repository.orderItemDao.findByOrder(c, orderId)) {
                    if ("CANCELLED".equals(item.getStatus())
                            || repository.billItemDao.existsForOrderItem(c, item.getOrderItemId())) continue;
                    if (defaultBillId == null) {
                        defaultBillId = repository.billDao.insert(c, branchId, null, shiftId);
                    }
                    repository.billItemDao.insert(c, defaultBillId, item.getOrderItemId(), item.getLineTotal());
                    changed = true;
                }
                if (changed) repository.recomputeWithVoucher(c, defaultBillId, voucherId);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        return queryService.getOrderBills(orderId);
    }

    /** ★ Tách: chuyển các dòng được chọn sang 1 bill MỚI; recompute cả hai. */
    public void splitItems(int sessionId, int branchId, Integer shiftId, List<Integer> billItemIds) throws SQLException {
        if (billItemIds == null || billItemIds.isEmpty()) return;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TableSession session = repository.sessionDao.findById(c, sessionId);
                if (session == null || session.getBranchId() != branchId
                        || !"OPEN".equals(session.getStatus())) {
                    throw new IllegalArgumentException("Phiên bàn không thuộc chi nhánh đang thao tác.");
                }
                int newBillId = repository.billDao.insert(c, branchId, sessionId, shiftId);
                for (Integer biId : billItemIds) {
                    BillItem bi = repository.billItemDao.findById(c, biId);
                    if (bi == null) continue;
                    Bill source = repository.billDao.findById(c, bi.getBillId());
                    if (source == null || source.getBranchId() != branchId
                            || source.getTableSessionId() == null
                            || source.getTableSessionId() != sessionId
                            || !"UNPAID".equals(source.getStatus())) {
                        throw new IllegalArgumentException(
                                "Có dòng hoá đơn không thuộc phiên bàn đang thao tác.");
                    }
                    repository.billItemDao.reassign(c, biId, newBillId);
                }
                // void các bill rỗng, rồi recompute toàn phiên no-drift (VAT + discount phân bổ theo tỷ lệ)
                for (Bill b : repository.billDao.findUnpaidBySession(c, sessionId)) {
                    if (repository.billItemDao.countByBill(c, b.getBillId()) == 0) repository.billDao.markVoid(c, b.getBillId());
                }
                repository.recomputeSession(c, sessionId);
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
                Bill targetBill = repository.billDao.findById(c, target);
                if (targetBill == null || targetBill.getBranchId() != branchId
                        || targetBill.getTableSessionId() == null
                        || !"UNPAID".equals(targetBill.getStatus())) {
                    throw new IllegalArgumentException(
                            "Chỉ được gộp hoá đơn chưa thu của cùng một phiên bàn.");
                }
                for (int i = 1; i < billIds.size(); i++) {
                    int src = billIds.get(i);
                    Bill sourceBill = repository.billDao.findById(c, src);
                    if (sourceBill == null || sourceBill.getBranchId() != branchId
                            || !"UNPAID".equals(sourceBill.getStatus())
                            || !java.util.Objects.equals(
                                    sourceBill.getTableSessionId(), targetBill.getTableSessionId())) {
                        throw new IllegalArgumentException(
                                "Chỉ được gộp hoá đơn chưa thu của cùng một phiên bàn.");
                    }
                    for (BillItem bi : repository.billItemDao.findByBill(c, src)) repository.billItemDao.reassign(c, bi.getBillItemId(), target);
                    repository.billDao.markVoid(c, src);
                }
                repository.recomputeForBill(c, target);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Áp voucher cho 1 bill. Trả về thông điệp lỗi, null nếu OK. */
    public String applyVoucher(int billId, String code, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Bill bill = repository.billDao.findByIdForUpdate(c, billId);
                if (bill == null) { c.rollback(); return "Không tìm thấy hoá đơn."; }
                if (bill.getBranchId() != branchId) {
                    c.rollback();
                    return "Hoá đơn không thuộc chi nhánh đang thao tác.";
                }
                if (!"UNPAID".equals(bill.getStatus())) {
                    c.rollback();
                    return "Hoá đơn đã thanh toán/huỷ.";
                }

                Voucher v = repository.voucherDao.findByCodeForUpdate(c, code == null ? "" : code.trim());
                String err = VoucherService.validateVoucherRecord(
                        v, branchId, repository.voucherBaseAmount(c, bill));
                if (err != null) { c.rollback(); return err; }
                int vid = v.getVoucherId();
                if (bill.getTableSessionId() != null) {
                    // voucher áp cho cả tab → tính trên TỔNG phiên rồi phân bổ theo tỷ lệ (no-drift)
                    repository.recomputeSession(c, bill.getTableSessionId(), vid);
                } else {
                    repository.recomputeWithVoucher(c, billId, vid);   // bill takeaway lẻ (không phiên)
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
        return null;
    }

    public String validateBillVoucher(int billId, int branchId) throws SQLException {
        Bill bill = queryService.getBill(billId);
        if (bill == null || bill.getVoucherId() == null) return null;
        return repository.voucherService.validateVoucherById(bill.getVoucherId(), branchId, bill.getSubtotal());
    }

    public void removeVoucher(int billId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Bill b = repository.billDao.findByIdForUpdate(c, billId);
                if (b == null || b.getBranchId() != branchId
                        || !"UNPAID".equals(b.getStatus())) {
                    throw new IllegalArgumentException(
                            "Hoá đơn không thuộc chi nhánh đang thao tác hoặc đã được chốt.");
                }
                if (b.getTableSessionId() != null) repository.recomputeSession(c, b.getTableSessionId(), null);
                else repository.recomputeWithVoucher(c, billId, null);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }


}
