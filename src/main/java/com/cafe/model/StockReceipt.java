package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

/** Read model của phiếu nhập, được GROUP BY inventory.StockReceiptLine.ReceiptBatchId. */
public class StockReceipt {
    private String receiptBatchId;
    private int branchId;
    private Integer supplierId;
    private int receivedBy;
    private LocalDate documentDate;
    private LocalDateTime createdAt;
    private String status;          // DRAFT | CONFIRMED | CANCELLED
    private BigDecimal totalCost = BigDecimal.ZERO;
    private String note;

    // join
    private String supplierName;
    private String receivedByName;

    public String getReceiptBatchId() { return receiptBatchId; }
    public void setReceiptBatchId(String receiptBatchId) { this.receiptBatchId = receiptBatchId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public int getReceivedBy() { return receivedBy; }
    public void setReceivedBy(int receivedBy) { this.receivedBy = receivedBy; }

    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

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
