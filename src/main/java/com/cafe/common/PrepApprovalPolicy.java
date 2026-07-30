package com.cafe.common;

import java.math.BigDecimal;

/** Ngưỡng bất thường cho mẻ pha sẵn: sản lượng vượt quá xa mức mục tiêu → cần Manager duyệt trước khi PREP_IN. */
public final class PrepApprovalPolicy {
    /** Hằng số dễ chỉnh: sản lượng > 1.5 × PrepTargetQty thì coi là bất thường. */
    public static final BigDecimal THRESHOLD_MULTIPLIER = new BigDecimal("1.5");

    private PrepApprovalPolicy() {}

    public static boolean requiresApproval(BigDecimal qtyProduced, BigDecimal prepTargetQty) {
        if (prepTargetQty == null || prepTargetQty.signum() <= 0) return false;
        return qtyProduced.compareTo(prepTargetQty.multiply(THRESHOLD_MULTIPLIER)) > 0;
    }
}
