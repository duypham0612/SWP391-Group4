package com.mycoffee.model;

public class Dashboard {

    private double totalRevenue; // Tổng doanh thu
    private int totalOrders;    // Tổng số đơn hàng
    private int lowStockCount;   // Số nguyên liệu sắp hết kho
    private int activeEmployees; // Số nhân viên đang làm việc

    public Dashboard() {
    }

    public Dashboard(double totalRevenue, int totalOrders, int lowStockCount, int activeEmployees) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.lowStockCount = lowStockCount;
        this.activeEmployees = activeEmployees;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public int getActiveEmployees() {
        return activeEmployees;
    }

    public void setActiveEmployees(int activeEmployees) {
        this.activeEmployees = activeEmployees;
    }
}
