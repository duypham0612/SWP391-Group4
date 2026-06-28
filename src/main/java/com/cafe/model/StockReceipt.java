package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** inventory.StockReceipt — phiếu nhập kho. */
public class StockReceipt {
    private int stockReceiptId;
    private int branchId;
    private Integer supplierId;
    private int receivedBy;
    private LocalDateTime receiptDate;
    private String status;          // DRAFT | CONFIRMED | CANCELLED
    private BigDecimal totalCost = BigDecimal.ZERO;
    private String note;

    // join
    private String supplierName;
    private String receivedByName;

    public int getStockReceiptId() { return stockReceiptId; }
    public void setStockReceiptId(int stockReceiptId) { this.stockReceiptId = stockReceiptId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public int getReceivedBy() { return receivedBy; }
    public void setReceivedBy(int receivedBy) { this.receivedBy = receivedBy; }

    public LocalDateTime getReceiptDate() { return receiptDate; }
    public void setReceiptDate(LocalDateTime receiptDate) { this.receiptDate = receiptDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getReceivedByName() { return receivedByName; }
    public void setReceivedByName(String receivedByName) { this.receivedByName = receivedByName; }
}
