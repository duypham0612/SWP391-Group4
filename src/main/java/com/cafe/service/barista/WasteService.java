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

    /** Sửa dòng hao hụt — áp txn cho phần chênh lệch. */
    public void updateWaste(int branchId, int wasteLogId, BigDecimal newQty, String wasteType, String reason, int userId) throws SQLException {
        inventoryService.updateWaste(branchId, wasteLogId, newQty, wasteType, reason, userId);
    }

    /** Huỷ dòng hao hụt — hoàn kho qua txn bù (không hard-delete). */
    public void voidWaste(int branchId, int wasteLogId, int userId) throws SQLException {
        inventoryService.voidWaste(branchId, wasteLogId, userId);
    }
}
