package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.IngredientDao;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.math.*;
import java.sql.*;
import java.util.*;

final class InventoryRepository {
    final BranchInventoryDao biDao; final InventoryTransactionDao txnDao;
    final StockReceiptDetailDao detailDao; final StockAdjustmentDao adjustmentDao;
    final IngredientUnitConversionDao conversionDao; final StockCountDao stockCountDao;
    final ProductRecipeDao productRecipeDao; final ModifierIngredientImpactDao impactDao;
    final OrderItemModifierDao oimDao; final OutboxEventDao outboxEventDao;
    final PrepRecipeDao prepRecipeDao; final PrepBatchDao prepBatchDao;
    final WasteEventItemDao wasteEventItemDao; final WasteEventDao wasteEventDao;
    final WasteEventReviewDao wasteEventReviewDao; final WasteEventAuditDao wasteEventAuditDao;
    final IngredientDao ingredientDao;

    InventoryRepository() { this(new BranchInventoryDao(),new InventoryTransactionDao(),new StockReceiptDetailDao(),
            new StockAdjustmentDao(),new IngredientUnitConversionDao(),new StockCountDao(),new ProductRecipeDao(),
            new ModifierIngredientImpactDao(),new OrderItemModifierDao(),new OutboxEventDao(),new PrepRecipeDao(),
            new PrepBatchDao(),new WasteEventItemDao(),new WasteEventDao(),new WasteEventReviewDao(),
            new WasteEventAuditDao(),new IngredientDao()); }
    InventoryRepository(BranchInventoryDao biDao,InventoryTransactionDao txnDao,StockReceiptDetailDao detailDao,
            StockAdjustmentDao adjustmentDao,IngredientUnitConversionDao conversionDao,StockCountDao stockCountDao,
            ProductRecipeDao productRecipeDao,ModifierIngredientImpactDao impactDao,OrderItemModifierDao oimDao,
            OutboxEventDao outboxEventDao,PrepRecipeDao prepRecipeDao,PrepBatchDao prepBatchDao,
            WasteEventItemDao wasteEventItemDao,WasteEventDao wasteEventDao,WasteEventReviewDao wasteEventReviewDao,
            WasteEventAuditDao wasteEventAuditDao,IngredientDao ingredientDao){
        this.biDao=Objects.requireNonNull(biDao);this.txnDao=Objects.requireNonNull(txnDao);
        this.detailDao=Objects.requireNonNull(detailDao);this.adjustmentDao=Objects.requireNonNull(adjustmentDao);
        this.conversionDao=Objects.requireNonNull(conversionDao);this.stockCountDao=Objects.requireNonNull(stockCountDao);
        this.productRecipeDao=Objects.requireNonNull(productRecipeDao);this.impactDao=Objects.requireNonNull(impactDao);
        this.oimDao=Objects.requireNonNull(oimDao);this.outboxEventDao=Objects.requireNonNull(outboxEventDao);
        this.prepRecipeDao=Objects.requireNonNull(prepRecipeDao);this.prepBatchDao=Objects.requireNonNull(prepBatchDao);
        this.wasteEventItemDao=Objects.requireNonNull(wasteEventItemDao);this.wasteEventDao=Objects.requireNonNull(wasteEventDao);
        this.wasteEventReviewDao=Objects.requireNonNull(wasteEventReviewDao);this.wasteEventAuditDao=Objects.requireNonNull(wasteEventAuditDao);
        this.ingredientDao=Objects.requireNonNull(ingredientDao);
    }
}
