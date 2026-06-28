package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** inventory.InventoryTransaction — sổ cái tồn kho (1 dòng = 1 thay đổi). */
public class InventoryTransaction {
    private long inventoryTxnId;
    private int branchId;
    private int ingredientId;
    private BigDecimal changeQty;
    private String txnType;
    private String refTable;
    private Long refId;
    private Integer createdBy;
    private LocalDateTime createdAt;

    // join
    private String ingredientName;
    private String ingredientUnit;
    private String createdByName;

    public long getInventoryTxnId() { return inventoryTxnId; }
    public void setInventoryTxnId(long inventoryTxnId) { this.inventoryTxnId = inventoryTxnId; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getChangeQty() { return changeQty; }
    public void setChangeQty(BigDecimal changeQty) { this.changeQty = changeQty; }

    public String getTxnType() { return txnType; }
    public void setTxnType(String txnType) { this.txnType = txnType; }

    public String getRefTable() { return refTable; }
    public void setRefTable(String refTable) { this.refTable = refTable; }

    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }

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
