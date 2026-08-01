package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** inventory.PrepBatch — mẻ pha sẵn (RAW→PREPPED). Contract #2. */
public class PrepBatch {

    private int prepBatchId;
    private int branchId;
    private int preppedIngredientId;
    private BigDecimal quantityProduced;
    private int madeBy;
    private LocalDateTime madeAt;
    private LocalDateTime expiresAt;
    private String status = "ACTIVE";   // ACTIVE | CANCELLED | PENDING | REJECTED
    private LocalDateTime voidedAt;
    /** Đã ghi hao hụt vì quá hạn — mẻ khép vòng đời, không gợi ý ghi thêm và không huỷ được nữa. */
    private LocalDateTime writtenOffAt;
    private Integer writeOffWasteEventItemId;
    private String clientRequestId;
    /** Vượt 1.5x mức mục tiêu lúc tạo — mẻ vào PENDING, chưa cộng PREPPED cho tới khi Manager duyệt. */
    private boolean requiresApproval;
    private LocalDateTime reviewedAt;
    private Integer reviewedBy;
    private String reviewedByName;   // join

    // join
    private String preppedIngredientName;
    private String preppedIngredientUnit;
    private String madeByName;
    private BigDecimal branchQuantityOnHand = BigDecimal.ZERO;
    private BigDecimal suggestedWasteQuantity = BigDecimal.ZERO;

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

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public boolean isActive() { return "ACTIVE".equals(status); }

    public LocalDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(LocalDateTime v) { this.voidedAt = v; }

    public LocalDateTime getWrittenOffAt() { return writtenOffAt; }
    public void setWrittenOffAt(LocalDateTime v) { this.writtenOffAt = v; }
    public boolean isWrittenOff() { return writtenOffAt != null; }

    public Integer getWriteOffWasteEventItemId() { return writeOffWasteEventItemId; }
    public void setWriteOffWasteEventItemId(Integer v) { this.writeOffWasteEventItemId = v; }
    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String v) { this.clientRequestId = v; }

    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isRejected() { return "REJECTED".equals(status); }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean v) { this.requiresApproval = v; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime v) { this.reviewedAt = v; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer v) { this.reviewedBy = v; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String v) { this.reviewedByName = v; }

    /** Mẻ PENDING mà hạn dùng đã trôi qua trong lúc chờ duyệt — chỉ còn đường Từ chối. */
    public boolean isExpiredWhilePending() {
        return isPending() && expiresAt != null
                && expiresAt.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    public String getPreppedIngredientName() { return preppedIngredientName; }
    public void setPreppedIngredientName(String v) { this.preppedIngredientName = v; }

    public String getPreppedIngredientUnit() { return preppedIngredientUnit; }
    public void setPreppedIngredientUnit(String v) { this.preppedIngredientUnit = v; }

    public String getMadeByName() { return madeByName; }
    public void setMadeByName(String v) { this.madeByName = v; }

    public BigDecimal getBranchQuantityOnHand() { return branchQuantityOnHand; }
    public void setBranchQuantityOnHand(BigDecimal v) { this.branchQuantityOnHand = v == null ? BigDecimal.ZERO : v; }

    public BigDecimal getSuggestedWasteQuantity() { return suggestedWasteQuantity; }
    public void setSuggestedWasteQuantity(BigDecimal v) { this.suggestedWasteQuantity = v == null ? BigDecimal.ZERO : v; }
    public boolean isHasSuggestedWaste() { return suggestedWasteQuantity != null && suggestedWasteQuantity.signum() > 0; }

}
