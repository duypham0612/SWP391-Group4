package com.mycoffee.model;

import java.sql.Timestamp;

/**
 * DTO đại diện cho 1 dòng món trong hàng chờ pha chế của Barista.
 * Gộp dữ liệu từ OrderDetails + Orders + Products + Tables để hiển thị.
 */
public class BaristaQueueItem {
    private int orderDetailId;
    private int orderId;
    private int productId;
    private String tableName;
    private String productName;
    private int quantity;
    private String note;
    private String itemStatus;
    private int priority;
    private Timestamp orderDate;
    private Timestamp startedAt;
    private Timestamp completedAt;

    public BaristaQueueItem() {
    }

    // ================= GETTER & SETTER =================

    public int getOrderDetailId() {
        return orderDetailId;
    }

    public void setOrderDetailId(int orderDetailId) {
        this.orderDetailId = orderDetailId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Timestamp getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Timestamp orderDate) {
        this.orderDate = orderDate;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    /** Thời gian pha (CompletedAt - StartedAt) định dạng "Xp Ys" - cho JSP dùng EL. */
    public String getPrepDurationLabel() {
        if (startedAt == null || completedAt == null) return "-";
        long sec = (completedAt.getTime() - startedAt.getTime()) / 1000;
        if (sec < 0) sec = 0;
        return (sec / 60) + "p " + (sec % 60) + "s";
    }
}
