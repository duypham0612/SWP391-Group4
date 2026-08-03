package com.cafe.model;

import java.time.LocalDateTime;

/** Metadata sự kiện được lặp trên các dòng cùng WasteEntry.EventGroupId. */
public class WasteEvent {
    private String eventGroupId;
    private int branchId;
    private String eventKind;
    private String source;
    private Integer productId;
    private Integer orderItemId;
    private Integer cupQuantity;
    private String causeCode;
    private String causeDetail;
    private Integer shiftAssignmentId;
    private int createdBy;
    private LocalDateTime createdAt;
    private String productName;

    public String getEventGroupId() { return eventGroupId; }
    public void setEventGroupId(String eventGroupId) { this.eventGroupId = eventGroupId; }
    public int getBranchId() { return branchId; }
    public void setBranchId(int branchId) { this.branchId = branchId; }
    public String getEventKind() { return eventKind; }
    public void setEventKind(String eventKind) { this.eventKind = eventKind; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }
    public Integer getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Integer orderItemId) { this.orderItemId = orderItemId; }
    public Integer getCupQuantity() { return cupQuantity; }
    public void setCupQuantity(Integer cupQuantity) { this.cupQuantity = cupQuantity; }
    public String getCauseCode() { return causeCode; }
    public void setCauseCode(String causeCode) { this.causeCode = causeCode; }
    public String getCauseDetail() { return causeDetail; }
    public void setCauseDetail(String causeDetail) { this.causeDetail = causeDetail; }
    public Integer getShiftAssignmentId() { return shiftAssignmentId; }
    public void setShiftAssignmentId(Integer shiftAssignmentId) { this.shiftAssignmentId = shiftAssignmentId; }
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public boolean isRemake() { return "REMAKE".equals(eventKind); }
}
