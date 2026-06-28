package com.cafe.service.barista;
import com.cafe.service.shared.InventoryService;
import com.cafe.service.admin.IngredientService;

import com.cafe.model.Ingredient;
import com.cafe.model.WasteLog;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** B5 · WasteService — ghi hao hụt/làm lại (qua InventoryService.logWaste + ledger). */
public class WasteService {

    private final InventoryService inventoryService = new InventoryService();
    private final IngredientService ingredientService = new IngredientService();

    public List<Ingredient> getIngredients() throws SQLException {
        return ingredientService.getIngredientList();
    }

    public List<WasteLog> getWasteLogs(int branchId) throws SQLException {
        return inventoryService.getWasteLogs(branchId);
    }

    public int logWaste(int branchId, int ingredientId, BigDecimal qty, String wasteType, String reason, int userId) throws SQLException {
        return inventoryService.logWaste(branchId, ingredientId, qty, wasteType, reason, userId);
    }
}
