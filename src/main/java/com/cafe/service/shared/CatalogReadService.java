package com.cafe.service.shared;

import com.cafe.common.ModifierGroupNames;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.CategoryDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.dao.admin.BranchMenuDao;
import com.cafe.dao.admin.BranchDao;
import com.cafe.dao.admin.ModifierGroupDao;
import com.cafe.dao.admin.ModifierOptionDao;
import com.cafe.dao.admin.ProductModifierGroupDao;
import com.cafe.dao.admin.RecipeDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.Category;
import com.cafe.model.Branch;
import com.cafe.model.ModifierGroup;
import com.cafe.model.ModifierOption;
import com.cafe.model.PosMenuItem;
import com.cafe.model.ProductStockStatus;
import com.cafe.model.Product;
import com.cafe.model.ProductModifierGroup;
import com.cafe.model.Recipe;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Đọc menu để dựng màn POS / QR (chỉ món available, chưa 86) + tra cứu công thức (Barista, read-only). */
public class CatalogReadService {
    private final BranchMenuDao branchMenuDao;
    private final ProductModifierGroupDao pmgDao;
    private final ModifierGroupDao groupDao;
    private final ModifierOptionDao optionDao;
    private final RecipeDao recipeDao;
    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final BranchDao branchDao;

    public CatalogReadService() {
        this(new BranchMenuDao(), new ProductModifierGroupDao(), new ModifierGroupDao(),
                new ModifierOptionDao(), new RecipeDao(), new ProductDao(), new CategoryDao(), new BranchDao());
    }
    public CatalogReadService(BranchMenuDao branchMenuDao, ProductModifierGroupDao pmgDao,
                              ModifierGroupDao groupDao, ModifierOptionDao optionDao,
                              RecipeDao recipeDao, ProductDao productDao,
                              CategoryDao categoryDao, BranchDao branchDao) {
        this.branchMenuDao = java.util.Objects.requireNonNull(branchMenuDao);
        this.pmgDao = java.util.Objects.requireNonNull(pmgDao);
        this.groupDao = java.util.Objects.requireNonNull(groupDao);
        this.optionDao = java.util.Objects.requireNonNull(optionDao);
        this.recipeDao = java.util.Objects.requireNonNull(recipeDao);
        this.productDao = java.util.Objects.requireNonNull(productDao);
        this.categoryDao = java.util.Objects.requireNonNull(categoryDao);
        this.branchDao = java.util.Objects.requireNonNull(branchDao);
    }

