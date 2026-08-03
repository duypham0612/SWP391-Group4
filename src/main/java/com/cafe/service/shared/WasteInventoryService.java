package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.model.*;

import java.math.*;
import java.sql.*;
import java.util.*;

/** Workflow hao hụt, audit/review và truy vấn chi phí hao hụt. */
public final class WasteInventoryService {
    private static final BigDecimal MAX_WASTE_QUANTITY=new BigDecimal("999999999.999");
    private final InventoryRepository repository; private final InventoryLedgerService ledgerService;
    public WasteInventoryService(){this(new InventoryRepository());}
    WasteInventoryService(InventoryRepository repository){this(repository,new InventoryLedgerService(repository));}
    WasteInventoryService(InventoryRepository repository,InventoryLedgerService ledgerService){this.repository=Objects.requireNonNull(repository);this.ledgerService=Objects.requireNonNull(ledgerService);}

    public long logWaste(int branchId, int ingredientId, BigDecimal qty, String wasteType, String reason, int userId) throws SQLException {
        requireIngredientWasteType(wasteType);
        requireWasteQuantity(qty);
        requireReason(reason);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                WasteEvent event = newEvent(branchId, "INGREDIENT_WASTE", "MANUAL", null, null, null,
                        causeFromWasteType(wasteType), reason, userId, UUID.randomUUID().toString());
                long id = logWasteInTx(conn, branchId, ingredientId, qty, wasteType, reason, userId, event);
                conn.commit();
                return id;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Ghi nhiều dòng hao hụt nguyên liệu trong một transaction. */
    public int logWasteLines(int branchId, List<WasteLogLine> lines, int userId) throws SQLException {
        return logWasteLines(branchId, lines, userId, null);
    }

    /** Client request id giúp retry POST không nhân đôi hao hụt. */
    public int logWasteLines(int branchId, List<WasteLogLine> lines, int userId, String requestId) throws SQLException {
        if (lines == null || lines.isEmpty()) throw new BusinessException("Chưa có dòng hao hụt nào để ghi.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int count = 0;
                for (WasteLogLine line : lines) {
                    if (line == null) throw new BusinessException("Dòng hao hụt không hợp lệ.");
                    requireIngredientWasteType(line.getWasteType());
                    requireWasteQuantity(line.getQuantity());
                    requireReason(line.getReason());
                    String eventGroupId = requestId == null || requestId.isBlank()
                            ? UUID.randomUUID().toString() : requestId + "-" + count;
                    if (repository.wasteEventDao.existsGroup(conn, branchId, eventGroupId)) {
                        conn.rollback(); return 0;
                    }
                    WasteEvent event = newEvent(branchId, "INGREDIENT_WASTE", "MANUAL", null, null, null,
                            causeForWasteLine(line), line.getReason(), userId, eventGroupId);
                    logWasteInTx(conn, branchId, line.getIngredientId(), line.getQuantity(),
                            normalizeWasteType(line.getWasteType()), cleanReason(line.getReason()), userId, event);
                    count++;
                }
                conn.commit();
                return count;
            } catch (SQLException e) {
                conn.rollback();
                // Unique index là chốt cuối khi hai POST cùng request-id chạy song song.
                if (requestId != null && !requestId.isBlank() && isDuplicateClientRequest(e)) return 0;
                throw e;
            }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Sửa dòng hao hụt (Contract #4) — áp TXN cho phần chênh lệch số lượng (delta = new − old).
     * delta>0: trừ thêm; delta<0: hoàn lại. Cập nhật WasteEventItem. Own tx.
     */
    public void updateWaste(int branchId, long wasteEntryId, BigDecimal newQty, String wasteType, String reason, int userId) throws SQLException {
        requireWasteQuantity(newQty);
        requireIngredientWasteType(wasteType);
        requireReason(reason);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.WasteEventItem w = repository.wasteEventItemDao.findByIdForBranch(conn, wasteEntryId, branchId);
                if (w == null) throw new BusinessException("Bản ghi hao hụt không còn khả dụng. Vui lòng tải lại.");
                requireBaristaCorrectionWindow(w, userId);
                if (!w.isActive()) throw new BusinessException("Bản ghi đã huỷ — không sửa được.");
                if (w.isRemake()) throw new BusinessException("Dòng làm lại món không sửa lẻ; hãy huỷ rồi ghi lại nếu cần.");
                BigDecimal delta = newQty.subtract(w.getQuantity());
                if (delta.signum() != 0) {
                    BigDecimal[] beforeState = repository.biDao.findQtyAndThreshold(conn, branchId, w.getIngredientId());
                    BigDecimal before = beforeState == null || beforeState[0] == null ? BigDecimal.ZERO : beforeState[0];
                    ledgerService.applyTxn(conn, branchId, w.getIngredientId(), delta.negate(),  // delta>0 trừ thêm tồn
                            TxnType.WASTE, InventoryReferenceType.WASTE_ENTRY,
                            wasteEntryId, userId);
                    // Sửa tăng có thể đẩy tồn xuống âm y như lúc ghi mới — Quản lý phải thấy được ngoại lệ đó.
                    flagNegativeStock(conn, branchId, w.getIngredientId(), wasteEntryId,
                            before, before.subtract(delta), reason);
                }
                if (repository.wasteEventItemDao.updateForBranch(conn, wasteEntryId, branchId, newQty, normalizeWasteType(wasteType),
                        cleanReason(reason), w.getQuantity()) != 1) {
                    throw new BusinessException("Bản ghi hao hụt đã được thay đổi bởi thao tác khác. Vui lòng tải lại.");
                }
                if (w.getEventGroupId() != null) {
                    repository.wasteEventDao.updateCause(conn, branchId, w.getEventGroupId(),
                            causeFromWasteType(wasteType), cleanReason(reason));
                }
            repository.activityLogDao.insertWasteEntry(conn, wasteEntryId, branchId, "UPDATE", w.getQuantity().toPlainString(),
                        newQty.toPlainString(), cleanReason(reason), userId);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Huỷ dòng hao hụt (Contract #4) — HOÀN KHO BẰNG TXN BÙ (+qty WASTE), đánh dấu VOIDED.
     * KHÔNG hard-delete, KHÔNG UPDATE thẳng tồn. Own tx.
     */
    public void voidWaste(int branchId, long wasteEntryId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.WasteEventItem w = repository.wasteEventItemDao.findByIdForBranch(conn, wasteEntryId, branchId);
                if (w == null) throw new BusinessException("Bản ghi hao hụt không còn khả dụng. Vui lòng tải lại.");
                requireBaristaCorrectionWindow(w, userId);
                if (!w.isActive()) { conn.rollback(); return; }   // idempotent
                if (w.isRemake()) {
                    throw new BusinessException("Dòng làm lại món gắn với ly đã pha nên không huỷ lẻ được. Nếu tồn kho sai, báo Quản lý kiểm kê lại.");
                }
                ledgerService.applyTxn(conn, branchId, w.getIngredientId(), w.getQuantity(),  // hoàn lại tồn (+)
                        TxnType.WASTE, InventoryReferenceType.WASTE_ENTRY,
                        wasteEntryId, userId);
                if (repository.wasteEventItemDao.updateStatusForBranch(conn, wasteEntryId, branchId, "VOIDED") != 1) {
                    throw new BusinessException("Bản ghi hao hụt đã được thay đổi bởi thao tác khác. Vui lòng tải lại.");
                }
            repository.activityLogDao.insertWasteEntry(conn, wasteEntryId, branchId, "VOID", w.getQuantity().toPlainString(),
                        null, null, userId);
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    long logWasteInTx(Connection conn, int branchId, int ingredientId, BigDecimal qty,
                             String wasteType, String reason, int userId) throws SQLException {
        WasteEvent event = newEvent(branchId, "INGREDIENT_WASTE", "MANUAL", null, null, null,
                causeFromWasteType(wasteType), reason, userId, UUID.randomUUID().toString());
        return logWasteInTx(conn, branchId, ingredientId, qty, wasteType, reason, userId, event);
    }

    long logWasteInTx(Connection conn, int branchId, int ingredientId, BigDecimal qty,
                             String wasteType, String reason, int userId, WasteEvent event) throws SQLException {
        requireWasteQuantity(qty);
        if (ingredientId <= 0 || !repository.biDao.isActiveConfiguredIngredient(conn, branchId, ingredientId)) {
            throw new BusinessException("Nguyên liệu không còn hoạt động hoặc chưa được cấu hình tồn tại chi nhánh này.");
        }
        BigDecimal[] beforeState = repository.biDao.findQtyAndThreshold(conn, branchId, ingredientId);
        BigDecimal before = beforeState == null || beforeState[0] == null ? BigDecimal.ZERO : beforeState[0];
        BigDecimal snapshot = estimateUnitCost(conn, branchId, ingredientId, new HashSet<>());
        String costBasis = snapshot == null ? "UNAVAILABLE" : "SNAPSHOT";
        long id = repository.wasteEventItemDao.insert(conn, event, ingredientId, qty,
                normalizeWasteType(wasteType), cleanReason(reason), userId, snapshot, costBasis);
        ledgerService.applyTxn(conn, branchId, ingredientId, qty.negate(), TxnType.WASTE,
                InventoryReferenceType.WASTE_ENTRY, id, userId);
        flagNegativeStock(conn, branchId, ingredientId, id, before, before.subtract(qty), reason);
        repository.activityLogDao.insertWasteEntry(conn, id, branchId, "CREATE", null, qty.toPlainString(), cleanReason(reason), userId);
        return id;
    }

    /**
     * Tồn xuống âm sau khi ghi/sửa hao hụt thì đẩy vào hàng đợi đối soát của Quản lý.
     * SOFT khi phần âm còn trong ngưỡng cảnh báo, HARD khi vượt — Quản lý ưu tiên xử lý HARD trước.
     */
    private void flagNegativeStock(Connection conn, int branchId, int ingredientId, long wasteEntryId,
                                   BigDecimal before, BigDecimal fallbackAfter, String reason) throws SQLException {
        BigDecimal[] state = repository.biDao.findQtyAndThreshold(conn, branchId, ingredientId);
        BigDecimal after = state == null || state[0] == null ? fallbackAfter : state[0];
        if (after == null || after.signum() >= 0) return;
        BigDecimal threshold = state == null || state[1] == null ? BigDecimal.ZERO : state[1].abs();
        String reviewType = after.abs().compareTo(threshold) <= 0 ? "SOFT_NEGATIVE" : "HARD_NEGATIVE";
        repository.wasteEventReviewDao.open(conn, wasteEntryId, reviewType, before, after, cleanReason(reason));
    }

    private static WasteEvent newEvent(int branchId, String kind, String source, Integer productId,
                             Integer orderItemId, Integer cupQty, String cause, String detail, int userId,
                             String eventGroupId) {
        WasteEvent e = new WasteEvent(); e.setBranchId(branchId); e.setEventKind(kind); e.setSource(source);
        e.setProductId(productId); e.setOrderItemId(orderItemId); e.setCupQuantity(cupQty);
        e.setCauseCode(normalizeCause(cause)); e.setCauseDetail(cleanReason(detail)); e.setCreatedBy(userId);
        e.setEventGroupId(eventGroupId); e.setCreatedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        return e;
    }

    private static String causeFromWasteType(String wasteType) {
        String type = normalizeWasteType(wasteType);
        if ("SPILL".equals(type)) return "SPILL";
        if ("EXPIRED".equals(type)) return "EXPIRED";
        return "OTHER";
    }
    private static String causeForWasteLine(WasteLogLine line) {
        String supplied = line.getCauseCode();
        return supplied == null || supplied.isBlank() ? causeFromWasteType(line.getWasteType()) : requireCauseCode(supplied);
    }
    private static String normalizeCause(String cause) {
        if (cause == null) return "OTHER";
        String normalized = cause.trim().toUpperCase(java.util.Locale.ROOT);
        return java.util.Set.of("SPILL","WRONG_RECIPE","QUALITY","CUSTOMER_FEEDBACK","EXPIRED","STORAGE","EQUIPMENT","QC_SAMPLE","OTHER").contains(normalized)
                ? normalized : "OTHER";
    }
    private static String requireCauseCode(String cause) {
        String normalized = normalizeCause(cause);
        if (cause == null || !normalized.equals(cause.trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new BusinessException("Mã nguyên nhân không hợp lệ.");
        }
        return normalized;
    }
    private static String causeFromReason(String reason) {
        String value = reason == null ? "" : reason.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("sai công thức")) return "WRONG_RECIPE";
        if (value.contains("đổ") || value.contains("hư món")) return "SPILL";
        if (value.contains("chất lượng")) return "QUALITY";
        if (value.contains("khách")) return "CUSTOMER_FEEDBACK";
        return "OTHER";
    }

    private static boolean isDuplicateClientRequest(SQLException error) {
        for (SQLException current = error; current != null; current = current.getNextException()) {
            if (current.getErrorCode() == 2601 || current.getErrorCode() == 2627) return true;
        }
        return false;
    }

    static void requireWasteQuantity(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0 || qty.compareTo(MAX_WASTE_QUANTITY) > 0
                || qty.stripTrailingZeros().scale() > 3) {
            throw new BusinessException("Số lượng phải lớn hơn 0, tối đa 999999999.999 và có không quá 3 chữ số thập phân.");
        }
    }

    private static void requireReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) throw new BusinessException("Vui lòng nhập lý do.");
        if (reason.trim().length() > 255) throw new BusinessException("Lý do tối đa 255 ký tự.");
    }

    private static void requireIngredientWasteType(String wasteType) {
        String type = wasteType == null ? "" : wasteType.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"SPILL".equals(type) && !"EXPIRED".equals(type) && !"OTHER".equals(type)) {
            throw new BusinessException("Hao hụt nguyên liệu chỉ gồm Đổ/rơi, Hết hạn hoặc Khác. Làm lại món ghi tự động từ màn KDS.");
        }
    }

    private static String normalizeWasteType(String wasteType) {
        if (wasteType == null || wasteType.isBlank()) return "OTHER";
        String type = wasteType.trim().toUpperCase(java.util.Locale.ROOT);
        if ("SPILL".equals(type) || "EXPIRED".equals(type) || "REMAKE".equals(type) || "OTHER".equals(type)) return type;
        return "OTHER";
    }

    private static String cleanReason(String reason) {
        if (reason == null) return null;
        String value = reason.trim();
        if (value.isEmpty()) return null;
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    /** Barista chỉ đính chính chính bản ghi mình tạo trong 15 phút; Manager có luồng review riêng. */
    private static void requireBaristaCorrectionWindow(WasteEventItem w, int userId) {
        if (w.getLoggedBy() != userId) throw new BusinessException("Bạn chỉ được sửa bản ghi do chính mình tạo.");
        if (w.getLoggedAt() == null || w.getLoggedAt().isBefore(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(15)))
            throw new BusinessException("Bản ghi đã quá 15 phút, hãy gửi Quản lý đối soát.");
    }

    public List<com.cafe.model.WasteEventItem> getWasteLogs(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<WasteEventItem> logs = repository.wasteEventItemDao.findByBranch(conn, branchId);
            enrichWasteCosts(conn, branchId, logs);
            return logs;
        }
    }

    public List<com.cafe.model.WasteEventItem> getWasteLogs(int branchId, java.time.LocalDateTime fromUtc,
                                                       java.time.LocalDateTime toUtc) throws SQLException {
        return getWasteLogs(branchId, fromUtc, toUtc, false);
    }

    /**
     * {@code ingredientOnly}: chỉ hao hụt nguyên liệu, bỏ dòng sinh từ làm lại món.
     * Quầy pha chế xem đúng phần mình ghi; đối soát của Quản lý vẫn lấy đủ cả hai loại.
     */
    public List<com.cafe.model.WasteEventItem> getWasteLogs(int branchId, java.time.LocalDateTime fromUtc,
                                                       java.time.LocalDateTime toUtc, boolean ingredientOnly) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<WasteEventItem> logs = repository.wasteEventItemDao.findByBranchBetween(conn, branchId, fromUtc, toUtc, ingredientOnly);
            enrichWasteCosts(conn, branchId, logs);
            return logs;
        }
    }

