package com.mycoffee.service;

import com.mycoffee.dao.OrderDAO;
import com.mycoffee.dao.VoucherDAO;
import com.mycoffee.model.Order;
import com.mycoffee.model.Voucher;

public class OrderService {
    private final OrderDAO orderDAO = new OrderDAO();
    private final VoucherDAO voucherDAO = new VoucherDAO();
    private final PayOSService payOSService = new PayOSService();

    public void addOrUpdateItem(int orderId, int productId, int quantity, double price) {
        orderDAO.addOrUpdateOrderDetail(orderId, productId, quantity, price);
    }

    public void applyVoucher(int orderId, String voucherCode) {
        Voucher v = voucherDAO.getVoucherByCode(voucherCode);
        if (v == null) return;

        Order order = orderDAO.getOrderById(orderId);
        if (order == null) return;

        double totalAmount = order.getTotalAmount();
        double discountAmount = v.isIsPercentage() ? totalAmount * (v.getDiscountValue() / 100.0) : v.getDiscountValue();

        // Cắt ngọn nếu giảm % vượt qua MaxDiscount
        if (v.isIsPercentage() && v.getMaxDiscount() != null && v.getMaxDiscount() > 0) {
            if (discountAmount > v.getMaxDiscount()) {
                discountAmount = v.getMaxDiscount();
            }
        }

        // Chống âm tiền
        if (discountAmount > totalAmount) {
            discountAmount = totalAmount;
        }

        orderDAO.updateOrderDiscount(orderId, discountAmount);
    }

    public String generatePaymentLink(int orderId, String baseUrl) {
        Order order = orderDAO.getOrderById(orderId);
        if (order == null || order.getFinalAmount() <= 0) return null;

        String returnUrl = baseUrl + "/pos?action=payos_return&orderId=" + orderId;
        String cancelUrl = baseUrl + "/pos?action=view&orderId=" + orderId;

        return payOSService.createPaymentLink(
                order.getOrderId(),
                (int) order.getFinalAmount(),
                "Thanh toan Don hang " + order.getOrderId(),
                returnUrl,
                cancelUrl
        );
    }

    public void completeOrder(int orderId) {
        orderDAO.completeOrder(orderId);
    }

    public int openOrCreateTableOrder(int branchId, int tableId, int cashierId) {
        int existingOrderId = orderDAO.getPendingOrderIdByTable(tableId);
        if (existingOrderId > 0) return existingOrderId;
        return orderDAO.createNewOrder(branchId, tableId, cashierId);
    }

    public void cancelTable(int orderId, int tableId) {
        orderDAO.cancelOrder(orderId, tableId);
    }

    public void resetBranchTables(int branchId) {
        orderDAO.resetAllTables(branchId);
    }
}