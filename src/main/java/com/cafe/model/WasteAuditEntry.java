package com.cafe.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** inventory.WasteAuditLog — một thao tác trên dòng hao hụt, để Quản lý truy vết ai sửa/huỷ cái gì. */
public class WasteAuditEntry {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private long wasteAuditLogId;
    private Integer wasteLogId;
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

    public long getWasteAuditLogId() { return wasteAuditLogId; }
    public void setWasteAuditLogId(long v) { wasteAuditLogId = v; }

    public Integer getWasteLogId() { return wasteLogId; }
    public void setWasteLogId(Integer v) { wasteLogId = v; }

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

    public String getPerformedAtDisplay() {
        if (performedAt == null) return "";
        return performedAt.atZone(ZoneOffset.UTC).withZoneSameInstant(VN_ZONE).format(DATE_TIME_FMT);
    }

    public String getActionLabel() {
        if ("CREATE".equals(actionType)) return "Ghi mới";
        if ("UPDATE".equals(actionType)) return "Sửa số lượng";
        if ("VOID".equals(actionType)) return "Huỷ dòng";
        if ("REVIEW".equals(actionType)) return "Đối soát";
        return actionType;
    }

    /** Huỷ dòng là thao tác hoàn kho — Quản lý cần nhìn thấy nổi bật hơn sửa số lượng. */
    public boolean isVoidAction() { return "VOID".equals(actionType); }

    /** "12 → 8" cho UPDATE, "12" cho VOID (không còn giá trị sau). */
    public String getChangeDisplay() {
        String from = beforeValue == null || beforeValue.isBlank() ? "" : plain(beforeValue);
        String to = afterValue == null || afterValue.isBlank() ? "" : plain(afterValue);
        if (!from.isEmpty() && !to.isEmpty()) return from + " → " + to;
        if (!to.isEmpty()) return to;
        return from;
    }

    /** BeforeValue/AfterValue lưu dạng chuỗi số thô nên bỏ phần .000 thừa cho dễ đọc. */
    private static String plain(String raw) {
        try {
            return com.cafe.common.QuantityFormat.plain(new java.math.BigDecimal(raw.trim()));
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}
