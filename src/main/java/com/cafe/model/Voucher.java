package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** payment.Voucher */
public class Voucher {
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private int voucherId;
    private String code;
    private String discountType;            // PERCENT | FIXED
    private BigDecimal discountValue = BigDecimal.ZERO;
    private BigDecimal minOrderAmount = BigDecimal.ZERO;
    private String scope = "CHAIN";         // CHAIN | BRANCH
    private Integer branchId;               // chỉ dùng khi scope = BRANCH
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;             // NULL = không giới hạn
    private int usedCount;
    private boolean active = true;

    private String branchName;              // join

    public int getVoucherId() { return voucherId; }
    public void setVoucherId(int voucherId) { this.voucherId = voucherId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    // Helper cho input datetime-local
    public String getStartInput() { return startDate == null ? "" : startDate.format(INPUT_FMT); }
    public String getEndInput()   { return endDate == null ? "" : endDate.format(INPUT_FMT); }

    public String getLifecycleStatusCode() {
        LocalDateTime now = LocalDateTime.now();
        if (endDate != null && now.isAfter(endDate)) return "EXPIRED";
        if (startDate != null && now.isBefore(startDate)) return "UPCOMING";
        return "RUNNING";
    }

    public String getLifecycleStatusLabel() {
        return switch (getLifecycleStatusCode()) {
            case "EXPIRED" -> "H\u1ebft h\u1ea1n";
            case "UPCOMING" -> "S\u1eafp di\u1ec5n ra";
            default -> "\u0110ang di\u1ec5n ra";
        };
    }

    public String getLifecycleBadgeClass() {
        return switch (getLifecycleStatusCode()) {
            case "EXPIRED" -> "badge-cancelled";
            case "UPCOMING" -> "badge-waiting";
            default -> "badge-ready";
        };
    }
}
