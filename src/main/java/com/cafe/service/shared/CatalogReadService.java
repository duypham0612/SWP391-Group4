package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.CategoryDao;
import com.cafe.dao.admin.HomeSettingDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.ModifierGroupDao;
import com.cafe.dao.shared.ModifierIngredientImpactDao;
import com.cafe.dao.shared.ModifierOptionDao;
import com.cafe.dao.shared.PrepRecipeDao;
import com.cafe.dao.shared.ProductModifierGroupDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.Category;
import com.cafe.model.HomeSetting;
import com.cafe.model.ModifierGroup;
import com.cafe.model.ModifierIngredientImpact;
import com.cafe.model.ModifierOption;
import com.cafe.model.PosMenuItem;
import com.cafe.model.PrepRecipe;
import com.cafe.model.Product;
import com.cafe.model.ProductModifierGroup;
import com.cafe.model.ProductRecipe;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Đọc menu để dựng màn POS / QR (chỉ món available, chưa 86) + tra cứu công thức (Barista, read-only). */
public class CatalogReadService {
    private static final String GROUP_SIZE = "Size";
    private static final String GROUP_SUGAR = "\u0110\u01b0\u1eddng";
    private static final String GROUP_ICE = "\u0110\u00e1";

    private final BranchMenuDao branchMenuDao = new BranchMenuDao();
    private final ProductModifierGroupDao pmgDao = new ProductModifierGroupDao();
    private final ModifierGroupDao groupDao = new ModifierGroupDao();
    private final ModifierOptionDao optionDao = new ModifierOptionDao();
    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();
    private final PrepRecipeDao prepRecipeDao = new PrepRecipeDao();
    private final ModifierIngredientImpactDao impactDao = new ModifierIngredientImpactDao();
    private final ProductDao productDao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final HomeSettingDao homeSettingDao = new HomeSettingDao();

    /** Menu bán được của chi nhánh: published + available + chưa 86, kèm nhóm modifier. */
    public List<PosMenuItem> getPosMenu(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<PosMenuItem> out = new ArrayList<>();
            for (BranchMenuItem bm : branchMenuDao.listForBranch(conn, branchId)) {
                if (!bm.isPublished() || !bm.isAvailable() || bm.isIs86()) continue;
                PosMenuItem item = new PosMenuItem();
                item.setProductId(bm.getProductId());
                item.setName(bm.getProductName());
                item.setImageUrl(bm.getImageUrl());
                item.setPrice(bm.getLocalPrice() != null ? bm.getLocalPrice() : bm.getBasePrice());

                for (ProductModifierGroup pmg : pmgDao.findByProduct(conn, bm.getProductId())) {
                    ModifierGroup g = groupDao.findById(conn, pmg.getModifierGroupId());
                    if (g == null) continue;
                    if (!isCustomerChoiceGroup(g.getName())) continue;
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

    private boolean isCustomerChoiceGroup(String name) {
        return GROUP_SIZE.equals(name) || GROUP_SUGAR.equals(name) || GROUP_ICE.equals(name);
    }

    // ===== Trang Home công khai: catalog theo danh mục (khách xem, không cần login) =====

    /**
     * Menu công khai: các danh mục (theo SortOrder) + sản phẩm Admin chọn hiển thị
     * (IsActive + ShowOnHome), trong mỗi danh mục sắp theo HomeSortOrder rồi tên.
     */
    public List<MenuSection> getPublicMenu() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            java.util.LinkedHashMap<Integer, MenuSection> byCat = new java.util.LinkedHashMap<>();
            for (Product p : productDao.findForHome(conn)) {  // đã lọc Active+ShowOnHome, ORDER BY SortOrder, HomeSortOrder, Name
                MenuSection s = byCat.get(p.getCategoryId());
                if (s == null) { s = new MenuSection(); s.name = p.getCategoryName(); byCat.put(p.getCategoryId(), s); }
                s.products.add(p);
            }
            return new ArrayList<>(byCat.values());
        }
    }

    /** Nội dung hero của trang Home (tiêu đề/mô tả/ảnh) do Admin cấu hình; null nếu chưa cấu hình. */
    public HomeSetting getHomeSetting() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return homeSettingDao.find(conn);
        }
    }

    /** Một nhóm trên trang Home: tên danh mục + danh sách sản phẩm. */
    public static class MenuSection {
        private String name;
        private final List<Product> products = new ArrayList<>();
        public String getName() { return name; }
        public List<Product> getProducts() { return products; }
        public int getCount() { return products.size(); }
    }

    // ===== B6 · Tra cứu công thức (Barista, read-only) =====

    public ProductPage getRecipeProductPage(String q, Integer categoryId, String recipeState,
                                            Integer branchId, int page, int pageSize) throws SQLException {
        int safePageSize = Math.max(1, pageSize);
        try (Connection conn = DBConnection.getConnection()) {
            int total = productDao.countForRecipeLookup(conn, q, categoryId, recipeState, branchId);
            int totalPages = Math.max(1, (int) Math.ceil(total / (double) safePageSize));
            int safePage = Math.min(Math.max(1, page), totalPages);
            int offset = (safePage - 1) * safePageSize;
            List<Product> items = productDao.findForRecipeLookup(
                    conn, q, categoryId, recipeState, branchId, offset, safePageSize);
            return new ProductPage(items, total, safePage, totalPages);
        }
    }

    public Product getRecipeProduct(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productDao.findById(conn, productId);
        }
    }

    public List<Category> getRecipeFilterCategories() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return categoryDao.findActive(conn);
        }
    }

    public static class ProductPage {
        private final List<Product> items;
        private final int total;
        private final int page;
        private final int totalPages;

        public ProductPage(List<Product> items, int total, int page, int totalPages) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.totalPages = totalPages;
        }

        public List<Product> getItems() { return items; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getTotalPages() { return totalPages; }
    }

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
