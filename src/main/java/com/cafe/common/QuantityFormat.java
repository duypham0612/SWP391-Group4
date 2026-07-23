package com.cafe.common;

import java.math.BigDecimal;

/**
 * Hiển thị SỐ LƯỢNG (tồn kho, công thức, hao hụt) — bỏ số 0 thừa sau dấu thập phân.
 *
 * Cột số lượng trong DB là DECIMAL(x,3) nên JDBC luôn trả về đủ scale: 70310.000, 21.600.
 * In thẳng ra JSP sẽ thành "70310.000 ml" — nhiễu và khó đọc.
 *
 * KHÔNG làm tròn về số nguyên: 21.600 → "21.6" chứ không phải "22". Lượng lẻ là thật
 * (18g cà phê × hệ số size 1.2 = 21.6g), làm tròn khi hiển thị sẽ khiến báo cáo hao hụt
 * lệch so với sổ cái inventory.InventoryTransaction.
 *
 * Tiền KHÔNG dùng helper này — tiền đã có {@code fmt:formatNumber maxFractionDigits="0"} ở JSP.
 */
public final class QuantityFormat {
    private QuantityFormat() {}

    /** 70310.000 → "70310" · 21.600 → "21.6" · null → "". */
    public static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
