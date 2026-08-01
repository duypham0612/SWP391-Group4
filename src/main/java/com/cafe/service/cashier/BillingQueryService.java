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

/** Các truy vấn bill/checkout không thay đổi trạng thái nghiệp vụ. */
public final class BillingQueryService {
    private final BillingRepository repository;

    public BillingQueryService() { this(new BillingRepository()); }
    BillingQueryService(BillingRepository repository) {
        this.repository = java.util.Objects.requireNonNull(repository);
    }

    public Bill getBill(int billId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            Bill b = repository.billDao.findById(c, billId);
            if (b != null) b.setItems(repository.billItemDao.findByBill(c, billId));
            return b;
        }
    }

    public List<Bill> getSessionBills(int sessionId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<Bill> bills = repository.billDao.findBySession(c, sessionId);
            bills.removeIf(b -> "VOID".equals(b.getStatus()));
            for (Bill b : bills) b.setItems(repository.billItemDao.findByBill(c, b.getBillId()));
            return bills;
        }
    }

    public List<Bill> getOrderBills(int orderId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<Bill> bills = repository.billDao.findByOrder(c, orderId);
            for (Bill b : bills) b.setItems(repository.billItemDao.findByBill(c, b.getBillId()));
            return bills;
        }
    }

    /** Toàn bộ điều kiện thu tiền và branch scope được kiểm tra tại Service. */
    public String validatePayable(int billId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            Bill bill = repository.billDao.findById(c, billId);
            if (bill == null || bill.getBranchId() != branchId) return "Không tìm thấy hoá đơn.";
            if (!"UNPAID".equals(bill.getStatus())) return "Hoá đơn đã được thanh toán hoặc đã huỷ.";
            List<BillItem> items = repository.billItemDao.findByBill(c, billId);
            if (items.isEmpty()) return "Hoá đơn chưa có món, không thể thanh toán.";
            if (items.stream().anyMatch(item -> !"SERVED".equals(item.getStatus())))
                return "Chưa thể thanh toán — Barista phải pha xong và Cashier phải bàn giao đủ món trước.";
            BigDecimal total = bill.getTotalAmount() == null ? BigDecimal.ZERO : bill.getTotalAmount();
            if (total.signum() <= 0) return "Tổng tiền hoá đơn phải lớn hơn 0.";
            if (bill.getVoucherId() != null) {
                Voucher voucher = repository.voucherDao.findById(c, bill.getVoucherId());
                return VoucherService.validateVoucherRecord(voucher, branchId, bill.getSubtotal());
            }
            return null;
        }
    }

    public TableSession getOpenSessionForCheckout(int sessionId, int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            TableSession session = repository.sessionDao.findById(c, sessionId);
            return session != null && session.getBranchId() == branchId && "OPEN".equals(session.getStatus())
                    ? session : null;
        }
    }

    /** Đơn mang đi chưa trả tiền, kể cả bill UNPAID đã được mở ở lần checkout trước. */
    public List<Order> getTakeawayOrdersAwaitingPayment(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            List<Order> orders = repository.orderDao.findTakeawayAwaitingPaymentByBranch(c, branchId);
            for (Order order : orders) order.setItems(repository.orderItemDao.findByOrder(c, order.getOrderId()));
            return orders;
        }
    }

    public List<Bill> getBillHistory(int branchId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return repository.billDao.findByBranch(c, branchId, 100); }
    }

    /** C6 · Lịch sử bill trong 1 ca thu ngân. */
    public List<Bill> getBillHistoryByShift(int shiftId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) { return repository.billDao.findByShift(c, shiftId, 200); }
    }


}
