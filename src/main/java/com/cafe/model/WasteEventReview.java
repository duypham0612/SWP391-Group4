package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ngoại lệ hao hụt cần Manager đối soát, không thay thế StockAdjustment. */
public class WasteEventReview {
    private long wasteEntryId; private String eventGroupId; private int ingredientId;
    private String ingredientName; private String reviewType; private BigDecimal qtyBefore; private BigDecimal qtyAfter;
    private String status; private String note; private LocalDateTime createdAt; private Integer resolvedBy;
    public long getWasteEntryId() { return wasteEntryId; }
    public void setWasteEntryId(long wasteEntryId) { this.wasteEntryId = wasteEntryId; }
    public String getEventGroupId() { return eventGroupId; }
    public void setEventGroupId(String eventGroupId) { this.eventGroupId = eventGroupId; }
    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int ingredientId) { this.ingredientId = ingredientId; }
    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }
    public BigDecimal getQtyBefore() { return qtyBefore; }
    public void setQtyBefore(BigDecimal qtyBefore) { this.qtyBefore = qtyBefore; }
    public BigDecimal getQtyAfter() { return qtyAfter; }
    public void setQtyAfter(BigDecimal qtyAfter) { this.qtyAfter = qtyAfter; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isOpen(){return "OPEN".equals(status);}
    /** Tồn âm vượt ngưỡng cần xử lý trước — dùng để tô đậm dòng trên màn đối soát. */
    public boolean isUrgent(){return "HARD_NEGATIVE".equals(reviewType);}
}
