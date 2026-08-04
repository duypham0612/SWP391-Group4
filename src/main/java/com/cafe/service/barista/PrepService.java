package com.cafe.service.barista;

import com.cafe.common.BusinessException;
import com.cafe.model.BranchInventory;
import com.cafe.model.Ingredient;
import com.cafe.model.PrepBatch;
import com.cafe.model.PrepChecklistRow;
import com.cafe.model.Recipe;
import com.cafe.service.admin.IngredientService;
import com.cafe.service.shared.InventoryService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** B4 · PrepService — prep-ahead (RAW→PREPPED via InventoryService.createPrepBatch, Contract #2). */
public class PrepService {

    private final InventoryService inventoryService;
    private final IngredientService ingredientService;

    public PrepService() { this(new InventoryService(), new IngredientService()); }
    public PrepService(InventoryService inventoryService, IngredientService ingredientService) {
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService");
        this.ingredientService = Objects.requireNonNull(ingredientService, "ingredientService");
    }

    public List<Ingredient> getPreppedIngredients() throws SQLException {
        return ingredientService.getIngredientListByType("PREPPED");
    }

    public List<PrepBatch> getExpiredActiveBatches(int branchId) throws SQLException {
        return inventoryService.getExpiredActivePrepBatches(branchId);
    }

    /** "Needs prepping today" checklist: PREPPED stock ≤ threshold. */
    public List<PrepChecklistRow> getPrepChecklist(int branchId) throws SQLException {
        return inventoryService.getPrepChecklist(branchId);
    }

    public PrepBatch createSuggestedBatch(int branchId, int preppedIngredientId, BigDecimal qtyProduced,
                                    int userId, String clientRequestId) throws SQLException {
        return inventoryService.createSuggestedPrepBatch(branchId, preppedIngredientId, qtyProduced,
                userId, clientRequestId);
    }

    /**
     * Discards the expired prep-ahead portion using exactly the system-suggested quantity: records
     * the waste + closes out the batch lifecycle in a single transaction. Returns the WasteEntryId.
     *
     * <p>The discarded quantity is taken from the still-expired batch itself, not from the client —
     * the barista only clicks confirm and never enters a number, so there's no way to fake it.
     */
    public long writeOffExpiredBatchSuggested(int branchId, int prepBatchId, int userId) throws SQLException {
        for (PrepBatch batch : inventoryService.getExpiredActivePrepBatches(branchId)) {
            if (batch.getPrepBatchId() == prepBatchId && batch.isHasSuggestedWaste()) {
                return inventoryService.writeOffExpiredPrepBatch(branchId, prepBatchId,
                        batch.getSuggestedWasteQuantity(), userId);
            }
        }
        throw new BusinessException(
                "Mẻ này không còn tồn để loại bỏ hoặc đã được xử lý. Vui lòng tải lại.");
    }

    public List<PrepBatch> getRecentBatches(int branchId) throws SQLException {
        return inventoryService.getRecentPrepBatches(branchId, 5);
    }

    /**
     * Recipe JSON for the client-side preview: {preppedId:[{n:rawName,u:unit,q:qty,y:yield}]}.
     * Used to show "how much RAW will be deducted" as the barista enters the output quantity.
     */
    public String getRecipeJson(List<Ingredient> preppedIngredients) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        Map<Integer, BigDecimal> yieldByIngredient = new HashMap<>();
        if (preppedIngredients != null)
            for (Ingredient i : preppedIngredients) {
                ids.add(i.getIngredientId());
                yieldByIngredient.put(i.getIngredientId(), i.getPrepYieldQty());
            }
        Map<Integer, List<Recipe>> map = inventoryService.getPrepRecipeMap(ids);
        StringBuilder sb = new StringBuilder("{");
        boolean firstKey = true;
        for (Map.Entry<Integer, List<Recipe>> e : map.entrySet()) {
            if (!firstKey) sb.append(',');
            firstKey = false;
            sb.append('"').append(e.getKey()).append("\":[");
            boolean firstLine = true;
            List<Recipe> recipe = e.getValue();
            BigDecimal prepYield = yieldByIngredient.get(e.getKey());
            for (Recipe line : recipe) {
                if (!firstLine) sb.append(',');
                firstLine = false;
                sb.append("{\"r\":").append(line.getIngredientId())
                  .append(",\"n\":\"").append(esc(line.getIngredientName()))
                  .append("\",\"u\":\"").append(esc(line.getIngredientUnit()))
                  .append("\",\"q\":").append(line.getQuantity().toPlainString())
                  .append(",\"y\":").append(prepYield == null ? "null" : prepYield.toPlainString())
                  .append('}');
            }
            sb.append(']');
        }
        return sb.append('}').toString();
    }

    /** Branch's current RAW stock on hand for client-side preview/warnings: {rawId: onHand}. */
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

    /**
     * Escaping for a JSON string embedded DIRECTLY into the {@code <script>} of {@code prep.jsp}
     * ({@code var recipes = ${recipeJson};}).
     *
     * <p>DELIBERATELY hand-rolled instead of using Jackson: besides the required JSON characters,
     * this function also converts {@code < > & '} to {@code \\uXXXX} — otherwise an ingredient
     * name containing {@code </script>} would close the script tag early and turn data into
     * executable code. Jackson does NOT escape those characters by default, so swapping in
     * Jackson here would be a security downgrade.
     */
    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("<", "\\u003C").replace(">", "\\u003E")
                .replace("&", "\\u0026").replace("'", "\\u0027")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
