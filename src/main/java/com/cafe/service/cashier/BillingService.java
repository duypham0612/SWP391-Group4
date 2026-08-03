package com.cafe.service.cashier;

import com.cafe.model.Bill;
import com.cafe.model.Order;
import com.cafe.model.DiningTable;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Facade tương thích cho caller cũ; toàn bộ use case nằm ở các service chuyên trách. */
public final class BillingService {
    private final BillCreationService creationService;
    private final PaymentService paymentService;
    private final BillVoidService voidService;
    private final BillingQueryService queryService;

    public BillingService() {
        BillingRepository repository = new BillingRepository();
        BillingQueryService query = new BillingQueryService(repository);
        this.creationService = new BillCreationService(repository, query);
        this.paymentService = new PaymentService(repository);
        this.voidService = new BillVoidService(repository);
        this.queryService = query;
    }

    public BillingService(BillCreationService creationService, PaymentService paymentService,
                          BillVoidService voidService, BillingQueryService queryService) {
        this.creationService = Objects.requireNonNull(creationService);
        this.paymentService = Objects.requireNonNull(paymentService);
        this.voidService = Objects.requireNonNull(voidService);
        this.queryService = Objects.requireNonNull(queryService);
    }

    public List<Bill> buildTableBill(int tableId, int branchId, Integer shiftId) throws SQLException { return creationService.buildTableBill(tableId, branchId, shiftId); }
    public List<Bill> buildTakeawayBill(int orderId, int branchId, Integer shiftId) throws SQLException { return creationService.buildTakeawayBill(orderId, branchId, shiftId); }
    public void splitItems(int tableId, int branchId, Integer shiftId, List<Integer> ids) throws SQLException { creationService.splitItems(tableId, branchId, shiftId, ids); }
    public void mergeBills(List<Integer> ids, int branchId) throws SQLException { creationService.mergeBills(ids, branchId); }
    public void setDiscount(int billId, BigDecimal discount, int branchId) throws SQLException { creationService.setDiscount(billId, discount, branchId); }

    public PaymentResult payBill(int billId, String method, Integer shiftId, BigDecimal cashTendered) throws SQLException {
        PaymentService.PaymentResult result = paymentService.payBill(billId, method, shiftId, cashTendered);
        return new PaymentResult(result.paid(), result.paidAmount(), result.cashTendered(),
                result.cashChange(), result.roundingAdjustment());
    }
    public static boolean isSupportedPaymentMethod(String method) { return PaymentService.isSupportedPaymentMethod(method); }
    public boolean voidBill(int billId, int branchId, String reason, Integer userId) throws SQLException { return voidService.voidBill(billId, branchId, reason, userId); }
    public Bill getBill(int billId) throws SQLException { return queryService.getBill(billId); }
    public List<Bill> getTableBills(int tableId) throws SQLException { return queryService.getTableBills(tableId); }
    public List<Bill> getOrderBills(int orderId) throws SQLException { return queryService.getOrderBills(orderId); }
    public String validatePayable(int billId, int branchId) throws SQLException { return queryService.validatePayable(billId, branchId); }
    public DiningTable getOpenTableForCheckout(int tableId, int branchId) throws SQLException { return queryService.getOpenTableForCheckout(tableId, branchId); }
    public List<Order> getTakeawayOrdersAwaitingPayment(int branchId) throws SQLException { return queryService.getTakeawayOrdersAwaitingPayment(branchId); }
    public List<Bill> getBillHistory(int branchId) throws SQLException { return queryService.getBillHistory(branchId); }
    public List<Bill> getBillHistoryByShift(int shiftId) throws SQLException { return queryService.getBillHistoryByShift(shiftId); }

    public record PaymentResult(boolean paid, BigDecimal paidAmount, BigDecimal cashTendered,
                                BigDecimal cashChange, BigDecimal roundingAdjustment) { }
}
