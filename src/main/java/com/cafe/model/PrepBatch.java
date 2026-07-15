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

    // join
    private String preppedIngredientName;
    private String preppedIngredientUnit;
    private String madeByName;

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

    public String getPreppedIngredientName() { return preppedIngredientName; }
    public void setPreppedIngredientName(String v) { this.preppedIngredientName = v; }

    public String getPreppedIngredientUnit() { return preppedIngredientUnit; }
    public void setPreppedIngredientUnit(String v) { this.preppedIngredientUnit = v; }

    public String getMadeByName() { return madeByName; }
    public void setMadeByName(String v) { this.madeByName = v; }

    // ----- Hiển thị -----
    public String getMadeAtDisplay() { return fmt(madeAt); }
    public String getExpiresAtDisplay() { return fmt(expiresAt); }

    /** Trạng thái hạn dùng (so với now UTC): none | ok | soon (≤2h) | expired. Chỉ tính cho mẻ ACTIVE. */
    public String getExpiryTier() {
        if (expiresAt == null || !isActive()) return "none";
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (expiresAt.isBefore(now)) return "expired";
        if (expiresAt.isBefore(now.plusHours(2))) return "soon";
        return "ok";
    }
}