    /**
     * Menu của chi nhánh. Món OUT/86 vẫn được trả về để Cashier/khách biết lý do,
     * nhưng được đánh dấu orderable=false; món Manager ngừng bán vẫn ẩn.
     */
    public List<PosMenuItem> getPosMenu(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            java.util.Map<Integer, ProductStockStatus> stockByProduct =
                    recipeDao.findProductStockStatuses(conn, branchId);
            List<PosMenuItem> out = new ArrayList<>();
            for (BranchMenuItem bm : branchMenuDao.listForBranch(conn, branchId)) {
                if (!bm.isPublished() || !bm.isListed()) continue;
                PosMenuItem item = new PosMenuItem();
                item.setProductId(bm.getProductId());
                item.setName(bm.getProductName());
                item.setImageUrl(bm.getImageUrl());
                item.setPrice(bm.getLocalPrice() != null ? bm.getLocalPrice() : bm.getBasePrice());

                ProductStockStatus stock = stockByProduct.get(bm.getProductId());
                if (bm.isTemporarilyUnavailable()) {
                    item.setAvailabilityState("EIGHTY_SIX");
                    item.setOrderable(false);
                } else if (stock != null) {
                    item.setAvailabilityState(stock.getState());
                    item.setLowIngredients(stock.getLowIngredients());
                    item.setOutIngredients(stock.getOutIngredients());
                    item.setOrderable(!stock.isOut());
                }

                if (item.isOrderable()) {
                    for (ProductModifierGroup pmg : pmgDao.findByProduct(conn, bm.getProductId())) {
                        ModifierGroup g = groupDao.findById(conn, pmg.getModifierGroupId());
                        if (g == null || !isChoiceGroup(g.getName())) continue;
                        PosMenuItem.Group grp = new PosMenuItem.Group();
                        grp.setGroupId(g.getModifierGroupId());
                        grp.setName(ModifierGroupNames.display(g.getName()));
                        grp.setRequired(g.isRequired());
                        grp.setMinSelect(g.getMinSelect());
                        grp.setMaxSelect(g.getMaxSelect());
                        for (ModifierOption o : optionDao.findByGroup(conn, g.getModifierGroupId())) {
                            if (o.isActive()) grp.getOptions().add(o);
                        }
                        if (!grp.getOptions().isEmpty()) item.getGroups().add(grp);
                    }
                }
                out.add(item);
            }
            return out;
        }
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
    public Branch getHomeBranch(Integer branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            if (branchId != null && branchId > 0) {
                Branch selected = branchDao.findActiveById(conn, branchId);
                if (selected != null) return selected;
            }
            return branchDao.findFirstActive(conn);
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

    /** Chi tiết món trong đúng phạm vi tìm kiếm công thức của Barista. */
    public Product getRecipeProductForLookup(int productId, String q, Integer categoryId,
                                             String recipeState, Integer branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productDao.findForRecipeLookupById(
                    conn, productId, q, categoryId, recipeState, branchId);
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
    public List<Recipe> getRecipeForProduct(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return recipeDao.findByProduct(conn, productId);
        }
    }

    /** Định mức pha sẵn của 1 nguyên liệu PREPPED (RAW → PREPPED + yield). */
    public List<Recipe> getPrepRecipe(int preppedIngredientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return recipeDao.findByPrepped(conn, preppedIngredientId);
        }
    }

    /** Tác động nguyên liệu của các modifier áp cho 1 product (option → ingredient, QtyDelta). */
    public List<OptionImpactRow> getModifierImpactsForProduct(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OptionImpactRow> rows = new ArrayList<>();
            for (ProductModifierGroup pmg : pmgDao.findByProduct(conn, productId)) {
                ModifierGroup g = groupDao.findById(conn, pmg.getModifierGroupId());
                if (g == null || !isChoiceGroup(g.getName())) continue;
                for (ModifierOption o : optionDao.findByGroup(conn, g.getModifierGroupId())) {
                    // Barista chỉ cần định mức của lựa chọn còn có thể bán trên POS.
                    if (!o.isActive()) continue;
                    for (Recipe imp : recipeDao.findByOption(conn, o.getModifierOptionId())) {
                        OptionImpactRow r = new OptionImpactRow();
                        r.groupName = ModifierGroupNames.display(g.getName());
                        r.optionName = o.getName();
                        r.ingredientId = imp.getIngredientId();
                        r.ingredientName = imp.getIngredientName();
                        r.ingredientUnit = imp.getIngredientUnit();
                        r.qtyDelta = imp.getQuantity();
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
        private int ingredientId;
        private BigDecimal qtyDelta;
        /** Nguyên liệu này có trong định mức chuẩn của món không — do caller gán, xem setter. */
        private boolean inBaseRecipe;

        public String getGroupName() { return groupName; }
        public String getOptionName() { return optionName; }
        public int getIngredientId() { return ingredientId; }
        public String getIngredientName() { return ingredientName; }
        public String getIngredientUnit() { return ingredientUnit; }
        public BigDecimal getQtyDelta() { return qtyDelta; }

        public boolean isInBaseRecipe() { return inBaseRecipe; }

        /**
         * Modifier có thể trỏ tới nguyên liệu không nằm trong định mức chuẩn (vd. Cà phê sữa
         * không có Đá/Đường trong công thức nhưng vẫn có option "Ít đá"). Caller đã đọc sẵn
         * định mức nên gán cờ từ ngoài, tránh truy vấn lại trong service.
         */
        public void setInBaseRecipe(boolean inBaseRecipe) { this.inBaseRecipe = inBaseRecipe; }

    }

    private static boolean isChoiceGroup(String name) {
        return ModifierGroupNames.isStandardChoice(name);
    }
}
