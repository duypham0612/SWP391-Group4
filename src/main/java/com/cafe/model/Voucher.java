package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** promotion.Voucher — rule snapshot used while applying a code to an unpaid bill. */
public class Voucher {
    private int voucherId;
    private String code;
    private String name;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer usageLimit;
    private int usedCount;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private boolean active;

    public int getVoucherId() { return voucherId; }
    public void setVoucherId(int v) { voucherId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String v) { discountType = v; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal v) { discountValue = v; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(BigDecimal v) { maxDiscountAmount = v; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(BigDecimal v) { minOrderAmount = v; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer v) { usageLimit = v; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int v) { usedCount = v; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime v) { startsAt = v; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime v) { endsAt = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
}
