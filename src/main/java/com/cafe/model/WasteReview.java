package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ngoại lệ hao hụt cần Manager đối soát, không thay thế StockAdjustment. */
public class WasteReview {
    private long wasteReviewId; private long wasteEventId; private int ingredientId;
    private String ingredientName; private String reviewType; private BigDecimal qtyBefore; private BigDecimal qtyAfter;
    private String status; private String note; private LocalDateTime createdAt; private Integer resolvedBy;
    private LocalDateTime resolvedAt; private String resolutionNote;
    public long getWasteReviewId(){return wasteReviewId;} public void setWasteReviewId(long v){wasteReviewId=v;}
    public long getWasteEventId(){return wasteEventId;} public void setWasteEventId(long v){wasteEventId=v;}
    public int getIngredientId(){return ingredientId;} public void setIngredientId(int v){ingredientId=v;}
    public String getIngredientName(){return ingredientName;} public void setIngredientName(String v){ingredientName=v;}
    public String getReviewType(){return reviewType;} public void setReviewType(String v){reviewType=v;}
    public BigDecimal getQtyBefore(){return qtyBefore;} public void setQtyBefore(BigDecimal v){qtyBefore=v;}
    public BigDecimal getQtyAfter(){return qtyAfter;} public void setQtyAfter(BigDecimal v){qtyAfter=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getNote(){return note;} public void setNote(String v){note=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public Integer getResolvedBy(){return resolvedBy;} public void setResolvedBy(Integer v){resolvedBy=v;}
    public LocalDateTime getResolvedAt(){return resolvedAt;} public void setResolvedAt(LocalDateTime v){resolvedAt=v;}
    public String getResolutionNote(){return resolutionNote;} public void setResolutionNote(String v){resolutionNote=v;}
    public boolean isOpen(){return "OPEN".equals(status) || "ACKNOWLEDGED".equals(status);}
}
