package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.ModifierGroupDao;
import com.cafe.dao.shared.ModifierIngredientImpactDao;
import com.cafe.dao.shared.ModifierOptionDao;
import com.cafe.dao.shared.PrepRecipeDao;
import com.cafe.dao.shared.ProductModifierGroupDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.ModifierGroup;
import com.cafe.model.ModifierIngredientImpact;
import com.cafe.model.ModifierOption;
import com.cafe.model.PosMenuItem;
import com.cafe.model.PrepRecipe;
import com.cafe.model.ProductModifierGroup;
import com.cafe.model.ProductRecipe;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Đọc menu để dựng màn POS / QR (chỉ món available, chưa 86) + tra cứu công thức (Barista, read-only). */
public class CatalogReadService {

    private final BranchMenuDao branchMenuDao = new BranchMenuDao();
    private final ProductModifierGroupDao pmgDao = new ProductModifierGroupDao();
    private final ModifierGroupDao groupDao = new ModifierGroupDao();
    private final ModifierOptionDao optionDao = new ModifierOptionDao();
    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();
    private final PrepRecipeDao prepRecipeDao = new PrepRecipeDao();
    private final ModifierIngredientImpactDao impactDao = new ModifierIngredientImpactDao();

    /** Menu bán được của chi nhánh: published + available + chưa 86, kèm nhóm modifier. */
    public List<PosMenuItem> getPosMenu(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<PosMenuItem> out = new ArrayList<>();
            for (BranchMenuItem bm : branchMenuDao.listForBranch(conn, branchId)) {
                if (!bm.isPublished() || !bm.isAvailable() || bm.isIs86()) continue;
                PosMenuItem item = new PosMenuItem();
                item.setProductId(bm.getProductId());
                item.setName(bm.getProductName());
                item.setPrice(bm.getLocalPrice() != null ? bm.getLocalPrice() : bm.getBasePrice());

                for (ProductModifierGroup pmg : pmgDao.findByProduct(conn, bm.getProductId())) {
                    ModifierGroup g = groupDao.findById(conn, pmg.getModifierGroupId());
                    if (g == null) continue;
                    PosMenuItem.Group grp = new PosMenuItem.Group();
                    grp.setGroupId(g.getModifierGroupId());
                    grp.setName(g.getName());
                    grp.setRequired(g.isRequired());
                    grp.setMinSelect(g.getMinSelect());
                    grp.setMaxSelect(g.getMaxSelect());
                    for (ModifierOption o : optionDao.findByGroup(conn, g.getModifierGroupId())) {
                        if (o.isActive()) grp.getOptions().add(o);
                    }
                    if (!grp.getOptions().isEmpty()) item.getGroups().add(grp);
                }
                out.add(item);
            }
            return out;
        }
    }

    // ===== B6 · Tra cứu công thức (Barista, read-only) =====

    /** Công thức món: từng dòng nguyên liệu (RAW/PREPPED) + định mức. */
    public List<ProductRecipe> getRecipeForProduct(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productRecipeDao.findByProduct(conn, productId);
        }
    }

    /** Định mức pha sẵn của 1 nguyên liệu PREPPED (RAW → PREPPED + yield). */
    public List<PrepRecipe> getPrepRecipe(int preppedIngredientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return prepRecipeDao.findByPrepped(conn, preppedIngredientId);
        }
    }

    /** Tác động nguyên liệu của các modifier áp cho 1 product (option → ingredient, QtyDelta). */
    public List<OptionImpactRow> getModifierImpactsForProduct(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OptionImpactRow> rows = new ArrayList<>();
            for (ProductModifierGroup pmg : pmgDao.findByProduct(conn, productId)) {
                ModifierGroup g = groupDao.findById(conn, pmg.getModifierGroupId());
                if (g == null) continue;
                for (ModifierOption o : optionDao.findByGroup(conn, g.getModifierGroupId())) {
                    for (ModifierIngredientImpact imp : impactDao.findByOption(conn, o.getModifierOptionId())) {
                        OptionImpactRow r = new OptionImpactRow();
                        r.groupName = g.getName();
                        r.optionName = o.getName();
                        r.ingredientName = imp.getIngredientName();
                        r.ingredientUnit = imp.getIngredientUnit();
                        r.qtyDelta = imp.getQtyDelta();
                        rows.add(r);
                    }
                }
            }
            return rows;
        }
    }

    /** Dòng phẳng cho view tra cứu modifier (EL-friendly). */
    public static class OptionImpactRow {
        private String groupName, optionName, ingredientName, ingredientUnit;
        private BigDecimal qtyDelta;
        public String getGroupName() { return groupName; }
        public String getOptionName() { return optionName; }
        public String getIngredientName() { return ingredientName; }
        public String getIngredientUnit() { return ingredientUnit; }
        public BigDecimal getQtyDelta() { return qtyDelta; }
    }
}
