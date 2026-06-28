package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** inventory.PrepBatch — mẻ pha sẵn (RAW→PREPPED). Contract #2. */
public class PrepBatch {
    private int prepBatchId;
    private int branchId;
    private int preppedIngredientId;
    private BigDecimal quantityProduced;
    private int madeBy;
    private LocalDateTime madeAt;
    private LocalDateTime expiresAt;

    // join
    private String preppedIngredientName;
    private String preppedIngredientUnit;
    private String madeByName;

    public int getPrepBatchId() { return prepBatchId; }
    public void setPrepBatchId(int v) { this.prepBatchId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public int getPreppedIngredientId() { return preppedIngredientId; }
    public void setPreppedIngredientId(int v) { this.preppedIngredientId = v; }

    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(BigDecimal v) { this.quantityProduced = v; }

    public int getMadeBy() { return madeBy; }
    public void setMadeBy(int v) { this.madeBy = v; }

    public LocalDateTime getMadeAt() { return madeAt; }
    public void setMadeAt(LocalDateTime v) { this.madeAt = v; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { this.expiresAt = v; }

    public String getPreppedIngredientName() { return preppedIngredientName; }
    public void setPreppedIngredientName(String v) { this.preppedIngredientName = v; }

    public String getPreppedIngredientUnit() { return preppedIngredientUnit; }
    public void setPreppedIngredientUnit(String v) { this.preppedIngredientUnit = v; }

    public String getMadeByName() { return madeByName; }
    public void setMadeByName(String v) { this.madeByName = v; }
}
