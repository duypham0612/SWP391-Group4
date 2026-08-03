package com.cafe.service.cashier;

import com.cafe.common.BillCalculator;
import com.cafe.common.BusinessException;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.cashier.BillLineDao;
import com.cafe.dao.cashier.CashierShiftDao;
import com.cafe.dao.cashier.DiningTableDao;
import com.cafe.dao.shared.OrderDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.dao.shared.OrderItemQueryDao;
import com.cafe.dao.shared.OrderItemModifierDao;
import com.cafe.dao.shared.OutboxEventDao;
import com.cafe.model.Bill;
import com.cafe.model.BillLine;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

final class BillingRepository {
    final BillDao billDao;
    final BillLineDao billLineDao;
    final CashierShiftDao cashierShiftDao;
    final OrderItemDao orderItemDao;
    final OrderItemQueryDao itemQueryDao;
    final OrderItemModifierDao orderItemModifierDao;
    final DiningTableDao tableDao;
    final OrderDao orderDao;
    final OutboxEventDao outboxEventDao;

    BillingRepository() {
        this(new BillDao(), new BillLineDao(), new CashierShiftDao(), new OrderItemDao(), new OrderItemQueryDao(),
                new OrderItemModifierDao(),
                new DiningTableDao(), new OrderDao(), new OutboxEventDao());
    }

    BillingRepository(BillDao billDao, BillLineDao billLineDao, CashierShiftDao cashierShiftDao,
                      OrderItemDao orderItemDao, OrderItemQueryDao itemQueryDao,
                      OrderItemModifierDao orderItemModifierDao,
                      DiningTableDao tableDao, OrderDao orderDao,
                      OutboxEventDao outboxEventDao) {
        this.billDao = java.util.Objects.requireNonNull(billDao);
        this.billLineDao = java.util.Objects.requireNonNull(billLineDao);
        this.cashierShiftDao = java.util.Objects.requireNonNull(cashierShiftDao);
        this.orderItemDao = java.util.Objects.requireNonNull(orderItemDao);
        this.itemQueryDao = java.util.Objects.requireNonNull(itemQueryDao);
        this.orderItemModifierDao = java.util.Objects.requireNonNull(orderItemModifierDao);
        this.tableDao = java.util.Objects.requireNonNull(tableDao);
        this.orderDao = java.util.Objects.requireNonNull(orderDao);
        this.outboxEventDao = java.util.Objects.requireNonNull(outboxEventDao);
    }

    /** Tính lại từ BilledAmount snapshot; không đọc giá/modifier hiện hành. */
    void recomputeForBill(Connection conn, int billId) throws SQLException {
        Bill bill = billDao.findById(conn, billId);
        if (bill != null) recomputeWithDiscount(conn, billId, bill.getDiscountAmount());
    }

    void recomputeWithDiscount(Connection conn, int billId, BigDecimal requestedDiscount)
            throws SQLException {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (BillLine item : billLineDao.findByBill(conn, billId)) {
            if (item.getAmount() != null) subtotal = subtotal.add(item.getAmount());
        }
        BigDecimal discount = BillCalculator.clampDiscount(requestedDiscount, subtotal);
        BigDecimal net = subtotal.subtract(discount);
        BigDecimal vat = BillCalculator.computeVat(net);
        BigDecimal total = BillCalculator.computeTotal(subtotal, discount);
        if (billDao.updateAmounts(conn, billId, subtotal, discount, vat, total) != 1) {
            throw new BusinessException("Hoá đơn vừa được chốt bởi thao tác khác. Vui lòng tải lại.");
        }
    }
}
