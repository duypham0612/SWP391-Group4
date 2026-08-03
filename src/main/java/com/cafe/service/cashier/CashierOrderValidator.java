package com.cafe.service.cashier;

import com.cafe.model.CartLine;

import java.util.List;
import com.cafe.service.shared.OrderQuantityValidator;

/**
 * Facade giữ tương thích cho POS Cashier.
 * Quy tắc thực tế nằm ở OrderQuantityValidator để QR và backend dùng cùng một giới hạn.
 */
public final class CashierOrderValidator {

    public static final int MAX_QUANTITY_PER_PRODUCT =
            OrderQuantityValidator.MAX_QUANTITY_PER_PRODUCT;

    private CashierOrderValidator() {
    }

    public static void validate(List<CartLine> lines) {
        OrderQuantityValidator.validate(lines);
    }
}
