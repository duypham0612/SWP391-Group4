package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** payment.Bill — hoá đơn (1 phiên bàn có thể tách nhiều bill). */
public class Bill {
    private int billId;
    private int branchId;
    private Integer tableSessionId;
    private Integer cashierShiftId;
    private BigDecimal subtotal;
    private BigDecimal vatAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Integer voucherId;
    private String paymentMethod;      // CASH | TRANSFER | QR_BANK
    private String status;             // UNPAID | PAID | VOID
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    // join / computed
    private String tableNumber;
    private String voucherCode;
    private List<BillItem> items = new ArrayList<>();

    public int getBillId() { return billId; }
    public void setBillId(int v) { this.billId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public Integer getTableSessionId() { return tableSessionId; }
    public void setTableSessionId(Integer v) { this.tableSessionId = v; }

    public Integer getCashierShiftId() { return cashierShiftId; }
    public void setCashierShiftId(Integer v) { this.cashierShiftId = v; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { this.subtotal = v; }

    public BigDecimal getVatAmount() { return vatAmount; }
    public void setVatAmount(BigDecimal v) { this.vatAmount = v; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal v) { this.discountAmount = v; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }

    public Integer getVoucherId() { return voucherId; }
    public void setVoucherId(Integer v) { this.voucherId = v; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String v) { this.paymentMethod = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime v) { this.paidAt = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String v) { this.tableNumber = v; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String v) { this.voucherCode = v; }

    public List<BillItem> getItems() { return items; }
    public void setItems(List<BillItem> v) { this.items = v; }
}
