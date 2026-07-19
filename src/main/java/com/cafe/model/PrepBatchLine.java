package com.cafe.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một dòng yêu cầu tạo mẻ pha sẵn (dùng cho tạo nhiều mẻ một lần — B4). */
public class PrepBatchLine {
    private final int preppedIngredientId;
    private final BigDecimal qtyProduced;
    private final LocalDateTime expiresAt;   // null nếu không đặt hạn
    private final String preppedIngredientName;

    public PrepBatchLine(int preppedIngredientId, BigDecimal qtyProduced, LocalDateTime expiresAt) {
        this(preppedIngredientId, qtyProduced, expiresAt, null);
    }

    public PrepBatchLine(int preppedIngredientId, BigDecimal qtyProduced, LocalDateTime expiresAt, String preppedIngredientName) {
        this.preppedIngredientId = preppedIngredientId;
        this.qtyProduced = qtyProduced;
        this.expiresAt = expiresAt;
        this.preppedIngredientName = preppedIngredientName;
    }

    public int getPreppedIngredientId() { return preppedIngredientId; }
    public BigDecimal getQtyProduced() { return qtyProduced; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getPreppedIngredientName() { return preppedIngredientName; }
}
