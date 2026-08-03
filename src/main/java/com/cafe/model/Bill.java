package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** payment.Bill — hoá đơn; bàn được suy qua các OrderItem thuộc bill. */
public class Bill {
    private int billId;
    private int branchId;
    private Integer diningTableId;
    private Integer cashierShiftId;
    private BigDecimal subtotal;
    private BigDecimal vatAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal roundingAdjustment;
    private BigDecimal paidAmount;
    private BigDecimal cashTendered;
    private BigDecimal cashChange;
    private String paymentMethod;      // CASH | TRANSFER | QR_BANK
    private String status;             // UNPAID | PAID | VOID
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    // join / computed
    private String tableNumber;
    private List<BillLine> items = new ArrayList<>();

    public int getBillId() { return billId; }
    public void setBillId(int v) { this.billId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public Integer getDiningTableId() { return diningTableId; }
    public void setDiningTableId(Integer v) { this.diningTableId = v; }

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

    public BigDecimal getRoundingAdjustment() { return roundingAdjustment; }
    public void setRoundingAdjustment(BigDecimal v) { this.roundingAdjustment = v; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal v) { this.paidAmount = v; }

    public BigDecimal getCashTendered() { return cashTendered; }
    public void setCashTendered(BigDecimal v) { this.cashTendered = v; }

    public BigDecimal getCashChange() { return cashChange; }
    public void setCashChange(BigDecimal v) { this.cashChange = v; }

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

    public List<BillLine> getItems() { return items; }
    public void setItems(List<BillLine> v) { this.items = v; }

    /** Chỉ được thu tiền sau khi mọi dòng trên bill đã hoàn tất bước bàn giao khách. */
    public boolean isReadyForPayment() {
        if (items == null || items.isEmpty()) return false;
        for (BillLine item : items) {
            if (!"SERVED".equals(item.getStatus())) return false;
        }
        return true;
    }
}
