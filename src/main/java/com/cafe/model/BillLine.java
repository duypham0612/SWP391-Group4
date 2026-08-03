package com.cafe.model;

import java.math.BigDecimal;

/** Projection của một sales.OrderItem đã được gắn vào payment.Bill. */
public class BillLine {
    private int billId;
    private int branchId;
    private int orderItemId;
    private BigDecimal amount;
    private String productName;
    private int quantity;
    private String status;
    private String selections;
    private String note;

    public int getBillId() { return billId; }
    public void setBillId(int value) { this.billId = value; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int value) { this.branchId = value; }

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int value) { this.orderItemId = value; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }

    public String getProductName() { return productName; }
    public void setProductName(String value) { this.productName = value; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { this.quantity = value; }

    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getSelections() { return selections; }
    public void setSelections(String value) { this.selections = value; }
    public String getNote() { return note; }
    public void setNote(String value) { this.note = value; }
}
