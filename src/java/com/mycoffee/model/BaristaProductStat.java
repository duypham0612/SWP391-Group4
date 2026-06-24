package com.mycoffee.model;

/**
 * DTO thống kê pha chế theo từng món (dùng cho Dashboard & Báo cáo).
 */
public class BaristaProductStat {
    private int productId;
    private String productName;
    private int totalQty;        // tổng số ly đã pha
    private int orderCount;      // số lần xuất hiện trong order
    private int avgPrepSeconds;  // thời gian pha trung bình (giây)

    public BaristaProductStat() {
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(int totalQty) {
        this.totalQty = totalQty;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public int getAvgPrepSeconds() {
        return avgPrepSeconds;
    }

    public void setAvgPrepSeconds(int avgPrepSeconds) {
        this.avgPrepSeconds = avgPrepSeconds;
    }

    /** Thời gian pha TB định dạng "Xp Ys" - cho JSP dùng EL. */
    public String getAvgPrepLabel() {
        return (avgPrepSeconds / 60) + "p " + (avgPrepSeconds % 60) + "s";
    }
}
