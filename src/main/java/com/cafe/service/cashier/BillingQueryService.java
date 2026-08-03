package com.cafe.service.cashier;

import com.cafe.config.DBConnection;
import com.cafe.model.Bill;
import com.cafe.model.BillLine;
import com.cafe.model.DiningTable;
import com.cafe.model.Order;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import com.cafe.model.OrderItem;
import com.cafe.model.OrderItemModifier;

/** Các truy vấn bill/checkout không thay đổi trạng thái nghiệp vụ. */
public final class BillingQueryService {
    private final BillingRepository repository;

    public BillingQueryService() { this(new BillingRepository()); }
    BillingQueryService(BillingRepository repository) {
        this.repository = java.util.Objects.requireNonNull(repository);
    }

    public Bill getBill(int billId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Bill bill = repository.billDao.findById(conn, billId);
            if (bill != null) bill.setItems(repository.billLineDao.findByBill(conn, billId));
            return bill;
        }
    }

    public List<Bill> getTableBills(int tableId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Bill> bills = repository.billDao.findByTable(conn, tableId);
            bills.removeIf(bill -> "VOID".equals(bill.getStatus()));
            for (Bill bill : bills) bill.setItems(repository.billLineDao.findByBill(conn, bill.getBillId()));
            return bills;
        }
    }

    public List<Bill> getOrderBills(int orderId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Bill> bills = repository.billDao.findByOrder(conn, orderId);
            for (Bill bill : bills) bill.setItems(repository.billLineDao.findByBill(conn, bill.getBillId()));
            return bills;
        }
    }

    public String validatePayable(int billId, int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Bill bill = repository.billDao.findById(conn, billId);
            if (bill == null || bill.getBranchId() != branchId) return "Không tìm thấy hoá đơn.";
            if (!"UNPAID".equals(bill.getStatus())) return "Hoá đơn đã được thanh toán hoặc đã huỷ.";
            List<BillLine> items = repository.billLineDao.findByBill(conn, billId);
            if (items.isEmpty()) return "Hoá đơn chưa có món, không thể thanh toán.";
            if (items.stream().anyMatch(item -> !"SERVED".equals(item.getStatus()))) {
                return "Chưa thể thanh toán — Barista phải pha xong và Cashier phải bàn giao đủ món trước.";
            }
            BigDecimal total = bill.getTotalAmount() == null ? BigDecimal.ZERO : bill.getTotalAmount();
            return total.signum() <= 0 ? "Tổng tiền hoá đơn phải lớn hơn 0." : null;
        }
    }

    public DiningTable getOpenTableForCheckout(int tableId, int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DiningTable table = repository.tableDao.findById(conn, tableId);
            return table != null && table.getBranchId() == branchId
                    && "OCCUPIED".equals(table.getStatus()) ? table : null;
        }
    }

    public List<Order> getTakeawayOrdersAwaitingPayment(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Order> orders = repository.orderDao.findTakeawayAwaitingPaymentByBranch(conn, branchId);
            for (Order order : orders) {
                List<OrderItem> items = repository.orderItemDao.findByOrder(conn, order.getOrderId());
                Map<Integer, List<OrderItemModifier>> modifiers = repository.orderItemModifierDao.findByItems(
                        conn, items.stream().map(OrderItem::getOrderItemId).toList());
                for (OrderItem item : items) {
                    item.setModifiers(modifiers.getOrDefault(item.getOrderItemId(), List.of()));
                }
                order.setItems(items);
            }
            return orders;
        }
    }

    public List<Bill> getBillHistory(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.billDao.findByBranch(conn, branchId, 100);
        }
    }

    public List<Bill> getBillHistoryByShift(int shiftId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.billDao.findByShift(conn, shiftId, 200);
        }
    }
}
