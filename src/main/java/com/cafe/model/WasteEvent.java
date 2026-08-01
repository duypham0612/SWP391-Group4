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

    public String getEventGroupId() { return eventGroupId; } public void setEventGroupId(String v) { eventGroupId=v; }
    public int getBranchId() { return branchId; } public void setBranchId(int v) { branchId=v; }
    public String getEventKind() { return eventKind; } public void setEventKind(String v) { eventKind=v; }
    public String getSource() { return source; } public void setSource(String v) { source=v; }
    public Integer getProductId() { return productId; } public void setProductId(Integer v) { productId=v; }
    public Integer getOrderItemId() { return orderItemId; } public void setOrderItemId(Integer v) { orderItemId=v; }
    public Integer getCupQuantity() { return cupQuantity; } public void setCupQuantity(Integer v) { cupQuantity=v; }
    public String getCauseCode() { return causeCode; } public void setCauseCode(String v) { causeCode=v; }
    public String getCauseDetail() { return causeDetail; } public void setCauseDetail(String v) { causeDetail=v; }
    public Integer getShiftAssignmentId() { return shiftAssignmentId; } public void setShiftAssignmentId(Integer v) { shiftAssignmentId=v; }
    public int getCreatedBy() { return createdBy; } public void setCreatedBy(int v) { createdBy=v; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt=v; }
    public String getProductName() { return productName; } public void setProductName(String v) { productName=v; }
    public boolean isRemake() { return "REMAKE".equals(eventKind); }
}
