package com.cafe.service.shared;

import com.cafe.model.WasteEventItem;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Tổng hợp hao hụt/làm lại từ một danh sách WasteEventItem — hàm thuần, không đụng DB.
 * Đặt ở service.shared vì cả Pha chế lẫn Quản lý chi nhánh đều dùng.
 */
public final class WasteSummary {
    private int activeCount;
    private int ingredientWasteCount;
    private int spillCount;
    private int expiredCount;
    private int otherCount;
    private int remakeCount;
    private int missingCostCount;
    private BigDecimal totalCost = BigDecimal.ZERO;
    private BigDecimal ingredientWasteCost = BigDecimal.ZERO;
    private BigDecimal remakeCost = BigDecimal.ZERO;
    private String topIngredientName;
    private BigDecimal topIngredientCost = BigDecimal.ZERO;

    private WasteSummary() { }

    public static WasteSummary from(List<WasteEventItem> logs) {
        WasteSummary s = new WasteSummary();
        Map<String, BigDecimal> byIngredient = new LinkedHashMap<>();
        Set<String> remakeEvents = new HashSet<>();
        if (logs == null) return s;
        for (WasteEventItem log : logs) {
            if (log == null || !log.isActive()) continue;
            s.activeCount++;
            if (log.isRemake()) {
                // Event mới đại diện cho một lần/ly remake; dữ liệu legacy không có event giữ cách đếm cũ.
                String eventGroupId = log.getEventGroupId();
                if (eventGroupId == null || remakeEvents.add(eventGroupId)) s.remakeCount++;
            } else {
                s.ingredientWasteCount++;
                // Ba loại của hao hụt nguyên liệu; loại lạ (dữ liệu cũ) gom vào "Khác" để tổng luôn khớp.
                if ("SPILL".equals(log.getWasteType())) s.spillCount++;
                else if ("EXPIRED".equals(log.getWasteType())) s.expiredCount++;
                else s.otherCount++;
            }

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
    public int getSpillCount() { return spillCount; }
    public int getExpiredCount() { return expiredCount; }
    public int getOtherCount() { return otherCount; }
    public int getRemakeCount() { return remakeCount; }
    public int getMissingCostCount() { return missingCostCount; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getIngredientWasteCost() { return ingredientWasteCost; }
    public BigDecimal getRemakeCost() { return remakeCost; }
    public String getTopIngredientName() { return topIngredientName; }
    public BigDecimal getTopIngredientCost() { return topIngredientCost; }
    public boolean isHasTopIngredient() { return topIngredientName != null; }
}
