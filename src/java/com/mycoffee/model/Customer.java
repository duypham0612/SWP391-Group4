package com.mycoffee.model;

/**
 * Model ánh xạ bảng Customers trong CSDL.
 * CustomerID kế thừa từ UserID bên bảng Users (quan hệ 1-1).
 * Bản ghi này được tạo TỰ ĐỘNG khi khách đăng ký tài khoản.
 */
public class Customer {

    private int customerId;      // Trùng với UserID bên bảng Users
    private String memberRank;   // Member, Silver, Gold, Platinum...
    private int currentPoints;   // Điểm tích lũy hiện tại

    public Customer() {
    }

    public Customer(int customerId, String memberRank, int currentPoints) {
        this.customerId    = customerId;
        this.memberRank    = memberRank;
        this.currentPoints = currentPoints;
    }

    // ── Getters & Setters ──────────────────────────────────────────

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getMemberRank() {
        return memberRank;
    }

    public void setMemberRank(String memberRank) {
        this.memberRank = memberRank;
    }

    public int getCurrentPoints() {
        return currentPoints;
    }

    public void setCurrentPoints(int currentPoints) {
        this.currentPoints = currentPoints;
    }

    @Override
    public String toString() {
        return "Customer{customerId=" + customerId
             + ", memberRank='" + memberRank + '\''
             + ", currentPoints=" + currentPoints + '}';
    }
}
