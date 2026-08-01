package com.cafe.model;

import java.time.LocalDateTime;

/** inventory.WasteEventAudit — một thao tác trên dòng hao hụt, để Quản lý truy vết ai sửa/huỷ cái gì. */
public class WasteEventAudit {
    private long wasteEventAuditId;
    private Integer wasteEventItemId;
    private Long wasteEventId;
    private String actionType;      // CREATE | UPDATE | VOID | REVIEW
    private String beforeValue;
    private String afterValue;
    private String reason;
    private int performedBy;
    private LocalDateTime performedAt;

    // join
    private String performedByName;
    private String ingredientName;
    private String ingredientUnit;
    private String wasteType;

    public long getWasteEventAuditId() { return wasteEventAuditId; }
    public void setWasteEventAuditId(long v) { wasteEventAuditId = v; }

    public Integer getWasteEventItemId() { return wasteEventItemId; }
    public void setWasteEventItemId(Integer v) { wasteEventItemId = v; }

    public Long getWasteEventId() { return wasteEventId; }
    public void setWasteEventId(Long v) { wasteEventId = v; }

    public String getActionType() { return actionType; }
    public void setActionType(String v) { actionType = v; }

    public String getBeforeValue() { return beforeValue; }
    public void setBeforeValue(String v) { beforeValue = v; }

    public String getAfterValue() { return afterValue; }
    public void setAfterValue(String v) { afterValue = v; }

    public String getReason() { return reason; }
    public void setReason(String v) { reason = v; }

    public int getPerformedBy() { return performedBy; }
    public void setPerformedBy(int v) { performedBy = v; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime v) { performedAt = v; }

    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String v) { performedByName = v; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String v) { ingredientName = v; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String v) { ingredientUnit = v; }

    public String getWasteType() { return wasteType; }
    public void setWasteType(String v) { wasteType = v; }

    /** Huỷ dòng là thao tác hoàn kho — Quản lý cần nhìn thấy nổi bật hơn sửa số lượng. */
    public boolean isVoidAction() { return "VOID".equals(actionType); }

}
