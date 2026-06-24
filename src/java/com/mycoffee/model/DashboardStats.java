package com.mycoffee.model;

import java.util.List;

public class DashboardStats {
    private double totalRevenue;
    private int totalOrders;
    private int lowStockCount;
    private List<OrderSummary> recentOrders;

    public DashboardStats() {}

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public int getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(int lowStockCount) { this.lowStockCount = lowStockCount; }

    public List<OrderSummary> getRecentOrders() { return recentOrders; }
    public void setRecentOrders(List<OrderSummary> recentOrders) { this.recentOrders = recentOrders; }
    
    // Inner class helper để giữ thông tin đơn hàng rút gọn
    public static class OrderSummary {
        private int orderId;
        private String timeOrder;
        private String tableName;
        private double finalAmount;
        private String orderStatus;

        public int getOrderId() { return orderId; }
        public void setOrderId(int orderId) { this.orderId = orderId; }

        public String getTimeOrder() { return timeOrder; }
        public void setTimeOrder(String timeOrder) { this.timeOrder = timeOrder; }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }

        public double getFinalAmount() { return finalAmount; }
        public void setFinalAmount(double finalAmount) { this.finalAmount = finalAmount; }

        public String getOrderStatus() { return orderStatus; }
        public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    }
}
