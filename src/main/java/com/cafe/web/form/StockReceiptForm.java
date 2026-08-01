package com.cafe.web.form;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Request nhiều dòng của phiếu nhập; conversion và precision được Service xác minh lại. */
public record StockReceiptForm(int receiptId, List<Line> lines) {
    public static StockReceiptForm from(HttpServletRequest request) {
        int receiptId = FormValues.optionalInt(request.getParameter("receiptId"), "Mã phiếu nhập");
        String[] picks = request.getParameterValues("pick");
        List<Line> lines = new ArrayList<>();
        if (picks != null) {
            for (String rawIngredientId : picks) {
                int ingredientId = FormValues.optionalInt(rawIngredientId, "Mã nguyên liệu");
                BigDecimal quantity = FormValues.decimal(
                        request.getParameter("qty_" + ingredientId), "Số lượng");
                BigDecimal unitCost = FormValues.decimal(
                        request.getParameter("cost_" + ingredientId), "Đơn giá");
                int conversionId = FormValues.optionalInt(
                        request.getParameter("unitConversionId_" + ingredientId), "Đơn vị nhập");
                lines.add(new Line(ingredientId, quantity, unitCost, conversionId));
            }
        }
        return new StockReceiptForm(receiptId, lines);
    }

    public record Line(int ingredientId, BigDecimal quantity, BigDecimal unitCost,
                       int unitConversionId) { }
}
