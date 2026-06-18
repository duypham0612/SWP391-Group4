package com.mycoffee.model;

import java.sql.Timestamp;

public class Order {
    private int orderId;
    private int branchId;
    private int tableId;
    private int cashierId;
    private String orderType;
    private double totalAmount;
    private double discountAmount;
    private double finalAmount;
    private String orderStatus;
    private Timestamp orderDate;

    // Constructor rỗng (Bắt buộc phải có để map dữ liệu từ ResultSet)
    public Order() {
    }

    // Constructor đầy đủ tham số
    public Order(int orderId, int branchId, int tableId, int cashierId, String orderType, double totalAmount, double discountAmount, double finalAmount, String orderStatus, Timestamp orderDate) {
        this.orderId = orderId;
        this.branchId = branchId;
        this.tableId = tableId;
        this.cashierId = cashierId;
        this.orderType = orderType;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.orderStatus = orderStatus;
        this.orderDate = orderDate;
    }

    // ================= GETTER & SETTER =================

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public int getCashierId() {
        return cashierId;
    }

    public void setCashierId(int cashierId) {
        this.cashierId = cashierId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }
}