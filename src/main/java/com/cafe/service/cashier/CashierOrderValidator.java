package com.cafe.service.cashier;

import com.cafe.service.shared.OrderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Validation riêng cho đơn được tạo từ POS của Cashier. */
public final class CashierOrderValidator {

    public static final int MAX_QUANTITY_PER_PRODUCT = 20;

    private CashierOrderValidator() {
    }

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
            if (total > MAX_QUANTITY_PER_PRODUCT) {
                throw quantityLimitError();
            }
            quantityByProduct.put(line.productId, total);
        }
    }

    private static IllegalArgumentException quantityLimitError() {
        return new IllegalArgumentException(
                "Mỗi loại món chỉ được đặt tối đa "
                        + MAX_QUANTITY_PER_PRODUCT + " trong một đơn.");
    }
}