    /** Nhật ký hao hụt theo trang — điều kiện tìm/lọc và OFFSET/FETCH đều được xử lý tại database. */
    public WasteLogPage getWasteLogPage(int branchId, java.time.LocalDateTime fromUtc, java.time.LocalDateTime toUtc,
                                        String query, String wasteType, String status, int requestedPage, int pageSize) throws SQLException {
        return getWasteLogPage(branchId, fromUtc, toUtc, query, wasteType, status, false, requestedPage, pageSize);
    }

    public WasteLogPage getWasteLogPage(int branchId, java.time.LocalDateTime fromUtc, java.time.LocalDateTime toUtc,
                                        String query, String wasteType, String status, boolean ingredientOnly,
                                        int requestedPage, int pageSize) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            int total = repository.wasteEventItemDao.countByBranchBetween(conn, branchId, fromUtc, toUtc, query, wasteType, status, ingredientOnly);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
            int page = Math.max(1, Math.min(requestedPage, totalPages));
            List<WasteEventItem> logs = repository.wasteEventItemDao.findPageByBranchBetween(conn, branchId, fromUtc, toUtc,
                    query, wasteType, status, ingredientOnly, (page - 1) * pageSize, pageSize);
            enrichWasteCosts(conn, branchId, logs);
            return new WasteLogPage(logs, total, page, pageSize);
        }
    }

    public static class WasteLogPage {
        private final List<WasteEventItem> logs;
        private final int total;
        private final int page;
        private final int pageSize;

        public WasteLogPage(List<WasteEventItem> logs, int total, int page, int pageSize) {
            this.logs = logs;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<WasteEventItem> getLogs() { return logs; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return Math.max(1, (int) Math.ceil((double) total / pageSize)); }
        public boolean isHasPrevious() { return page > 1; }
        public boolean isHasNext() { return page < getTotalPages(); }
        public int getStartRow() { return total == 0 ? 0 : (page - 1) * pageSize + 1; }
        public int getEndRow() { return Math.min(page * pageSize, total); }

        /** Tối đa 5 số trang quanh trang hiện tại để pager không phình khi lịch sử dài. */
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

    public WasteEventItem getWasteLog(int branchId, long wasteEntryId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            WasteEventItem log = repository.wasteEventItemDao.findById(conn, wasteEntryId);
            if (log == null || log.getBranchId() != branchId) return null;
            enrichWasteCosts(conn, branchId, List.of(log));
            return log;
        }
    }

    public List<com.cafe.model.WasteEventReview> getOpenWasteReviews(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return repository.wasteEventReviewDao.findOpenByBranch(conn, branchId); }
    }

    /** Nhật ký đính chính hao hụt (sửa/huỷ) trong khoảng ngày — dữ liệu truy vết cho Quản lý. */
    public List<com.cafe.model.WasteEventAudit> getWasteCorrections(int branchId, java.time.LocalDateTime fromUtc,
                                                                    java.time.LocalDateTime toUtc, int limit) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.activityLogDao.findWasteCorrectionsByBranchBetween(conn, branchId, fromUtc, toUtc, limit);
        }
    }

    public boolean resolveWasteReview(int branchId, long reviewId, int managerId, String note) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String cleanedNote = cleanReason(note);
                Long wasteEntryId = repository.wasteEventReviewDao.resolveReturningEntryId(
                        conn, branchId, reviewId, managerId, cleanedNote);
                if (wasteEntryId != null) {
            repository.activityLogDao.insertWasteEntry(conn, wasteEntryId, branchId, "REVIEW", null,
                            "RESOLVED", cleanedNote, managerId);
                }
                conn.commit(); return wasteEntryId != null;
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    public BigDecimal estimateUnitCost(int branchId, int ingredientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return estimateUnitCost(conn, branchId, ingredientId, new HashSet<>());
        }
    }

    private void enrichWasteCosts(Connection conn, int branchId, List<WasteEventItem> logs) throws SQLException {
        if (logs == null || logs.isEmpty()) return;
        for (WasteEventItem log : logs) {
            log.setUnitCost(estimateUnitCost(conn, branchId, log.getIngredientId(), new HashSet<>()));
        }
    }

    private BigDecimal estimateUnitCost(Connection conn, int branchId, int ingredientId, Set<Integer> visiting) throws SQLException {
        BigDecimal direct = repository.detailDao.findLatestUnitCost(conn, branchId, ingredientId);
        if (direct != null) return direct;

        if (!visiting.add(ingredientId)) return null;
        List<Recipe> recipe = repository.prepRecipeDao.findByPrepped(conn, ingredientId);
        Ingredient prepped = repository.ingredientDao.findById(conn, ingredientId);
        if (recipe.isEmpty() || prepped == null || prepped.getPrepYieldQty() == null) {
            visiting.remove(ingredientId);
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Recipe line : recipe) {
            BigDecimal rawCost = estimateUnitCost(conn, branchId, line.getIngredientId(), visiting);
            if (rawCost == null) {
                visiting.remove(ingredientId);
                return null;
            }
            BigDecimal rawPerUnit = line.getQuantity().divide(
                    prepped.getPrepYieldQty(), 6, RoundingMode.HALF_UP);
            total = total.add(rawPerUnit.multiply(rawCost));
        }
        visiting.remove(ingredientId);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** Nhập kho (Manager) — cộng tồn theo từng dòng phiếu, trả về tổng tiền. Chạy trong tx của caller. */

    /**
     * Ghi hao hụt đúng một lượt pha khi món quay về hàng chờ để làm lại (dòng REMAKE/WASTE).
     * Dòng này tính cho lượt VỪA BỎ hay giữ chỗ cho lượt KẾ TIẾP là do caller quyết định khi ghi cờ
     * RemakeInventoryReserved — quy tắc ở {@link com.cafe.common.RemakeReservation}.
     */
    public void reserveRemakeForOrderItem(Connection conn, int branchId, int orderItemId, int productId,
                                          int quantity, String reason, int userId) throws SQLException {
        List<Recipe> recipe = repository.productRecipeDao.findByProduct(conn, productId);
        if (recipe.isEmpty()) throw new BusinessException("Món chưa có công thức — không thể ghi nhận hao hụt làm lại.");
        List<Recipe> impacts = new ArrayList<>();
        for (Integer optionId : repository.oimDao.findOptionIds(conn, orderItemId)) {
            impacts.addAll(repository.impactDao.findByOption(conn, optionId));
        }
        Map<Integer, BigDecimal> required = DeductionCalculator.computeRequired(recipe, impacts, quantity);
        if (required.isEmpty()) throw new BusinessException("Công thức không có lượng nguyên liệu hợp lệ.");
        String note = cleanReason("Làm lại dòng món #" + orderItemId + (reason == null ? "" : " - " + reason));
        WasteEvent event = newEvent(branchId, "REMAKE", "KDS", productId, orderItemId, quantity,
                causeFromReason(reason), note, userId, UUID.randomUUID().toString());
        for (Map.Entry<Integer, BigDecimal> entry : required.entrySet()) {
            logWasteInTx(conn, branchId, entry.getKey(), entry.getValue(), "REMAKE", note, userId, event);
        }
    }

    /**
     * Món bị HUỶ trong lúc đang giữ chỗ nguyên liệu cho lượt pha kế tiếp (RemakeInventoryReserved=1):
     * lượt đó sẽ không bao giờ được pha, nên dòng WASTE giữ chỗ phải hoàn về kho — nếu bỏ qua thì sổ
     * ghi thừa đúng một lượt so với lượng thực dùng. Hoàn theo ĐÚNG số lượng đã ghi (không tính lại
     * công thức, vì định mức có thể đã đổi từ lúc ghi). Chạy TRONG tx của caller.
     */
    public void releaseRemakeReservation(Connection conn, int branchId, int orderItemId, Integer userId)
            throws SQLException {
        for (WasteEventItem line : repository.wasteEventItemDao.findActiveRemakeLinesOfLatestEvent(conn, branchId, orderItemId)) {
            // Đánh dấu VOIDED TRƯỚC rồi mới hoàn kho: câu UPDATE có điều kiện Status='ACTIVE' là chốt
            // nguyên tử, nên hai lần huỷ song song (khách bấm hai lần) chỉ có một lần cộng lại tồn.
            if (repository.wasteEventItemDao.updateStatusForBranch(conn, line.getWasteEntryId(), branchId, "VOIDED") != 1) continue;
            // Khách tự huỷ đơn qua QR thì không có userId — quy về người đã ghi dòng hao hụt để audit vẫn có chủ.
            int performedBy = userId == null ? line.getLoggedBy() : userId;
            ledgerService.applyTxn(conn, branchId, line.getIngredientId(), line.getQuantity(),   // hoàn kho (+)
                    TxnType.WASTE, InventoryReferenceType.WASTE_ENTRY,
                    line.getWasteEntryId(), performedBy);
                    repository.activityLogDao.insertWasteEntry(conn, line.getWasteEntryId(), branchId, "VOID",
                    line.getQuantity().toPlainString(), null,
                    "Huỷ món khi đang giữ chỗ nguyên liệu làm lại - hoàn kho phần chưa pha", performedBy);
        }
    }

    /**
     * Tạo mẻ pha sẵn (Contract #2) — NƠI DUY NHẤT đổi RAW→PREPPED. Own tx.
     * Trừ RAW theo Recipe PREPPED (consumed = qtyProduced/yield × qtyPerYield), cộng PREPPED qtyProduced.
     */

}
