package com.cafe.model;

import java.time.LocalDateTime;

/** Append-only audit event for barista operational actions. */
public class BaristaActionLog {
    private long actionLogId;
    private int branchId;
    private Integer shiftId;
    private String entityType;
    private Long entityId;
    private String actionType;
    private String beforeJson;
    private String afterJson;
    private String reason;
    private Integer performedBy;
    private String performedByName;
    private String correlationId;
    private LocalDateTime createdAt;

    public long getActionLogId() { return actionLogId; }
    public void setActionLogId(long v) { actionLogId = v; }
    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { branchId = v; }
    public Integer getShiftId() { return shiftId; }
    public void setShiftId(Integer v) { shiftId = v; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String v) { entityType = v; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long v) { entityId = v; }
    public String getActionType() { return actionType; }
    public void setActionType(String v) { actionType = v; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String v) { beforeJson = v; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String v) { afterJson = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { reason = v; }
    public Integer getPerformedBy() { return performedBy; }
    public void setPerformedBy(Integer v) { performedBy = v; }
    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String v) { performedByName = v; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String v) { correlationId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }

    public String getActionLabel() {
        if (actionType == null) return "Không rõ";
        return switch (actionType) {
            case "PREP_CREATED" -> "Tạo mẻ";
            case "PREP_QUANTITY_CHANGED" -> "Sửa sản lượng";
            case "PREP_CANCELLED" -> "Huỷ mẻ";
            case "WASTE_CREATED" -> "Ghi hao hụt";
            case "WASTE_CHANGED" -> "Sửa hao hụt";
            case "WASTE_VOIDED" -> "Huỷ hao hụt";
            case "REMAKE_MANUAL_CREATED" -> "Làm lại ngoài đơn";
            case "MENU_86_REPORTED" -> "Báo tạm hết";
            case "MENU_86_APPROVED" -> "Xác nhận tạm hết";
            case "MENU_86_REOPEN_REQUESTED" -> "Xin mở bán lại";
            case "MENU_86_REOPENED" -> "Mở bán lại";
            case "MENU_86_REJECTED" -> "Từ chối báo hết";
            default -> actionType;
        };
    }

    public String getEntityLabel() {
        if (entityType == null) return "Không rõ";
        return switch (entityType) {
            case "PREP_BATCH" -> "Mẻ pha";
            case "WASTE_LOG" -> "Hao hụt";
            case "MENU_86" -> "Yêu cầu tạm hết";
            case "MANUAL_REMAKE" -> "Làm lại ngoài đơn";
            default -> entityType;
        };
    }
}
