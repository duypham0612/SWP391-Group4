package com.cafe.service.barista;

import com.cafe.common.DeductionCalculator;
import com.cafe.common.PrepConsumptionCalculator;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Calculates immediate prep demand from open KDS items plus the configured safety threshold. */
public class PrepRecommendationService {
    private final BranchInventoryDao inventoryDao = new BranchInventoryDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();
    private final PrepRecipeDao prepRecipeDao = new PrepRecipeDao();

    public List<PrepRecommendation> getRecommendations(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<BranchInventory> inventory = inventoryDao.findByBranch(conn, branchId);
            Map<Integer, BranchInventory> byId = new LinkedHashMap<>();
            for (BranchInventory row : inventory) byId.put(row.getIngredientId(), row);

            Map<Integer, BigDecimal> demand = new HashMap<>();
            for (OrderItem item : orderItemDao.findKdsQueue(conn, branchId)) {
                if (item.isRemakeInventoryReserved()) continue;
                List<ProductRecipe> recipe = productRecipeDao.findByProduct(conn, item.getProductId());
                Map<Integer, BigDecimal> required = DeductionCalculator.computeRequired(recipe, item.getQuantity(),
                        item.getSize(), item.getIceLevel(), item.getSugarLevel());
                for (Map.Entry<Integer, BigDecimal> entry : required.entrySet()) {
                    BranchInventory ingredient = byId.get(entry.getKey());
                    if (ingredient != null && "PREPPED".equals(ingredient.getIngredientType())) {
                        demand.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
                    }
                }
            }

            List<PrepRecommendation> out = new ArrayList<>();
            for (BranchInventory row : inventory) {
                if (!"PREPPED".equals(row.getIngredientType())) continue;
                List<PrepRecipe> prepRecipe = prepRecipeDao.findByPrepped(conn, row.getIngredientId());
                BigDecimal suggested = PrepRecommendation.calculate(row.getQuantityOnHand(), row.getMinThreshold(),
                        demand.getOrDefault(row.getIngredientId(), BigDecimal.ZERO));
                List<String> shortfalls = new ArrayList<>();
                if (suggested.signum() > 0) {
                    for (PrepRecipe line : prepRecipe) {
                        BigDecimal needed = PrepConsumptionCalculator.consumedRaw(suggested, line);
                        BranchInventory raw = byId.get(line.getRawIngredientId());
                        BigDecimal available = raw == null || raw.getQuantityOnHand() == null
                                ? BigDecimal.ZERO : raw.getQuantityOnHand();
                        if (available.compareTo(needed) < 0) {
                            shortfalls.add(line.getRawIngredientName() + ": cần " + plain(needed)
                                    + ", còn " + plain(available) + " " + line.getRawIngredientUnit());
                        }
                    }
                }
                out.add(new PrepRecommendation(row.getIngredientId(), row.getIngredientName(), row.getIngredientUnit(),
                        row.getQuantityOnHand(), row.getMinThreshold(), demand.get(row.getIngredientId()),
                        !prepRecipe.isEmpty(), shortfalls));
            }
            out.sort(Comparator.comparing(PrepRecommendation::isNeedPrep).reversed()
                    .thenComparing(PrepRecommendation::getName, String.CASE_INSENSITIVE_ORDER));
            return out;
        }
    }

    private static String plain(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
}
