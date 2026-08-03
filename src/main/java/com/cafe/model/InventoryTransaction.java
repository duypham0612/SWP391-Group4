package com.cafe.model;

import com.cafe.common.InventoryReferenceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** inventory.InventoryTransaction — sổ cái tồn kho (1 dòng = 1 thay đổi). */
public class InventoryTransaction {
    private long inventoryTransactionId;
    private int branchId;
    private int ingredientId;
    private BigDecimal changeQty;
    private String txnType;
    private InventoryReferenceType referenceType;
    private Long referenceId;
    private Integer createdBy;
    private LocalDateTime createdAt;

    // join
    private String ingredientName;
    private String ingredientUnit;
    private String createdByName;

    public long getInventoryTransactionId() { return inventoryTransactionId; }
    public void setInventoryTransactionId(long inventoryTransactionId) { this.inventoryTransactionId = inventoryTransactionId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getChangeQty() { return changeQty; }
    public void setChangeQty(BigDecimal changeQty) { this.changeQty = changeQty; }
    public String getTxnType() { return txnType; }
    public void setTxnType(String txnType) { this.txnType = txnType; }

    public InventoryReferenceType getReferenceType() { return referenceType; }
    public void setReferenceType(InventoryReferenceType value) { referenceType = value; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long value) { referenceId = value; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String ingredientUnit) { this.ingredientUnit = ingredientUnit; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
}
