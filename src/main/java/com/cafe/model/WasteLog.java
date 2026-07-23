package com.cafe.model;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** inventory.WasteLog — ghi hao hụt/làm lại; mỗi dòng kèm 1 txn WASTE ở ledger. */
public class WasteLog {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private static final Locale VI_LOCALE = Locale.forLanguageTag("vi-VN");

    private int wasteLogId;
    private int branchId;
    private int ingredientId;
    private BigDecimal quantity;
    private String wasteType;          // SPILL | EXPIRED | REMAKE | OTHER
    private String reason;
    private int loggedBy;
    private LocalDateTime loggedAt;
    private String status = "ACTIVE";   // ACTIVE | VOIDED
    private LocalDateTime voidedAt;

    // join
    private String ingredientName;
    private String ingredientUnit;
    private String ingredientType;
    private String loggedByName;
    private BigDecimal unitCost;
    private Long wasteEventId;
    private BigDecimal unitCostAtLog;
    private String costBasis = "LEGACY_ESTIMATE";
    private WasteEvent wasteEvent;

    public int getWasteLogId() { return wasteLogId; }
    public void setWasteLogId(int v) { this.wasteLogId = v; }

    public int getBranchId() { return branchId; }
    public void setBranchId(int v) { this.branchId = v; }

    public int getIngredientId() { return ingredientId; }
    public void setIngredientId(int v) { this.ingredientId = v; }

    /** Cho JSP — bỏ .000 thừa. */
    public String getQuantityDisplay() { return com.cafe.common.QuantityFormat.plain(quantity); }

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

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public boolean isActive() { return "ACTIVE".equals(status); }

    public LocalDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(LocalDateTime v) { this.voidedAt = v; }

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String v) { this.ingredientName = v; }

    public String getIngredientUnit() { return ingredientUnit; }
    public void setIngredientUnit(String v) { this.ingredientUnit = v; }

    public String getIngredientType() { return ingredientType; }
    public void setIngredientType(String v) { this.ingredientType = v; }

    public String getLoggedByName() { return loggedByName; }
    public void setLoggedByName(String v) { this.loggedByName = v; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public Long getWasteEventId() { return wasteEventId; }
    public void setWasteEventId(Long v) { wasteEventId = v; }
    public BigDecimal getUnitCostAtLog() { return unitCostAtLog; }
    public void setUnitCostAtLog(BigDecimal v) { unitCostAtLog = v; }
    public String getCostBasis() { return costBasis; }
    public void setCostBasis(String v) { costBasis = v == null ? "LEGACY_ESTIMATE" : v; }
    public WasteEvent getWasteEvent() { return wasteEvent; }
    public void setWasteEvent(WasteEvent v) { wasteEvent = v; }

    public boolean isCostAvailable() { return effectiveUnitCost() != null && quantity != null; }

    public BigDecimal getLineCost() {
        if (!isCostAvailable()) return null;
        return quantity.multiply(effectiveUnitCost());
    }

    public String getCostDisplay() {
        BigDecimal cost = getLineCost();
        if (cost == null) return "Chưa có giá";
        NumberFormat fmt = NumberFormat.getNumberInstance(VI_LOCALE);
        return fmt.format(cost.setScale(0, RoundingMode.HALF_UP)) + " đ";
    }

    public String getCostBasisLabel() {
        if ("SNAPSHOT".equals(costBasis)) return "Đã chốt";
        if ("UNAVAILABLE".equals(costBasis)) return "Chưa có giá";
        return "Ước tính dữ liệu cũ";
    }
    private BigDecimal effectiveUnitCost() { return "SNAPSHOT".equals(costBasis) ? unitCostAtLog : unitCost; }

    public String getLoggedAtDisplay() {
        if (loggedAt == null) return "";
        return loggedAt.atZone(ZoneOffset.UTC).withZoneSameInstant(VN_ZONE).format(DATE_TIME_FMT);
    }

    public String getWasteTypeLabel() {
        if ("SPILL".equals(wasteType)) return "Hao đổ/rơi";
        if ("EXPIRED".equals(wasteType)) return "Hết hạn";
        if ("REMAKE".equals(wasteType)) return "Làm lại món";
        return "Khác";
    }

    public boolean isRemake() { return "REMAKE".equals(wasteType); }
    public boolean isCorrectionWindowOpen() {
        return loggedAt != null && !loggedAt.isBefore(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(15));
    }
    public boolean isEditable() { return isActive() && !isRemake() && isCorrectionWindowOpen(); }
    public boolean isVoidable() { return isActive() && !isRemake() && isCorrectionWindowOpen(); }
}
