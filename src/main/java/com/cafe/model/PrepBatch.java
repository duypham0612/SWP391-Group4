package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** inventory.PrepBatch — mẻ pha sẵn (RAW→PREPPED). Contract #2. */
public class PrepBatch {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    /** UTC (lưu trong DB) → giờ VN, format dd/MM HH:mm. */
    private static String fmt(LocalDateTime utc) {
        if (utc == null) return "";
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(VN).format(F);
    }
    private int prepBatchId;
    private int branchId;
    private int preppedIngredientId;
    private BigDecimal quantityProduced;
    private int madeBy;
    private LocalDateTime madeAt;
    private LocalDateTime expiresAt;
    private String status = "ACTIVE";   // ACTIVE | CANCELLED
    private LocalDateTime voidedAt;
    /** Đã ghi hao hụt vì quá hạn — mẻ khép vòng đời, không gợi ý ghi thêm và không huỷ được nữa. */
    private LocalDateTime writtenOffAt;
    private Integer writeOffWasteLogId;
    private String clientRequestId;

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

    public Integer getWriteOffWasteLogId() { return writeOffWasteLogId; }
    public void setWriteOffWasteLogId(Integer v) { this.writeOffWasteLogId = v; }
    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String v) { this.clientRequestId = v; }

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

    // ----- Hiển thị -----
    public String getMadeAtDisplay() { return fmt(madeAt); }
    public String getExpiresAtDisplay() { return fmt(expiresAt); }
    public String getQuantityProducedDisplay() {
        return com.cafe.common.QuantityFormat.groupedVi(quantityProduced);
    }

    /** Trạng thái hạn dùng (so với now UTC): none | ok | soon (≤2h) | expired. Chỉ tính cho mẻ ACTIVE. */
    public String getExpiryTier() {
        if (expiresAt == null || !isActive()) return "none";
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (expiresAt.isBefore(now)) return "expired";
        if (expiresAt.isBefore(now.plusHours(2))) return "soon";
        return "ok";
    }
}
