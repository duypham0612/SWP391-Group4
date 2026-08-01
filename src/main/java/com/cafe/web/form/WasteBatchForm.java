package com.cafe.web.form;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

/** Dữ liệu HTTP thô của một batch hao hụt; WasteService sở hữu mapping/gộp/invariant. */
public record WasteBatchForm(String clientRequestId, List<Line> lines) {
    public static WasteBatchForm from(HttpServletRequest request) {
        String[] ingredientIds = values(request.getParameterValues("ingredientId"));
        String[] quantities = values(request.getParameterValues("quantity"));
        String[] wasteTypes = values(request.getParameterValues("wasteType"));
        String[] reasonPresets = values(request.getParameterValues("reasonPreset"));
        String[] reasonDetails = values(request.getParameterValues("reasonDetail"));
        int size = ingredientIds.length;
        if (quantities.length != size || wasteTypes.length != size
                || reasonPresets.length != size || reasonDetails.length != size) {
            throw new FormBindingException(
                    "Dữ liệu các dòng hao hụt không đầy đủ. Vui lòng tải lại màn hình và thử lại.");
        }
        List<Line> lines = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            lines.add(new Line(ingredientIds[index], quantities[index], wasteTypes[index],
                    reasonPresets[index], reasonDetails[index]));
        }
        return new WasteBatchForm(FormValues.trim(request.getParameter("clientRequestId")), lines);
    }

    private static String[] values(String[] input) {
        return input == null ? new String[0] : input;
    }

    public record Line(String ingredientId, String quantity, String wasteType,
                       String reasonPreset, String reasonDetail) { }
}
