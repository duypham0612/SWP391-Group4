package com.cafe.model;

import java.math.BigDecimal;

/** payment.BillItem — dòng đơn nào nằm trên bill nào (1 OrderItem ⊂ đúng 1 Bill). */
public class BillItem {
    private int billItemId;
    private int billId;
    private int orderItemId;
    private BigDecimal amount;

    // join hiển thị
    private String productName;
    private int quantity;
    private String status;             // trạng thái OrderItem

    public int getBillItemId() { return billItemId; }
    public void setBillItemId(int v) { this.billItemId = v; }

    public int getBillId() { return billId; }
    public void setBillId(int v) { this.billId = v; }

    public int getOrderItemId() { return orderItemId; }
    public void setOrderItemId(int v) { this.orderItemId = v; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
