package com.cafe.service.shared;

import com.cafe.model.WasteLog;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tổng hợp hao hụt/làm lại từ một danh sách WasteLog — hàm thuần, không đụng DB.
 * Đặt ở service.shared vì cả Pha chế lẫn Quản lý chi nhánh đều dùng.
 */
public final class WasteSummary {
    private int activeCount;
    private int ingredientWasteCount;
    private int remakeCount;
    private int missingCostCount;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private BigDecimal ingredientWasteCost = BigDecimal.ZERO;
    private BigDecimal remakeCost = BigDecimal.ZERO;
    private String topIngredientName;
    private BigDecimal topIngredientCost = BigDecimal.ZERO;

    private WasteSummary() { }

    public static WasteSummary from(List<WasteLog> logs) {
        WasteSummary s = new WasteSummary();
        Map<String, BigDecimal> byIngredient = new LinkedHashMap<>();
        if (logs == null) return s;
        for (WasteLog log : logs) {
            if (log == null || !log.isActive()) continue;
            s.activeCount++;
            if (log.isRemake()) s.remakeCount++; else s.ingredientWasteCount++;

            BigDecimal cost = log.getLineCost();
            if (cost == null) {
                s.missingCostCount++;
                continue;
            }
            s.totalCost = s.totalCost.add(cost);
            if (log.isRemake()) s.remakeCost = s.remakeCost.add(cost);
            else s.ingredientWasteCost = s.ingredientWasteCost.add(cost);

            String name = log.getIngredientName() == null
                    ? "Nguyên liệu #" + log.getIngredientId() : log.getIngredientName();
            byIngredient.merge(name, cost, BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> e : byIngredient.entrySet()) {
            if (s.topIngredientName == null || e.getValue().compareTo(s.topIngredientCost) > 0) {
                s.topIngredientName = e.getKey();
                s.topIngredientCost = e.getValue();
            }
        }
        return s;
    }

    public int getActiveCount() { return activeCount; }
    public int getIngredientWasteCount() { return ingredientWasteCount; }
    public int getRemakeCount() { return remakeCount; }
    public int getMissingCostCount() { return missingCostCount; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getIngredientWasteCost() { return ingredientWasteCost; }
    public BigDecimal getRemakeCost() { return remakeCost; }
    public String getTopIngredientName() { return topIngredientName; }
    public BigDecimal getTopIngredientCost() { return topIngredientCost; }
    public boolean isHasTopIngredient() { return topIngredientName != null; }
}
