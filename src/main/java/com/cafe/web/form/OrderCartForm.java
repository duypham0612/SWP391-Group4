package com.cafe.web.form;

import com.cafe.service.shared.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Payload JSON giỏ hàng ở ranh giới HTTP; Service xác minh toàn bộ quy tắc đặt món. */
public record OrderCartForm(Integer tableId, List<Line> lines) {
    public static OrderCartForm fromJson(HttpServletRequest request, ObjectMapper mapper) throws IOException {
        JsonNode body = mapper.readTree(request.getInputStream());
        if (body == null || !body.isObject()) {
            throw new FormBindingException("Giỏ hàng không đúng định dạng.");
        }
        Integer tableId = null;
        JsonNode tableNode = body.get("tableId");
        if (tableNode != null && !tableNode.isNull()) {
            if (!tableNode.isIntegralNumber() || !tableNode.canConvertToInt()) {
                throw new FormBindingException("Bàn không hợp lệ.");
            }
            tableId = tableNode.intValue();
        }

        JsonNode items = body.get("items");
        if (items != null && !items.isArray()) {
            throw new FormBindingException("Danh sách món không đúng định dạng.");
        }
        List<Line> lines = new ArrayList<>();
        if (items != null) {
            for (JsonNode item : items) lines.add(parseLine(item));
        }
        return new OrderCartForm(tableId, lines);
    }

    public List<OrderService.CartLine> toCartLines() {
        List<OrderService.CartLine> result = new ArrayList<>(lines.size());
        for (Line source : lines) {
            OrderService.CartLine target = new OrderService.CartLine();
            target.productId = source.productId();
            target.quantity = source.quantity();
            target.note = source.note();
            target.optionIds.addAll(source.optionIds());
            result.add(target);
        }
        return result;
    }

    private static Line parseLine(JsonNode item) {
        if (item == null || !item.isObject()) {
            throw new FormBindingException("Dòng món không đúng định dạng.");
        }
        int productId = integral(item.get("productId"), 0, "Mã món");
        int quantity = integral(item.get("quantity"), 1, "Số lượng");
        JsonNode noteNode = item.get("note");
        if (noteNode != null && !noteNode.isNull() && !noteNode.isTextual()) {
            throw new FormBindingException("Ghi chú món không đúng định dạng.");
        }
        String note = noteNode == null || noteNode.isNull() ? null : noteNode.textValue();
        List<Integer> optionIds = new ArrayList<>();
        JsonNode options = item.get("optionIds");
        if (options != null && !options.isArray()) {
            throw new FormBindingException("Danh sách tuỳ chọn không đúng định dạng.");
        }
        if (options != null) {
            for (JsonNode option : options) {
                optionIds.add(integral(option, 0, "Mã tuỳ chọn"));
            }
        }
        return new Line(productId, quantity, note, optionIds);
    }

    private static int integral(JsonNode value, int fallback, String label) {
        if (value == null || value.isNull()) return fallback;
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new FormBindingException(label + " không hợp lệ.");
        }
        return value.intValue();
    }

    public record Line(int productId, int quantity, String note, List<Integer> optionIds) { }
}
