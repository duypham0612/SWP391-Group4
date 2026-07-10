package com.cafe.service.barista;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.admin.IngredientService;

import com.cafe.model.BranchInventory;
import com.cafe.model.Ingredient;
import com.cafe.model.PrepBatch;
import com.cafe.model.PrepBatchLine;
import com.cafe.model.PrepChecklistRow;
import com.cafe.model.PrepRecipe;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** B4 · PrepService — pha sẵn (RAW→PREPPED qua InventoryService.createPrepBatch, Contract #2). */
public class PrepService {

    private final InventoryService inventoryService = new InventoryService();
    private final IngredientService ingredientService = new IngredientService();

    public List<Ingredient> getPreppedIngredients() throws SQLException {
        return ingredientService.getIngredientListByType("PREPPED");
    }

    /** Mẻ pha hôm nay (không liệt kê vô hạn lịch sử). */
    public List<PrepBatch> getTodayBatches(int branchId) throws SQLException {
        return inventoryService.getTodayPrepBatches(branchId);
    }

    /** Checklist "cần pha hôm nay": PREPPED tồn ≤ ngưỡng. */
    public List<PrepChecklistRow> getPrepChecklist(int branchId) throws SQLException {
        return inventoryService.getPrepChecklist(branchId);
    }

    public int createBatch(int branchId, int preppedIngredientId, BigDecimal qtyProduced,
                           LocalDateTime expiresAt, int userId) throws SQLException {
        return inventoryService.createPrepBatch(branchId, preppedIngredientId, qtyProduced, expiresAt, userId);
    }

    /** Tạo nhiều mẻ một lần — tất cả-hoặc-không (một transaction). */
    public void createBatches(int branchId, List<PrepBatchLine> lines, int userId) throws SQLException {
        inventoryService.createPrepBatches(branchId, lines, userId);
    }

    /** Huỷ mẻ — hoàn kho qua txn bù (không hard-delete). */
    public void cancelBatch(int branchId, int prepBatchId, int userId) throws SQLException {
        inventoryService.cancelPrepBatch(branchId, prepBatchId, userId);
    }

    /** Sửa sản lượng mẻ — áp txn cho phần chênh lệch. */
    public void updateBatch(int branchId, int prepBatchId, BigDecimal newQtyProduced, int userId) throws SQLException {
        inventoryService.updatePrepBatch(branchId, prepBatchId, newQtyProduced, userId);
    }

    /**
     * JSON công thức cho preview phía client: {preppedId:[{n:rawName,u:unit,q:qty,y:yield}]}.
     * Dùng để hiển thị "sẽ trừ bao nhiêu RAW" khi barista nhập sản lượng.
     */
    public String getRecipeJson(List<Ingredient> preppedIngredients) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        if (preppedIngredients != null)
            for (Ingredient i : preppedIngredients) ids.add(i.getIngredientId());
        Map<Integer, List<PrepRecipe>> map = inventoryService.getPrepRecipeMap(ids);
        StringBuilder sb = new StringBuilder("{");
        boolean firstKey = true;
        for (Map.Entry<Integer, List<PrepRecipe>> e : map.entrySet()) {
            if (!firstKey) sb.append(',');
            firstKey = false;
            sb.append('"').append(e.getKey()).append("\":[");
            boolean firstLine = true;
            for (PrepRecipe pr : e.getValue()) {
                if (!firstLine) sb.append(',');
                firstLine = false;
                sb.append("{\"r\":").append(pr.getRawIngredientId())
                  .append(",\"n\":\"").append(esc(pr.getRawIngredientName()))
                  .append("\",\"u\":\"").append(esc(pr.getRawIngredientUnit()))
                  .append("\",\"q\":").append(pr.getQuantity().toPlainString())
                  .append(",\"y\":").append(pr.getYieldQty().toPlainString())
                  .append('}');
            }
            sb.append(']');
        }
        return sb.append('}').toString();
    }

    /** Tồn RAW hiện tại của chi nhánh cho preview/cảnh báo client: {rawId: onHand}. */
    public String getRawOnHandJson(int branchId) throws SQLException {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (BranchInventory bi : inventoryService.getBranchInventory(branchId)) {
            if (!"RAW".equals(bi.getIngredientType())) continue;
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(bi.getIngredientId()).append("\":")
              .append(bi.getQuantityOnHand() == null ? "0" : bi.getQuantityOnHand().toPlainString());
        }
        return sb.append('}').toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
