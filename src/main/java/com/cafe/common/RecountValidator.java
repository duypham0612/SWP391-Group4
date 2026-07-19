package com.cafe.common;

import com.cafe.model.StockAdjustment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Parse form kiểm kê nhanh ở KDS: ô trống bỏ qua, số 0 vẫn là một lần kiểm kê. */
public final class RecountValidator {

    private RecountValidator() { }

    public static List<StockAdjustment> parse(String[] ingredientIds, String[] actualQtys) {
        if (ingredientIds == null && actualQtys == null) return List.of();
        if (ingredientIds == null || actualQtys == null || ingredientIds.length != actualQtys.length) {
            throw new BusinessException("Dữ liệu kiểm kê không khớp. Vui lòng tải lại bảng và thử lại.");
        }

        List<StockAdjustment> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < ingredientIds.length; i++) {
            String qtyText = actualQtys[i];
            if (qtyText == null || qtyText.isBlank()) continue;

            int ingredientId = parseIngredientId(ingredientIds[i]);
            if (!seen.add(ingredientId)) {
                throw new BusinessException("Một nguyên liệu bị gửi trùng trong phiếu kiểm kê.");
            }
            BigDecimal actualQty = parseActualQty(qtyText);

            StockAdjustment a = new StockAdjustment();
            a.setIngredientId(ingredientId);
            a.setActualQty(actualQty);
            out.add(a);
        }
        return out;
    }

    private static int parseIngredientId(String raw) {
        if (raw == null || raw.isBlank()) throw new BusinessException("Nguyên liệu kiểm kê không hợp lệ.");
        try {
            int id = Integer.parseInt(raw.trim());
            if (id <= 0) throw new NumberFormatException("non-positive");
            return id;
        } catch (NumberFormatException e) {
            throw new BusinessException("Nguyên liệu kiểm kê không hợp lệ.");
        }
    }

    private static BigDecimal parseActualQty(String raw) {
        try {
            BigDecimal qty = new BigDecimal(raw.trim());
            if (qty.signum() < 0) throw new BusinessException("Tồn thực tế không được âm.");
            return qty;
        } catch (NumberFormatException e) {
            throw new BusinessException("Tồn thực tế không hợp lệ.");
        }
    }
}
