package com.cafe.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Quy đổi có kiểm soát precision; tồn kho chỉ chấp nhận tối đa 3 số lẻ. */
public final class InventoryUnitConverter {
    private InventoryUnitConverter() { }

    public static BigDecimal toBase(BigDecimal enteredQuantity, BigDecimal factorToBase) {
        if (enteredQuantity == null || enteredQuantity.signum() < 0) {
            throw new BusinessException("Số lượng không được để trống hoặc nhỏ hơn 0.");
        }
        if (factorToBase == null || factorToBase.signum() <= 0) {
            throw new BusinessException("Hệ số quy đổi phải lớn hơn 0.");
        }
        BigDecimal exact = enteredQuantity.multiply(factorToBase);
        try {
            BigDecimal base = exact.setScale(3, RoundingMode.UNNECESSARY);
            if (base.precision() - base.scale() > 9) {
                throw new BusinessException("Số lượng sau quy đổi vượt giới hạn tồn kho.");
            }
            return base;
        } catch (ArithmeticException e) {
            throw new BusinessException(
                    "Số lượng sau quy đổi có quá 3 chữ số thập phân; vui lòng chọn số lượng phù hợp.");
        }
    }
}
