package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.math.*;
import java.sql.*;
import java.util.*;

/** Workflow mẻ pha sẵn, duyệt, huỷ, cập nhật và ghi quá hạn. */
public final class PrepInventoryService {
    private final InventoryRepository repository; private final InventoryLedgerService ledgerService;
    private final WasteInventoryService wasteService;
    public PrepInventoryService(){this(new InventoryRepository());}
    PrepInventoryService(InventoryRepository repository){this(repository,new InventoryLedgerService(repository));}
    PrepInventoryService(InventoryRepository repository,InventoryLedgerService ledgerService){this(repository,ledgerService,new WasteInventoryService(repository,ledgerService));}
    PrepInventoryService(InventoryRepository repository,InventoryLedgerService ledgerService,WasteInventoryService wasteService){this.repository=Objects.requireNonNull(repository);this.ledgerService=Objects.requireNonNull(ledgerService);this.wasteService=Objects.requireNonNull(wasteService);}

    public int createPrepBatch(int branchId, int preppedIngredientId, BigDecimal qtyProduced,
                               java.time.LocalDateTime expiresAt, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int batchId = doCreatePrepBatch(conn, branchId, preppedIngredientId, qtyProduced,
                        expiresAt, userId, null, false, false);
                conn.commit();
                return batchId;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }   // BusinessException → hoàn tác sạch
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Luồng Barista mới: hạn dùng do cấu hình PREPPED quyết định, target theo chi nhánh và
     * clientRequestId làm thao tác idempotent.
     */
    public com.cafe.model.PrepBatch createSuggestedPrepBatch(int branchId, int preppedIngredientId,
                                        BigDecimal qtyProduced, int userId, String clientRequestId) throws SQLException {
        if (qtyProduced == null || qtyProduced.signum() <= 0)
            throw new BusinessException("Sản lượng thực tế phải lớn hơn 0.");
        String requestId = normalizeRequestId(clientRequestId);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.PrepBatch existing = repository.prepBatchDao.findByClientRequest(conn, branchId, requestId);
                if (existing != null) { conn.commit(); return existing; }

                com.cafe.model.Ingredient ingredient = repository.ingredientDao.findById(conn, preppedIngredientId);
                if (ingredient == null || !ingredient.isActive()
                        || !"PREPPED".equals(ingredient.getIngredientType()))
                    throw new BusinessException("Nguyên liệu pha sẵn không còn khả dụng.");
                if (ingredient.getShelfLifeMinutes() == null)
                    throw new BusinessException("Admin chưa đặt hạn bảo quản cho " + ingredient.getName() + ".");

                BranchInventory policy = repository.biDao.findByBranchIngredient(conn, branchId, preppedIngredientId);
                if (policy == null || policy.getPrepTargetQty() == null)
                    throw new BusinessException("Manager chưa đặt mức tồn mục tiêu cho " + ingredient.getName() + ".");

                boolean requiresApproval = com.cafe.common.PrepApprovalPolicy.requiresApproval(
                        qtyProduced, policy.getPrepTargetQty());

                java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)
                        .plusMinutes(ingredient.getShelfLifeMinutes());
                int batchId = doCreatePrepBatch(conn, branchId, preppedIngredientId, qtyProduced,
                        expiresAt, userId, requestId, true, requiresApproval);
                com.cafe.model.PrepBatch created = repository.prepBatchDao.findByIdForBranch(conn, batchId, branchId);
                conn.commit();
                return created;
            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                    try (Connection retry = DBConnection.getConnection()) {
                        com.cafe.model.PrepBatch existing =
                                repository.prepBatchDao.findByClientRequest(retry, branchId, requestId);
                        if (existing != null) return existing;
                    }
                }
                throw e;
            } catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private static String normalizeRequestId(String raw) {
        if (raw == null) throw new BusinessException("Phiên xác nhận mẻ không hợp lệ. Vui lòng tải lại.");
        try {
            return java.util.UUID.fromString(raw.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Phiên xác nhận mẻ không hợp lệ. Vui lòng tải lại.");
        }
    }

    /**
     * Tạo NHIỀU mẻ pha sẵn trong MỘT transaction (Contract #2) — barista chọn nhiều món một lần.
     * Tất cả-hoặc-không: chỉ cần một dòng thiếu RAW/thiếu công thức → rollback toàn bộ, không mẻ nào được tạo.
     */
    public void createPrepBatches(int branchId, List<com.cafe.model.PrepBatchLine> lines, int userId) throws SQLException {
        if (lines == null || lines.isEmpty())
            throw new BusinessException("Chưa chọn nguyên liệu nào để pha.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (com.cafe.model.PrepBatchLine ln : lines) {
                    try {
                        doCreatePrepBatch(conn, branchId, ln.getPreppedIngredientId(),
                                ln.getQtyProduced(), ln.getExpiresAt(), userId, null, false, false);
                    } catch (BusinessException e) {
                        throw withPrepLineContext(ln, e);
                    }
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private BusinessException withPrepLineContext(com.cafe.model.PrepBatchLine line, BusinessException cause) {
        String name = line.getPreppedIngredientName();
        if (name == null || name.isBlank()) name = "Nguyên liệu #" + line.getPreppedIngredientId();
        String msg = cause.getMessage() == null ? "Không thể tạo mẻ." : cause.getMessage();
        if (msg.startsWith(name + ":")) return cause;
        return new BusinessException(name + ": " + msg);
    }

    /**
     * Lõi tạo 1 mẻ — chạy TRONG tx của caller: chặn thiếu công thức, guard tồn RAW, insert + ledger.
     * {@code requiresApproval=true}: RAW vẫn trừ ngay (đã tiêu thụ vật lý), nhưng PREP_IN bị hoãn tới
     * khi Manager duyệt (xem {@link #approvePrepBatch}) — mẻ insert với Status='PENDING'.
     */
    private int doCreatePrepBatch(Connection conn, int branchId, int preppedIngredientId,
                                  BigDecimal qtyProduced, java.time.LocalDateTime expiresAt, int userId,
                                  String clientRequestId, boolean enforceWorklist,
                                  boolean requiresApproval) throws SQLException {
        List<Recipe> recipe = repository.prepRecipeDao.findByPrepped(conn, preppedIngredientId);
        BigDecimal prepYieldQty = requirePrepYield(conn, preppedIngredientId);
        // Chặn thiếu công thức: tránh cộng PREPPED mà không trừ RAW nào (sai Contract #2).
        if (recipe.isEmpty())
            throw new BusinessException("Có nguyên liệu pha sẵn chưa khai báo công thức prep — không thể tạo mẻ.");
        // Tiền-kiểm đủ tồn RAW (chặn tồn âm). Đọc có khoá dòng: không khoá thì hai barista pha song song
        // cùng đọc tồn cũ, cùng qua guard rồi cùng trừ. Khoá theo thứ tự tên RAW (ORDER BY của
        // PrepRecipeDao) nên mọi transaction xếp hàng cùng chiều, không deadlock chéo.
        List<String> shortfalls = new ArrayList<>();
        for (Recipe line : recipe) {
            BigDecimal consumed = PrepConsumptionCalculator.consumedRaw(qtyProduced, prepYieldQty, line);
            BigDecimal onHand = repository.biDao.findQtyOnHandForUpdate(conn, branchId, line.getIngredientId());
            if (onHand.compareTo(consumed) < 0)
                shortfalls.add(line.getIngredientName() + ": cần " + plain(consumed)
                        + " / còn " + plain(onHand) + " " + line.getIngredientUnit());
        }
        if (!shortfalls.isEmpty())
            throw new BusinessException("Không đủ nguyên liệu thô để pha: " + String.join("; ", shortfalls) + ".");

        if (enforceWorklist) {
            // Giữ một thứ tự khoá duy nhất RAW → PREPPED cho tạo/hủy mẻ. Sau khi người đầu
            // commit, request thứ hai mới đọc lại tồn và bị chặn nếu task đã được hoàn thành.
            BigDecimal onHand = repository.biDao.findQtyOnHandForUpdate(conn, branchId, preppedIngredientId);
            BranchInventory policy = repository.biDao.findByBranchIngredient(conn, branchId, preppedIngredientId);
            if (policy == null || policy.getPrepTargetQty() == null)
                throw new BusinessException("Manager chưa đặt mức tồn mục tiêu cho nguyên liệu pha sẵn.");
            if (onHand.signum() < 0)
                throw new BusinessException("Tồn nguyên liệu pha sẵn đang âm — cần Manager kiểm kê trước.");
            if (onHand.compareTo(policy.getMinThreshold()) > 0)
                throw new BusinessException("Nguyên liệu đã trên ngưỡng cảnh báo — không còn cần pha lúc này.");
        }

        int batchId = repository.prepBatchDao.insert(conn, branchId, preppedIngredientId, qtyProduced,
                expiresAt, userId, clientRequestId, requiresApproval);
        // RAW luôn bị trừ ngay — đã tiêu thụ vật lý lúc pha, không phụ thuộc việc có cần duyệt hay không.
        for (Recipe line : recipe) {
            ledgerService.applyTxn(conn, branchId, line.getIngredientId(),
                    PrepConsumptionCalculator.consumedRaw(qtyProduced, prepYieldQty, line).negate(),
                    TxnType.PREP_OUT, InventoryReferenceType.PREP_BATCH, (long) batchId, userId);
        }
        if (!requiresApproval) {
            // Mặc định: có hiệu lực ngay, bán được ngay. Mẻ bất thường (PENDING) chờ approvePrepBatch.
            ledgerService.applyTxn(conn, branchId, preppedIngredientId, qtyProduced,
                    TxnType.PREP_IN, InventoryReferenceType.PREP_BATCH, (long) batchId, userId);
        }
        return batchId;
    }

    private static String plain(BigDecimal v) {
        return com.cafe.common.QuantityFormat.plain(v);
    }

    private BigDecimal requirePrepYield(Connection conn, int preppedIngredientId) throws SQLException {
        Ingredient ingredient = repository.ingredientDao.findById(conn, preppedIngredientId);
        if (ingredient == null || ingredient.getPrepYieldQty() == null
                || ingredient.getPrepYieldQty().signum() <= 0) {
            throw new BusinessException("Nguyên liệu pha sẵn chưa khai báo sản lượng một mẻ.");
        }
        return ingredient.getPrepYieldQty();
    }

    /**
     * Huỷ mẻ pha sẵn (Contract #2, #4) — HOÀN KHO BẰNG TXN BÙ, không hard-delete.
     * Đảo lại createPrepBatch theo ĐÚNG lượng sổ cái đã ghi cho chính mẻ này
     * (ReferenceType/ReferenceId), KHÔNG
     * tính lại theo công thức: định mức có thể đã đổi từ lúc pha, và số đã ghi bị làm tròn về
     * DECIMAL(12,3). Đọc lại sổ nên ledger nets về đúng 0 theo từng type. Cùng nguyên tắc với
     * {@link #releaseRemakeReservation}. Đánh dấu Status='CANCELLED'. Own tx.
     *
     * @return false nếu mẻ đã ở trạng thái huỷ từ trước (idempotent, không ghi gì thêm).
     */
    public boolean cancelPrepBatch(int branchId, int prepBatchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean result = cancelPrepBatchInTx(conn, branchId, prepBatchId, userId);
                conn.commit();
                return result;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Manager đính chính mẻ sai; chặn bảo thủ nếu PREPPED đã phát sinh bất kỳ lượt trừ nào. */
    public boolean cancelPrepBatchByManager(int branchId, int prepBatchId, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.PrepBatch batch = repository.prepBatchDao.findByIdForBranch(conn, prepBatchId, branchId);
                if (batch == null) throw new BusinessException("Mẻ pha không còn khả dụng.");
                if (batch.getMadeAt() != null && repository.txnDao.hasNegativeAfter(conn, branchId,
                        batch.getPreppedIngredientId(), batch.getMadeAt())) {
                    throw new BusinessException("Nguyên liệu này đã phát sinh tiêu thụ sau khi tạo mẻ. "
                            + "Không thể hủy tự động; hãy kiểm kê và điều chỉnh tồn thực tế.");
                }
                boolean result = cancelPrepBatchInTx(conn, branchId, prepBatchId, userId);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Manager duyệt mẻ PENDING → ACTIVE: lúc này mới cộng PREPPED (PREP_IN). Own tx.
     * UPDATE-guard chạy TRƯỚC applyTxn để tự làm row-lock chốt nguyên tử — 2 request duyệt/từ chối
     * trùng nhau (double-click, 2 tab) thì request thứ hai luôn nhận 0 rows trước khi kịp ghi sổ cái.
     */
    public com.cafe.model.PrepBatch approvePrepBatch(int branchId, int prepBatchId, int reviewerId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.PrepBatch b = repository.prepBatchDao.findByIdForBranch(conn, prepBatchId, branchId);
                if (b == null) throw new BusinessException("Mẻ pha không còn khả dụng. Vui lòng tải lại.");
                if (!b.isPending()) throw new BusinessException("Mẻ không còn ở trạng thái chờ duyệt. Vui lòng tải lại.");
                if (b.isExpiredWhilePending())
                    throw new BusinessException("Mẻ đã quá hạn dùng trong lúc chờ duyệt — hãy Từ chối, "
                            + "RAW sẽ được hoàn lại; barista cần pha mẻ mới.");
                if (repository.prepBatchDao.approve(conn, prepBatchId, branchId, reviewerId) != 1)
                    throw new BusinessException("Mẻ đã được xử lý bởi thao tác khác. Vui lòng tải lại.");
                ledgerService.applyTxn(conn, branchId, b.getPreppedIngredientId(), b.getQuantityProduced(),
                        TxnType.PREP_IN, InventoryReferenceType.PREP_BATCH,
                        (long) prepBatchId, reviewerId);
                com.cafe.model.PrepBatch updated = repository.prepBatchDao.findByIdForBranch(conn, prepBatchId, branchId);
                conn.commit();
                return updated;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Manager từ chối mẻ PENDING → REJECTED: hoàn RAW (đảo PREP_OUT), KHÔNG đụng PREPPED (chưa
     * từng có PREP_IN nào cho mẻ này). Own tx. Cùng nguyên tắc UPDATE-guard-trước như approve.
     */
    public com.cafe.model.PrepBatch rejectPrepBatch(int branchId, int prepBatchId, int reviewerId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.PrepBatch b = repository.prepBatchDao.findByIdForBranch(conn, prepBatchId, branchId);
                if (b == null) throw new BusinessException("Mẻ pha không còn khả dụng. Vui lòng tải lại.");
                if (!b.isPending()) throw new BusinessException("Mẻ không còn ở trạng thái chờ duyệt. Vui lòng tải lại.");
                if (repository.prepBatchDao.reject(conn, prepBatchId, branchId, reviewerId) != 1)
                    throw new BusinessException("Mẻ đã được xử lý bởi thao tác khác. Vui lòng tải lại.");
                Map<Integer, BigDecimal> rawApplied = appliedRawOfPrepBatch(conn, branchId, prepBatchId, b);
                for (Map.Entry<Integer, BigDecimal> e : rawApplied.entrySet()) {
                    ledgerService.applyTxn(conn, branchId, e.getKey(), e.getValue().negate(),   // âm→dương: hoàn RAW
                            TxnType.PREP_OUT, InventoryReferenceType.PREP_BATCH,
                            (long) prepBatchId, reviewerId);
                }
                com.cafe.model.PrepBatch updated = repository.prepBatchDao.findByIdForBranch(conn, prepBatchId, branchId);
                conn.commit();
                return updated;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Hậu kiểm không chặn: Manager đánh dấu "đã xem, đúng" — KHÔNG đổi kho, chỉ phục vụ audit. */
    public void markPrepBatchReviewed(int branchId, int prepBatchId, int reviewerId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            if (repository.prepBatchDao.markReviewed(conn, prepBatchId, branchId, reviewerId) != 1)
                throw new BusinessException("Mẻ không còn ở trạng thái chờ hậu kiểm. Vui lòng tải lại.");
        }
    }

    private boolean cancelPrepBatchInTx(Connection conn, int branchId, int prepBatchId, int userId)
            throws SQLException {
        com.cafe.model.PrepBatch b = repository.prepBatchDao.findByIdForBranch(conn, prepBatchId, branchId);
        if (b == null) throw new BusinessException("Mẻ pha không còn khả dụng. Vui lòng tải lại.");
        if (!b.isActive()) return false;
        if (b.isWrittenOff())
            throw new BusinessException("Mẻ đã ghi hao hụt vì quá hạn — không huỷ được nữa.");

        Map<Integer, BigDecimal> rawApplied = appliedRawOfPrepBatch(conn, branchId, prepBatchId, b);
        BigDecimal preppedApplied = appliedPreppedOfPrepBatch(conn, branchId, prepBatchId, b);
        for (Map.Entry<Integer, BigDecimal> e : rawApplied.entrySet()) {
            ledgerService.applyTxn(conn, branchId, e.getKey(), e.getValue().negate(),
                    TxnType.PREP_OUT, InventoryReferenceType.PREP_BATCH, (long) prepBatchId, userId);
        }
        requirePreppedOnHandForReduction(conn, branchId, b.getPreppedIngredientId(),
                b.getPreppedIngredientName(), b.getPreppedIngredientUnit(), preppedApplied, "huỷ mẻ");
        ledgerService.applyTxn(conn, branchId, b.getPreppedIngredientId(), preppedApplied.negate(),
                TxnType.PREP_IN, InventoryReferenceType.PREP_BATCH, (long) prepBatchId, userId);
        if (repository.prepBatchDao.updateStatusForBranch(conn, prepBatchId, branchId, "CANCELLED") != 1)
            throw new BusinessException("Mẻ đã được thay đổi bởi thao tác khác. Vui lòng tải lại.");
        return true;
    }

    /**
     * Lượng RAW mà chính mẻ này đã trừ, đọc từ sổ cái (giá trị âm, gộp theo nguyên liệu).
     * Mẻ cũ chưa có dấu vết sổ cái thì mới tính lại theo công thức — nhánh dự phòng cho dữ liệu
     * có trước khi ledger được ghi đầy đủ, không phải đường đi thông thường.
     */
    private Map<Integer, BigDecimal> appliedRawOfPrepBatch(Connection conn, int branchId, int prepBatchId,
                                                           com.cafe.model.PrepBatch batch) throws SQLException {
        Map<Integer, BigDecimal> applied = repository.txnDao.sumByRef(conn, branchId,
                InventoryReferenceType.PREP_BATCH, prepBatchId,
                TxnType.PREP_OUT.name());
        if (!applied.isEmpty()) return applied;
        List<Recipe> recipe = repository.prepRecipeDao.findByPrepped(conn, batch.getPreppedIngredientId());
        if (recipe.isEmpty()) return applied;
        BigDecimal prepYieldQty = requirePrepYield(conn, batch.getPreppedIngredientId());
        for (Recipe line : recipe) {
            BigDecimal consumed = PrepConsumptionCalculator.consumedRaw(
                    batch.getQuantityProduced(), prepYieldQty, line);
            applied.merge(line.getIngredientId(), consumed.negate(), BigDecimal::add);
        }
        return applied;
    }

    /** Lượng PREPPED mà chính mẻ này đã cộng vào tồn (dương). Trống sổ thì lấy sản lượng đang lưu. */
    private BigDecimal appliedPreppedOfPrepBatch(Connection conn, int branchId, int prepBatchId,
                                                 com.cafe.model.PrepBatch batch) throws SQLException {
        BigDecimal applied = repository.txnDao.sumByRef(conn, branchId,
                InventoryReferenceType.PREP_BATCH, prepBatchId, TxnType.PREP_IN.name())
                .get(batch.getPreppedIngredientId());
        return applied == null ? batch.getQuantityProduced() : applied;
    }

    /**
     * Sửa sản lượng mẻ pha sẵn (Contract #2, #4) — áp TXN cho phần CHÊNH LỆCH (delta = new − old).
     * delta>0: trừ thêm RAW + cộng thêm PREPPED; delta<0: hoàn RAW + rút bớt PREPPED. Own tx.
     */
    public void updatePrepBatch(int branchId, int prepBatchId, BigDecimal newQtyProduced, int userId) throws SQLException {
        if (newQtyProduced == null || newQtyProduced.signum() <= 0) throw new IllegalArgumentException("Sản lượng phải > 0");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.PrepBatch b = repository.prepBatchDao.findByIdForBranch(conn, prepBatchId, branchId);
                if (b == null) throw new BusinessException("Mẻ pha không còn khả dụng. Vui lòng tải lại.");
                if (!b.isActive()) throw new BusinessException("Mẻ đã huỷ — không sửa được.");
                if (b.isWrittenOff())
                    throw new BusinessException("Mẻ đã ghi hao hụt vì quá hạn — không sửa sản lượng được nữa.");
                BigDecimal delta = newQtyProduced.subtract(b.getQuantityProduced());
                if (delta.signum() != 0) {
                    // Tăng sản lượng → tiêu hao MỚI, tính theo định mức hiện hành + tiền-kiểm đủ tồn (khoá dòng).
                    if (delta.signum() > 0) {
                        if (isExpired(b))
                            throw new BusinessException("Mẻ đã quá hạn — hãy pha mẻ mới thay vì tăng sản lượng mẻ này.");
                        List<Recipe> recipe = repository.prepRecipeDao.findByPrepped(conn, b.getPreppedIngredientId());
                        if (recipe.isEmpty())
                            throw new BusinessException("Công thức prep đã bị xoá — không thể tăng sản lượng mẻ này.");
                        BigDecimal prepYieldQty = requirePrepYield(conn, b.getPreppedIngredientId());
                        List<String> shortfalls = new ArrayList<>();
                        for (Recipe line : recipe) {
                            BigDecimal need = PrepConsumptionCalculator.consumedRaw(delta, prepYieldQty, line);
                            BigDecimal onHand = repository.biDao.findQtyOnHandForUpdate(conn, branchId, line.getIngredientId());
                            if (onHand.compareTo(need) < 0)
                                shortfalls.add(line.getIngredientName() + ": cần thêm " + plain(need)
                                        + " / còn " + plain(onHand) + " " + line.getIngredientUnit());
                        }
                        if (!shortfalls.isEmpty())
                            throw new BusinessException("Không đủ nguyên liệu thô để tăng sản lượng: " + String.join("; ", shortfalls) + ".");
                        for (Recipe line : recipe) {
                            ledgerService.applyTxn(conn, branchId, line.getIngredientId(),
                                    PrepConsumptionCalculator.consumedRaw(delta, prepYieldQty, line).negate(),
                                    TxnType.PREP_OUT, InventoryReferenceType.PREP_BATCH,
                                    (long) prepBatchId, userId);
                        }
                    } else {
                        // Giảm sản lượng → HOÀN theo tỉ lệ trên lượng SỔ CÁI đã ghi cho mẻ này, không tính
                        // lại công thức. Nhờ vậy huỷ/giảm luôn nets về 0 dù định mức đã đổi hay số đã làm tròn.
                        Map<Integer, BigDecimal> rawApplied = appliedRawOfPrepBatch(conn, branchId, prepBatchId, b);
                        for (Map.Entry<Integer, BigDecimal> e : rawApplied.entrySet()) {
                            BigDecimal refund = e.getValue().negate()
                                    .multiply(delta.abs())
                                    .divide(b.getQuantityProduced(), 6, RoundingMode.HALF_UP);
                            ledgerService.applyTxn(conn, branchId, e.getKey(), refund,                        // hoàn RAW (+)
                                    TxnType.PREP_OUT, InventoryReferenceType.PREP_BATCH,
                                    (long) prepBatchId, userId);
                        }
                        // Guard PREPPED sau phần RAW: giữ chiều khoá RAW → PREPPED giống lúc tạo mẻ.
                        requirePreppedOnHandForReduction(conn, branchId, b.getPreppedIngredientId(),
                                b.getPreppedIngredientName(), b.getPreppedIngredientUnit(), delta.abs(), "giảm sản lượng mẻ");
                    }
                    ledgerService.applyTxn(conn, branchId, b.getPreppedIngredientId(), delta,                 // delta>0 cộng thêm PREPPED
                            TxnType.PREP_IN, InventoryReferenceType.PREP_BATCH,
                            (long) prepBatchId, userId);
                    if (repository.prepBatchDao.updateQuantityForBranch(conn, prepBatchId, branchId,
                            newQtyProduced, b.getQuantityProduced()) != 1) {
                        throw new BusinessException("Mẻ đã được thay đổi bởi thao tác khác. Vui lòng tải lại.");
                    }
                }
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }   // BusinessException → hoàn tác sạch
            finally { conn.setAutoCommit(true); }
        }
    }

    /** Mẻ đã qua hạn dùng (so với giờ UTC hiện tại). Mẻ không đặt hạn thì không bao giờ quá hạn. */
    private static boolean isExpired(com.cafe.model.PrepBatch batch) {
        return batch.getExpiresAt() != null
                && batch.getExpiresAt().isBefore(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
    }

    /**
     * Ghi hao hụt cho một mẻ pha sẵn ĐÃ QUÁ HẠN rồi đóng vòng đời của mẻ (Contract #2, #4). Own tx.
     * Gộp hai việc vốn rời nhau — ghi hao hụt ở màn Waste và mẻ vẫn treo ACTIVE mãi — vào một
     * transaction: trừ tồn đúng một lần rồi đánh dấu WrittenOffAt, nên mẻ không hiện lại ở banner
     * quá hạn để bị ghi hao hụt chồng lên, và banner bàn giao ca tự tắt khi đã xử lý xong.
     * Chốt nguyên tử là câu UPDATE có điều kiện WrittenOffAt IS NULL.
     *
     * @return WasteEntryId vừa ghi.
     */
    public long writeOffExpiredPrepBatch(int branchId, int prepBatchId, BigDecimal qty, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                com.cafe.model.PrepBatch b = repository.prepBatchDao.findByIdForBranch(conn, prepBatchId, branchId);
                if (b == null) throw new BusinessException("Mẻ pha không còn khả dụng. Vui lòng tải lại.");
                if (!b.isActive()) throw new BusinessException("Mẻ đã huỷ — không ghi hao hụt cho mẻ này.");
                if (b.isWrittenOff()) throw new BusinessException("Mẻ này đã được ghi hao hụt. Vui lòng tải lại.");
                if (!isExpired(b))
                    throw new BusinessException("Mẻ chưa quá hạn — dùng màn Hao hụt nếu cần ghi nhận hao hụt thường.");
                WasteInventoryService.requireWasteQuantity(qty);
                String unit = b.getPreppedIngredientUnit() == null ? "" : " " + b.getPreppedIngredientUnit();
                if (qty.compareTo(b.getQuantityProduced()) > 0)
                    throw new BusinessException("Lượng hao hụt vượt sản lượng của mẻ: tối đa "
                            + plain(b.getQuantityProduced()) + unit + ".");
                requirePreppedOnHandForReduction(conn, branchId, b.getPreppedIngredientId(),
                        b.getPreppedIngredientName(), b.getPreppedIngredientUnit(), qty, "ghi hao hụt mẻ quá hạn");

                String reason = "Mẻ pha sẵn #" + prepBatchId + " quá hạn "
                        + BusinessDay.fmtFullDateTimeVn(b.getExpiresAt());
                long wasteEntryId = wasteService.logWasteInTx(conn, branchId, b.getPreppedIngredientId(), qty,
                        "EXPIRED", reason, userId);
                if (repository.prepBatchDao.markWrittenOff(conn, prepBatchId, branchId, wasteEntryId) != 1) {
                    throw new BusinessException("Mẻ đã được xử lý bởi thao tác khác. Vui lòng tải lại.");
                }
                conn.commit();
                return wasteEntryId;
            } catch (SQLException e) { conn.rollback(); throw e; }
            catch (RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private void requirePreppedOnHandForReduction(Connection conn, int branchId, int preppedIngredientId,
                                                  String name, String unit, BigDecimal qtyToRemove,
                                                  String actionLabel) throws SQLException {
        // Khoá dòng như guard tồn RAW: hai thao tác rút song song không được cùng đọc tồn cũ rồi cùng trừ.
        BigDecimal onHand = repository.biDao.findQtyOnHandForUpdate(conn, branchId, preppedIngredientId);
        if (onHand.compareTo(qtyToRemove) >= 0) return;

        String ingredient = (name == null || name.isBlank()) ? "nguyên liệu pha sẵn" : name;
        String suffix = unit == null || unit.isBlank() ? "" : " " + unit;
        // BranchInventory gộp theo nguyên liệu, không theo từng mẻ; guard này chặn rút quá tồn hiện có.
        throw new BusinessException("Không thể " + actionLabel + " " + ingredient + ": cần rút "
                + plain(qtyToRemove) + suffix + " nhưng tồn hiện còn " + plain(onHand) + suffix
                + ". Phần còn lại có thể đã được dùng; hãy ghi hao hụt phần tồn còn lại hoặc báo Quản lý kiểm kê.");
    }

    /** Ghi hao hụt (Barista) — insert WasteEntry + ledgerService.applyTxn(-qty, WASTE). Own tx. */

}
