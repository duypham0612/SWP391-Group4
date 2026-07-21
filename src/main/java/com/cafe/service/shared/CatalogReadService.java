package com.cafe.service.shared;

import com.cafe.config.DBConnection;
import com.cafe.dao.admin.CategoryDao;
import com.cafe.dao.admin.HomeSettingDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.PrepRecipeDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.Category;
import com.cafe.model.HomeSetting;
import com.cafe.model.PosMenuItem;
import com.cafe.model.PrepRecipe;
import com.cafe.model.Product;
import com.cafe.model.ProductRecipe;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Đọc menu để dựng màn POS / QR (chỉ món available, chưa 86) + tra cứu công thức (Barista, read-only). */
public class CatalogReadService {

    private final BranchMenuDao branchMenuDao = new BranchMenuDao();
    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();
    private final PrepRecipeDao prepRecipeDao = new PrepRecipeDao();
    private final ProductDao productDao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final HomeSettingDao homeSettingDao = new HomeSettingDao();

    /** Menu bán được của chi nhánh: published + available + chưa 86 + còn tồn (không hết theo kho). */
    public List<PosMenuItem> getPosMenu(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Hết theo kho = tự động ẩn (bản chất 1): tính từ tồn, không phải cờ 86 thủ công.
            java.util.Set<Integer> depleted = productRecipeDao.findDepletedProductIds(conn, branchId);
            List<PosMenuItem> out = new ArrayList<>();
            for (BranchMenuItem bm : branchMenuDao.listForBranch(conn, branchId)) {
                if (!bm.isPublished() || !bm.isAvailable() || bm.isIs86()
                        || depleted.contains(bm.getProductId())) continue;
                PosMenuItem item = new PosMenuItem();
                item.setProductId(bm.getProductId());
                item.setName(bm.getProductName());
                item.setImageUrl(bm.getImageUrl());
                item.setPrice(bm.getLocalPrice() != null ? bm.getLocalPrice() : bm.getBasePrice());
                item.setSizeEnabled(bm.isSizeEnabled());
                item.setSizeSDelta(bm.getSizeSDelta());
                item.setSizeMDelta(bm.getSizeMDelta());
                item.setSizeLDelta(bm.getSizeLDelta());
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

}
