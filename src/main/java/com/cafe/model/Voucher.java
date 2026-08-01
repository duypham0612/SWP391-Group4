package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** payment.Voucher */
public class Voucher {
    private int voucherId;
    private String code;
    private String discountType;            // PERCENT | FIXED
    private BigDecimal discountValue = BigDecimal.ZERO;
    private BigDecimal minOrderAmount = BigDecimal.ZERO;
    private String scope = "CHAIN";         // CHAIN | BRANCH
    private Integer branchId;               // chỉ dùng khi scope = BRANCH
    /** UTC, lưu trực tiếp trong database. */
    private LocalDateTime startAtUtc;
    private LocalDateTime endAtUtc;
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

    public LocalDateTime getStartAtUtc() { return startAtUtc; }
    public void setStartAtUtc(LocalDateTime value) { startAtUtc = value; }

    public LocalDateTime getEndAtUtc() { return endAtUtc; }
    public void setEndAtUtc(LocalDateTime value) { endAtUtc = value; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getLifecycleStatusCode() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        if (endAtUtc != null && !nowUtc.isBefore(endAtUtc)) return "EXPIRED";
        if (startAtUtc != null && nowUtc.isBefore(startAtUtc)) return "UPCOMING";
        return "RUNNING";
    }

}
