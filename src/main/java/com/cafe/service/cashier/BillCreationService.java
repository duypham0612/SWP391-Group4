package com.cafe.service.cashier;

import com.cafe.config.Tx;
import com.cafe.model.Bill;
import com.cafe.model.BillLine;
import com.cafe.model.DiningTable;
import com.cafe.model.Order;
import com.cafe.model.OrderItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Tạo, tách/gộp bill và cập nhật giảm giá thủ công trong transaction use case. */
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

    /** Gom mọi dòng chưa thanh toán theo (DiningTableId, BranchId) vào bill UNPAID mặc định. */
    public List<Bill> buildTableBill(int tableId, int branchId, Integer shiftId) throws SQLException {
        boolean built = Tx.call(conn -> {
            DiningTable table = repository.tableDao.findByIdForUpdate(conn, tableId);
            if (table == null || table.getBranchId() != branchId
                    || !"OCCUPIED".equals(table.getStatus())) {
                return false;
            }
            List<Bill> bills = repository.billDao.findUnpaidByTable(conn, tableId);
            Integer defaultBillId = bills.isEmpty() ? null : bills.get(0).getBillId();
            if (defaultBillId != null) repository.billDao.findByIdForUpdate(conn, defaultBillId);
            boolean changed = false;
            for (OrderItem item : repository.itemQueryDao.findByTable(conn, tableId)) {
                if ("CANCELLED".equals(item.getStatus()) || item.getBillId() != null) continue;
                if (defaultBillId == null) defaultBillId = repository.billDao.insert(conn, branchId, shiftId);
                repository.billLineDao.insert(conn, defaultBillId, item.getOrderItemId());
                changed = true;
            }
            if (changed) repository.recomputeForBill(conn, defaultBillId);
            return true;
        });
        // Bàn không hợp lệ -> danh sách rỗng, ĐÚNG như bản cũ return List.of() ngay trong tx.
        return built ? queryService.getTableBills(tableId) : List.of();
    }

    public List<Bill> buildTakeawayBill(int orderId, int branchId, Integer shiftId) throws SQLException {
        boolean built = Tx.call(conn -> {
            Order order = repository.orderDao.findById(conn, orderId);
            if (order == null || order.getBranchId() != branchId
                    || order.getDiningTableId() != null
                    || !"TAKEAWAY".equals(order.getOrderType())
                    || !"COMPLETED".equals(order.getStatus())) {
                return false;
            }
            List<Bill> bills = repository.billDao.findByOrder(conn, orderId);
            Integer defaultBillId = null;
            for (Bill bill : bills) {
                if ("UNPAID".equals(bill.getStatus())) {
                    defaultBillId = bill.getBillId();
                    break;
                }
            }
            if (defaultBillId != null) repository.billDao.findByIdForUpdate(conn, defaultBillId);
            boolean changed = false;
            for (OrderItem item : repository.orderItemDao.findByOrder(conn, orderId)) {
                if ("CANCELLED".equals(item.getStatus()) || item.getBillId() != null) continue;
                if (defaultBillId == null) defaultBillId = repository.billDao.insert(conn, branchId, shiftId);
                repository.billLineDao.insert(conn, defaultBillId, item.getOrderItemId());
                changed = true;
            }
            if (changed) repository.recomputeForBill(conn, defaultBillId);
            return true;
        });
        return built ? queryService.getOrderBills(orderId) : List.of();
    }

    public void splitItems(int tableId, int branchId, Integer shiftId, List<Integer> orderItemIds)
            throws SQLException {
        if (orderItemIds == null || orderItemIds.isEmpty()) return;
        Tx.run(conn -> {
            DiningTable table = repository.tableDao.findByIdForUpdate(conn, tableId);
            if (table == null || table.getBranchId() != branchId
                    || !"OCCUPIED".equals(table.getStatus())) {
                throw new IllegalArgumentException("Bàn không thuộc chi nhánh đang thao tác.");
            }
            int newBillId = repository.billDao.insert(conn, branchId, shiftId);
            Set<Integer> sourceBillIds = new LinkedHashSet<>();
            for (Integer orderItemId : orderItemIds) {
                BillLine item = repository.billLineDao.findById(conn, orderItemId);
                if (item == null) continue;
                Bill source = repository.billDao.findByIdForUpdate(conn, item.getBillId());
                if (source == null || source.getBranchId() != branchId
                        || !java.util.Objects.equals(source.getDiningTableId(), tableId)
                        || !"UNPAID".equals(source.getStatus())) {
                    throw new IllegalArgumentException("Có dòng hoá đơn không thuộc bàn đang thao tác.");
                }
                sourceBillIds.add(source.getBillId());
                repository.billLineDao.reassign(conn, orderItemId, newBillId);
            }
            if (repository.billLineDao.countByBill(conn, newBillId) == 0) {
                repository.billDao.markVoid(conn, newBillId);
            } else {
                repository.recomputeForBill(conn, newBillId);
            }
            for (Integer sourceId : sourceBillIds) {
                if (repository.billLineDao.countByBill(conn, sourceId) == 0) {
                    repository.billDao.markVoid(conn, sourceId);
                } else {
                    repository.recomputeForBill(conn, sourceId);
                }
            }
        });
    }

    public void mergeBills(List<Integer> billIds, int branchId) throws SQLException {
        if (billIds == null || billIds.size() < 2) return;
        Tx.run(conn -> {
            Bill target = repository.billDao.findByIdForUpdate(conn, billIds.get(0));
            if (target == null || target.getBranchId() != branchId
                    || !"UNPAID".equals(target.getStatus())) {
                throw new IllegalArgumentException("Bill đích không hợp lệ.");
            }
            BigDecimal mergedDiscount = value(target.getDiscountAmount());
            for (int i = 1; i < billIds.size(); i++) {
                Bill source = repository.billDao.findByIdForUpdate(conn, billIds.get(i));
                if (source == null || source.getBranchId() != branchId
                        || !"UNPAID".equals(source.getStatus())
                        || !java.util.Objects.equals(source.getDiningTableId(), target.getDiningTableId())) {
                    throw new IllegalArgumentException("Chỉ được gộp bill chưa thu của cùng một bàn.");
                }
                mergedDiscount = mergedDiscount.add(value(source.getDiscountAmount()));
                for (BillLine item : repository.billLineDao.findByBill(conn, source.getBillId())) {
                    repository.billLineDao.reassign(conn, item.getOrderItemId(), target.getBillId());
                }
                repository.billDao.markVoid(conn, source.getBillId());
            }
            repository.recomputeWithDiscount(conn, target.getBillId(), mergedDiscount);
        });
    }

    public void setDiscount(int billId, BigDecimal discount, int branchId) throws SQLException {
        if (discount == null || discount.signum() < 0) {
            throw new IllegalArgumentException("Giảm giá không hợp lệ.");
        }
        Tx.run(conn -> {
            Bill bill = repository.billDao.findByIdForUpdate(conn, billId);
            if (bill == null || bill.getBranchId() != branchId
                    || !"UNPAID".equals(bill.getStatus())) {
                throw new IllegalArgumentException("Hoá đơn không còn hợp lệ để cập nhật giảm giá.");
            }
            repository.recomputeWithDiscount(conn, billId, discount);
        });
    }

    private static BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
