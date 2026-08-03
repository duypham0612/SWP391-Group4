package com.cafe.service.shared;

import com.cafe.dao.ops.ActivityLogDao;
import com.cafe.dao.inventory.BranchInventoryDao;
import com.cafe.dao.catalog.IngredientDao;
import com.cafe.dao.catalog.IngredientUnitDao;
import com.cafe.dao.inventory.InventoryTransactionDao;
import com.cafe.dao.sales.OrderItemModifierDao;
import com.cafe.dao.ops.OutboxEventDao;
import com.cafe.dao.inventory.PrepBatchApprovalDao;
import com.cafe.dao.inventory.PrepBatchDao;
import com.cafe.dao.catalog.RecipeDao;
import com.cafe.dao.inventory.StockAdjustmentDao;
import com.cafe.dao.inventory.StockCountDao;
import com.cafe.dao.inventory.StockReceiptDetailDao;
import com.cafe.dao.inventory.WasteEventDao;
import com.cafe.dao.inventory.WasteEventItemDao;
import com.cafe.dao.inventory.WasteEventReviewDao;
import com.cafe.common.*;
import com.cafe.model.*;

import java.math.*;
import java.sql.*;
import java.util.*;

final class InventoryRepository {
    final BranchInventoryDao biDao; final InventoryTransactionDao txnDao;
    final StockReceiptDetailDao detailDao; final StockAdjustmentDao adjustmentDao;
    final IngredientUnitDao unitDao; final StockCountDao stockCountDao;
    final RecipeDao productRecipeDao; final RecipeDao impactDao;
    final OrderItemModifierDao oimDao; final OutboxEventDao outboxEventDao;
    final RecipeDao prepRecipeDao; final PrepBatchDao prepBatchDao;
    final PrepBatchApprovalDao prepApprovalDao;
    final WasteEventItemDao wasteEventItemDao; final WasteEventDao wasteEventDao;
    final WasteEventReviewDao wasteEventReviewDao; final ActivityLogDao activityLogDao;
    final IngredientDao ingredientDao;

    InventoryRepository() { this(new BranchInventoryDao(),new InventoryTransactionDao(),new StockReceiptDetailDao(),
            new StockAdjustmentDao(),new IngredientUnitDao(),new StockCountDao(),new RecipeDao(),
            new OrderItemModifierDao(),new OutboxEventDao(),
            new PrepBatchDao(),new PrepBatchApprovalDao(),new WasteEventItemDao(),new WasteEventDao(),new WasteEventReviewDao(),
            new ActivityLogDao(),new IngredientDao()); }
    InventoryRepository(BranchInventoryDao biDao,InventoryTransactionDao txnDao,StockReceiptDetailDao detailDao,
            StockAdjustmentDao adjustmentDao,IngredientUnitDao unitDao,StockCountDao stockCountDao,
            RecipeDao recipeDao,OrderItemModifierDao oimDao,
            OutboxEventDao outboxEventDao,PrepBatchDao prepBatchDao,PrepBatchApprovalDao prepApprovalDao,
            WasteEventItemDao wasteEventItemDao,WasteEventDao wasteEventDao,WasteEventReviewDao wasteEventReviewDao,
            ActivityLogDao activityLogDao,IngredientDao ingredientDao){
        this.biDao=Objects.requireNonNull(biDao);this.txnDao=Objects.requireNonNull(txnDao);
        this.detailDao=Objects.requireNonNull(detailDao);this.adjustmentDao=Objects.requireNonNull(adjustmentDao);
        this.unitDao=Objects.requireNonNull(unitDao);this.stockCountDao=Objects.requireNonNull(stockCountDao);
        this.productRecipeDao=Objects.requireNonNull(recipeDao);this.impactDao=recipeDao;
        this.oimDao=Objects.requireNonNull(oimDao);this.outboxEventDao=Objects.requireNonNull(outboxEventDao);
        this.prepRecipeDao=recipeDao;this.prepBatchDao=Objects.requireNonNull(prepBatchDao);
        this.prepApprovalDao=Objects.requireNonNull(prepApprovalDao);
        this.wasteEventItemDao=Objects.requireNonNull(wasteEventItemDao);this.wasteEventDao=Objects.requireNonNull(wasteEventDao);
        this.wasteEventReviewDao=Objects.requireNonNull(wasteEventReviewDao);this.activityLogDao=Objects.requireNonNull(activityLogDao);
        this.ingredientDao=Objects.requireNonNull(ingredientDao);
    }
}
