package com.cafe.service.barista;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.admin.IngredientService;

import com.cafe.model.Ingredient;
import com.cafe.model.PrepBatch;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** B4 · PrepService — pha sẵn (RAW→PREPPED qua InventoryService.createPrepBatch, Contract #2). */
public class PrepService {

    private final InventoryService inventoryService = new InventoryService();
    private final IngredientService ingredientService = new IngredientService();

    public List<Ingredient> getPreppedIngredients() throws SQLException {
        return ingredientService.getIngredientListByType("PREPPED");
    }

    public List<PrepBatch> getBatches(int branchId) throws SQLException {
        return inventoryService.getPrepBatches(branchId);
    }

    public int createBatch(int branchId, int preppedIngredientId, BigDecimal qtyProduced, int userId) throws SQLException {
        return inventoryService.createPrepBatch(branchId, preppedIngredientId, qtyProduced, userId);
    }
}
