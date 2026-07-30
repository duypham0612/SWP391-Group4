package com.cafe.service.shared;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Giới hạn số lượng dùng chung cho đơn tại quầy và đơn QR. */
public final class OrderQuantityValidator {
    public static final int MAX_QUANTITY_PER_PRODUCT = 20;

    private OrderQuantityValidator() { }

    public static void validate(List<OrderService.CartLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng không được để trống.");
        }

        Map<Integer, Integer> quantityByProduct = new HashMap<>();
        for (OrderService.CartLine line : lines) {
            if (line == null || line.productId <= 0) {
                throw new IllegalArgumentException("Món trong đơn không hợp lệ.");
            }
            if (line.quantity <= 0) {
                throw new IllegalArgumentException("Số lượng món phải lớn hơn 0.");
            }

            int total;
            try {
                total = Math.addExact(
                        quantityByProduct.getOrDefault(line.productId, 0),
                        line.quantity);
            } catch (ArithmeticException e) {
                throw quantityLimitError();
            }
            if (total > MAX_QUANTITY_PER_PRODUCT) throw quantityLimitError();
            quantityByProduct.put(line.productId, total);
        }
    }

    private static IllegalArgumentException quantityLimitError() {
        return new IllegalArgumentException(
                "Mỗi loại món chỉ được đặt tối đa "
                        + MAX_QUANTITY_PER_PRODUCT + " trong một đơn.");
    }
}
