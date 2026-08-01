package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.admin.IngredientDao;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.math.*;
import java.sql.*;
import java.util.*;

/** Truy vấn tồn, ledger, checklist và danh sách mẻ pha. */
public final class InventoryQueryService {
    private final InventoryRepository repository;
    public InventoryQueryService(){this(new InventoryRepository());}
    InventoryQueryService(InventoryRepository repository){this.repository=Objects.requireNonNull(repository);}

    public List<com.cafe.model.PrepBatch> getPrepBatches(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return repository.prepBatchDao.findByBranch(conn, branchId); }
    }

    /** B4 · Mẻ pha HÔM NAY (thay vì toàn bộ lịch sử). */
    public List<com.cafe.model.PrepBatch> getTodayPrepBatches(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return repository.prepBatchDao.findTodayByBranch(conn, branchId); }
    }

    /** F4 · Me ACTIVE da qua han, kem so hao hut de xuat an toan. */
    public List<com.cafe.model.PrepBatch> getExpiredActivePrepBatches(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<com.cafe.model.PrepBatch> batches = repository.prepBatchDao.findExpiredActive(conn, branchId);
            // Phân bổ FIFO trên toàn danh sách: nhiều mẻ cùng nguyên liệu phải chia nhau đúng phần tồn
            // đang có, nếu mỗi mẻ tự lấy min(sản lượng, tồn) thì tổng gợi ý vượt tồn thực.
            ExpiryWasteCalculator.allocateSuggestedWaste(batches);
            return batches;
        }
    }

    /** Mẻ pha hôm nay theo trang — tìm kiếm, bộ lọc và phân trang đều được chạy ở database. */
    public PrepBatchPage getTodayPrepBatchPage(int branchId, String query, int ingredientId,
                                               String expiry, String status, int requestedPage, int pageSize) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            int total = repository.prepBatchDao.countTodayByBranch(conn, branchId, query, ingredientId, expiry, status);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
            int page = Math.max(1, Math.min(requestedPage, totalPages));
            List<com.cafe.model.PrepBatch> batches = repository.prepBatchDao.findTodayPageByBranch(conn, branchId,
                    query, ingredientId, expiry, status, (page - 1) * pageSize, pageSize);
            return new PrepBatchPage(batches, total, page, pageSize);
        }
    }

    public static class PrepBatchPage {
        private final List<com.cafe.model.PrepBatch> batches;
        private final int total;
        private final int page;
        private final int pageSize;

        public PrepBatchPage(List<com.cafe.model.PrepBatch> batches, int total, int page, int pageSize) {
            this.batches = batches;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<com.cafe.model.PrepBatch> getBatches() { return batches; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return Math.max(1, (int) Math.ceil((double) total / pageSize)); }
        public boolean isHasPrevious() { return page > 1; }
        public boolean isHasNext() { return page < getTotalPages(); }
        public int getStartRow() { return total == 0 ? 0 : (page - 1) * pageSize + 1; }
        public int getEndRow() { return Math.min(page * pageSize, total); }

        public List<Integer> getVisiblePages() {
            List<Integer> pages = new ArrayList<>();
            int totalPages = getTotalPages();
            int start = Math.max(1, page - 2);
            int end = Math.min(totalPages, start + 4);
            start = Math.max(1, end - 4);
            for (int value = start; value <= end; value++) pages.add(value);
            return pages;
        }
    }

    /** B4 · Checklist "cần pha": mọi PREPPED của chi nhánh + tồn/ngưỡng + có công thức hay chưa. */
    public List<com.cafe.model.PrepChecklistRow> getPrepChecklist(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<BranchInventory> prepped = new ArrayList<>();
            List<Integer> preppedIds = new ArrayList<>();
            for (BranchInventory bi : repository.biDao.findActiveByBranch(conn, branchId)) {
                if (!"PREPPED".equals(bi.getIngredientType())) continue;
                prepped.add(bi);
                preppedIds.add(bi.getIngredientId());
            }
            // Một truy vấn cho mọi công thức thay vì mỗi nguyên liệu một lượt (N+1 mỗi lần mở màn Prep).
            Map<Integer, List<Recipe>> recipes = repository.prepRecipeDao.findByPreppedIds(conn, preppedIds);
            List<com.cafe.model.PrepChecklistRow> rows = new ArrayList<>();
            for (BranchInventory bi : prepped) {
                List<Recipe> recipe = recipes.get(bi.getIngredientId());
                boolean hasRecipe = recipe != null && !recipe.isEmpty();
                rows.add(new com.cafe.model.PrepChecklistRow(bi.getIngredientId(), bi.getIngredientName(),
                        bi.getIngredientUnit(), bi.getQuantityOnHand(), bi.getMinThreshold(),
                        bi.getPrepTargetQty(), hasRecipe, bi.getIngredientShelfLifeMinutes()));
            }
            return rows;
        }
    }

    /** B4 · Công thức prep của từng PREPPED (cho preview tiêu hao RAW phía client). */
    public Map<Integer, List<Recipe>> getPrepRecipeMap(List<Integer> preppedIds) throws SQLException {
        if (preppedIds == null || preppedIds.isEmpty()) return new java.util.LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection()) {
            return repository.prepRecipeDao.findByPreppedIds(conn, preppedIds);
        }
    }

    public List<BranchInventory> getBranchInventory(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return repository.biDao.findByBranch(conn, branchId); }
    }

    public List<BranchInventory> getLowStock(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return repository.biDao.findLowStock(conn, branchId); }
    }

    public List<BranchInventory> getOversoldStock(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return repository.biDao.findOversold(conn, branchId); }
    }

    public List<InventoryTransaction> getIngredientLedger(int branchId, int ingredientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return repository.txnDao.findByBranchIngredient(conn, branchId, ingredientId); }
    }

    public void setMinThreshold(int branchId, int ingredientId, BigDecimal threshold) throws SQLException {
        if (threshold == null || threshold.signum() < 0)
            throw new BusinessException("Ngưỡng cảnh báo phải lớn hơn hoặc bằng 0.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { repository.biDao.updateThreshold(conn, branchId, ingredientId, threshold); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public void setPrepPolicy(int branchId, int ingredientId, BigDecimal threshold, BigDecimal target)
            throws SQLException {
        if (threshold == null || threshold.signum() < 0)
            throw new BusinessException("Ngưỡng cảnh báo phải lớn hơn hoặc bằng 0.");
        if (target == null || target.compareTo(threshold) <= 0)
            throw new BusinessException("Mức tồn mục tiêu phải lớn hơn ngưỡng cảnh báo.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { repository.biDao.updatePrepPolicy(conn, branchId, ingredientId, threshold, target); conn.commit(); }
            catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public List<com.cafe.model.PrepBatch> getRecentPrepBatches(int branchId, int limit) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.prepBatchDao.findRecentByBranch(conn, branchId, limit);
        }
    }
    public List<com.cafe.model.PrepBatch> getPendingApprovalBatches(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.prepBatchDao.findPendingApproval(conn, branchId);
        }
    }

    public List<com.cafe.model.PrepBatch> getUnreviewedBatches(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.prepBatchDao.findUnreviewedActive(conn, branchId);
        }
    }

    public List<Ingredient> getActiveWasteIngredients(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Ingredient> ingredients = new ArrayList<>();
            for (BranchInventory inventory : repository.biDao.findActiveByBranch(conn, branchId)) {
                Ingredient ingredient = new Ingredient();
                ingredient.setIngredientId(inventory.getIngredientId());
                ingredient.setName(inventory.getIngredientName());
                ingredient.setUnit(inventory.getIngredientUnit());
                ingredient.setIngredientType(inventory.getIngredientType());
                ingredient.setActive(true);
                ingredients.add(ingredient);
            }
            return ingredients;
        }
    }

}
