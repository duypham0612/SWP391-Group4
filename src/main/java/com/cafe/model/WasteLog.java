package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** inventory.WasteLog — ghi hao hụt/làm lại; mỗi dòng kèm 1 txn WASTE ở ledger. */
public class WasteLog {
    private int wasteLogId;
    private int branchId;
    private int ingredientId;
    private BigDecimal quantity;
    private String wasteType;          // SPILL | EXPIRED | REMAKE | OTHER
    private String reason;
    private int loggedBy;
    private LocalDateTime loggedAt;

    // join
    private String ingredientName;
    private String ingredientUnit;
    private String loggedByName;

    public int getWasteLogId() { return wasteLogId; }
    public void setWasteLogId(int v) { this.wasteLogId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int v) { this.ingredientId = v; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }

    public String getWasteType() { return wasteType; }
    public void setWasteType(String v) { this.wasteType = v; }

    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }

    public int getLoggedBy() { return loggedBy; }
    public void setLoggedBy(int v) { this.loggedBy = v; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime v) { this.loggedAt = v; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String v) { this.ingredientName = v; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String v) { this.ingredientUnit = v; }

    public String getLoggedByName() { return loggedByName; }
    public void setLoggedByName(String v) { this.loggedByName = v; }
}
