package com.cafe.service.shared;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** So sánh tổng nguyên liệu cần cho một đơn với tồn đã khoá trong transaction. */
public final class OrderStockValidator {
    private OrderStockValidator() { }

    public record Shortfall(int ingredientId, BigDecimal required, BigDecimal onHand) { }

    /** Chỉ trả các nguyên liệu mà đơn cần nhiều hơn số đang có. */
    public static List<Shortfall> findShortfalls(Map<Integer, BigDecimal> requiredByIngredient,
                                                  Map<Integer, BigDecimal> onHandByIngredient) {
        List<Shortfall> out = new ArrayList<>();
        if (requiredByIngredient == null) return out;
        for (Map.Entry<Integer, BigDecimal> entry : requiredByIngredient.entrySet()) {
            BigDecimal required = entry.getValue();
            if (required == null || required.signum() <= 0) continue;
            BigDecimal onHand = onHandByIngredient == null
                    ? BigDecimal.ZERO
                    : onHandByIngredient.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            if (onHand == null) onHand = BigDecimal.ZERO;
            if (onHand.compareTo(required) < 0) {
                out.add(new Shortfall(entry.getKey(), required, onHand));
            }
        }
        out.sort(Comparator.comparingInt(Shortfall::ingredientId));
        return out;
    }
}
