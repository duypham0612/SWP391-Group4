package com.cafe.service.shared;

import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.common.BusinessException;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.cashier.BillItemDao;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.ModifierGroupDao;
import com.cafe.dao.shared.ModifierOptionDao;
import com.cafe.dao.shared.OrderDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.dao.shared.OrderItemActionDao;
import com.cafe.dao.shared.OrderItemModifierDao;
import com.cafe.dao.shared.ProductModifierGroupDao;
import com.cafe.dao.shared.ProductRecipeDao;
import com.cafe.dao.admin.ProductDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.ModifierGroup;
import com.cafe.model.ModifierOption;
import com.cafe.model.Order;
import com.cafe.model.OrderItem;
import com.cafe.model.PickupTicket;
import com.cafe.model.ProductRecipe;
import com.cafe.model.ProductModifierGroup;
import com.cafe.model.Product;
import com.cafe.model.StockAdjustment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lõi Sales — Cashier SỞ HỮU mọi order entry (đơn quầy & QR cùng một service).
 * Contract #1: order.created + status enum chung; #3: cùng bảng sales.Orders.
 */
public class OrderService {

    private final OrderDao orderDao = new OrderDao();
    private final OrderItemDao itemDao = new OrderItemDao();
    private final OrderItemActionDao actionDao = new OrderItemActionDao();
    private final OrderItemModifierDao oimDao = new OrderItemModifierDao();
    private final BranchMenuDao branchMenuDao = new BranchMenuDao();
    private final ModifierOptionDao optionDao = new ModifierOptionDao();
    private final ModifierGroupDao groupDao = new ModifierGroupDao();
    private final ProductModifierGroupDao pmgDao = new ProductModifierGroupDao();
    private final BillDao billDao = new BillDao();
    private final BillItemDao billItemDao = new BillItemDao();
    private final ProductRecipeDao productRecipeDao = new ProductRecipeDao();
    private final ProductDao productDao = new ProductDao();
    private final com.cafe.dao.admin.BranchDao branchDao = new com.cafe.dao.admin.BranchDao();
    private final InventoryService inventoryService = new InventoryService();

    /** Một dòng giỏ hàng từ POS/QR. */
    public static class CartLine {
        public int productId;
        public int quantity;
        public String note;
        public List<Integer> optionIds = new ArrayList<>();
        public String size = "M";
        public String iceLevel = "Bình thường";
        public String sugarLevel = "100%";
    }

    public static class UnblockResult {
        private final boolean success;
        private final int remainingBlockedWithRecountedIngredients;

        public UnblockResult(boolean success, int remainingBlockedWithRecountedIngredients) {
            this.success = success;
            this.remainingBlockedWithRecountedIngredients = remainingBlockedWithRecountedIngredients;
        }

        public boolean isSuccess() { return success; }
        public int getRemainingBlockedWithRecountedIngredients() {
            return remainingBlockedWithRecountedIngredients;
        }
    }

    /**
     * Đặt đơn (COUNTER hoặc QR) — tạo Order + OrderItem(WAITING) + OrderItemModifier, publish order.created.
     * Tất cả trong MỘT transaction. Giá tính server-side (giá menu chi nhánh + Σ priceDelta option).
     */
    public int placeOrder(int branchId, Integer sessionId, String source, String orderType,
                          Integer createdBy, List<CartLine> lines) throws SQLException {
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("Đơn rỗng");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<Integer, BigDecimal> priceByProduct = new HashMap<>();
                Map<Integer, Boolean> is86ByProduct = new HashMap<>();
                Map<Integer, String> nameByProduct = new HashMap<>();
                for (BranchMenuItem bm : branchMenuDao.listForBranch(conn, branchId)) {
                    priceByProduct.put(bm.getProductId(),
                            bm.getLocalPrice() != null ? bm.getLocalPrice() : bm.getBasePrice());
                    is86ByProduct.put(bm.getProductId(), bm.isIs86());
                    nameByProduct.put(bm.getProductId(), bm.getProductName());
                }

                Order o = new Order();
                o.setBranchId(branchId);
                o.setTableSessionId(sessionId);
                o.setSource(source);
                o.setOrderType(orderType == null ? "DINE_IN" : orderType);
                o.setStatus("ACTIVE");
                o.setCreatedBy(createdBy);
                int orderId = orderDao.insert(conn, o);

                // Mã gọi món: sinh ngay trong transaction tạo đơn để KDS/bàn giao/khách QR khớp
                // đúng ly với đơn. Số thứ tự đếm theo ngày kinh doanh của chi nhánh.
                String pickupCode = buildPickupCode(conn, branchId, o.getSource(), o.getOrderType());
                orderDao.updatePickupCode(conn, orderId, pickupCode);

                for (CartLine line : lines) {
                    if (line.quantity <= 0) continue;
                    BigDecimal base = priceByProduct.get(line.productId);
                    if (base == null) throw new SQLException("Sản phẩm " + line.productId + " không bán ở chi nhánh này");
                    // Chặn oversell tại nguồn: món đang 86 (hết) thì không cho vào đơn (Contract #3 — cashier sở hữu order entry).
                    if (Boolean.TRUE.equals(is86ByProduct.get(line.productId))) {
                        String nm = nameByProduct.getOrDefault(line.productId, "#" + line.productId);
                        throw new IllegalArgumentException("Món \"" + nm + "\" đang hết (86) — vui lòng bỏ khỏi đơn.");
                    }

                    List<Integer> optionIds = validateOptions(conn, line.productId, line.optionIds);
                    Product product = productDao.findById(conn, line.productId);
                    String size = cleanSize(line.size, product);
                    String iceLevel = cleanIce(line.iceLevel);
                    String sugarLevel = cleanSugar(line.sugarLevel);

                    // gom priceDelta của các option đã chọn
                    BigDecimal unit = base.add(sizeDelta(product, size));
                    Map<Integer, BigDecimal> deltaByOption = new HashMap<>();
                    for (Integer optId : optionIds) {
                        ModifierOption opt = optionDao.findById(conn, optId);
                        deltaByOption.put(optId, opt.getPriceDelta());
                        unit = unit.add(opt.getPriceDelta());
                    }

                    OrderItem it = new OrderItem();
                    it.setOrderId(orderId);
                    it.setProductId(line.productId);
                    it.setQuantity(line.quantity);
                    it.setUnitPrice(unit);
                    it.setNote(buildOptionNote(size, iceLevel, sugarLevel, line.note));
                    it.setStatus("WAITING");
                    int itemId = itemDao.insert(conn, it);

                    for (Integer optId : optionIds) {
                        oimDao.insert(conn, itemId, optId,
                                deltaByOption.getOrDefault(optId, BigDecimal.ZERO));
                    }
                }

                EventPublisher.publish(conn, EventType.ORDER_CREATED, String.valueOf(orderId), branchId,
                        "{\"orderId\":" + orderId + ",\"source\":\"" + source + "\"}");
                conn.commit();
                return orderId;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally { conn.setAutoCommit(true); }
        }
    }

    /**
     * Sinh mã gọi món dạng {prefix}{seq}: G=đơn QR khách, T=mang đi, D=tại bàn.
     * seq là thứ tự đơn trong ngày kinh doanh của chi nhánh (đếm ngay sau insert nên gồm cả đơn này).
     * Chấp nhận rủi ro trùng cực hiếm khi hai đơn commit cùng nano-giây — đủ cho tải một quán.
     */
    private String buildPickupCode(Connection conn, int branchId, String source, String orderType)
            throws SQLException {
        com.cafe.model.Branch branch = branchDao.findById(conn, branchId);
        java.time.LocalTime openTime = branch == null ? null : branch.getOpenTime();
        java.time.LocalDateTime dayStart = com.cafe.common.BusinessDay.startUtc(openTime);
        int seq = orderDao.countOrdersSince(conn, branchId, dayStart);
        String prefix = "QR".equals(source) ? "G" : ("TAKEAWAY".equals(orderType) ? "T" : "D");
        return prefix + seq;
    }

    private String cleanSize(String value, Product product) {
        if (product == null || !product.isSizeEnabled()) return "M";
        String size = value == null ? "M" : value.trim().toUpperCase(java.util.Locale.ROOT);
        return ("S".equals(size) || "M".equals(size) || "L".equals(size)) ? size : "M";
    }

    private BigDecimal sizeDelta(Product product, String size) {
        if (product == null || !product.isSizeEnabled()) return BigDecimal.ZERO;
        if ("S".equals(size)) return product.getSizeSDelta();
        if ("L".equals(size)) return product.getSizeLDelta();
        return product.getSizeMDelta();
    }

    private String cleanIce(String value) {
        if (value == null) return "Bình thường";
        String v = value.trim();
        if ("Không đá".equals(v) || "Ít đá".equals(v) || "Bình thường".equals(v) || "Nhiều đá".equals(v)) return v;
        return "Bình thường";
    }

    private String cleanSugar(String value) {
        if (value == null) return "100%";
        String v = value.trim();
        if ("0%".equals(v) || "30%".equals(v) || "50%".equals(v) || "70%".equals(v) || "100%".equals(v)) return v;
        return "100%";
    }

    private String buildOptionNote(String size, String iceLevel, String sugarLevel, String note) {
        String options = "Size " + size + ", " + iceLevel + ", đường " + sugarLevel;
        if (note == null || note.isBlank()) return options;
        return options + " | " + note.trim();
    }

    private List<Integer> validateOptions(Connection conn, int productId, List<Integer> optionIds) throws SQLException {
        List<Integer> selected = optionIds == null ? List.of() : optionIds;
        if (selected.isEmpty()) return new ArrayList<>();
        Map<Integer, ModifierGroup> groupsById = new HashMap<>();
        Map<Integer, Integer> optionToGroup = new HashMap<>();
        Map<Integer, Integer> selectedCountByGroup = new HashMap<>();

        for (ProductModifierGroup pmg : pmgDao.findByProduct(conn, productId)) {
            ModifierGroup group = groupDao.findById(conn, pmg.getModifierGroupId());
            if (group == null) continue;
            groupsById.put(group.getModifierGroupId(), group);
            for (ModifierOption option : optionDao.findByGroup(conn, group.getModifierGroupId())) {
                if (option.isActive()) optionToGroup.put(option.getModifierOptionId(), group.getModifierGroupId());
            }
        }

        List<Integer> validOptionIds = new ArrayList<>();
        for (Integer optionId : selected) {
            if (optionId == null) continue;
            Integer groupId = optionToGroup.get(optionId);
            if (groupId == null) {
                throw new IllegalArgumentException("Tuỳ chọn không hợp lệ cho món này.");
            }
            selectedCountByGroup.merge(groupId, 1, Integer::sum);
            validOptionIds.add(optionId);
        }

        for (ModifierGroup group : groupsById.values()) {
            int count = selectedCountByGroup.getOrDefault(group.getModifierGroupId(), 0);
            if (group.isRequired() && count < group.getMinSelect()) {
                throw new IllegalArgumentException("Vui lòng chọn " + group.getName() + ".");
            }
            if (count < group.getMinSelect()) {
                throw new IllegalArgumentException("Vui lòng chọn tối thiểu " + group.getMinSelect() + " tuỳ chọn cho " + group.getName() + ".");
            }
            if (group.getMaxSelect() > 0 && count > group.getMaxSelect()) {
                throw new IllegalArgumentException(group.getName() + " chỉ được chọn tối đa " + group.getMaxSelect() + " tuỳ chọn.");
            }
        }
        return validOptionIds;
    }

    // ---------- KDS (Barista) ----------

    public List<OrderItem> getKdsQueue(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> items = itemDao.findKdsQueue(conn, branchId);
            java.util.Set<Integer> productIds = new java.util.HashSet<>();
            for (OrderItem it : items) productIds.add(it.getProductId());
            java.util.Set<Integer> withRecipe = productRecipeDao.findProductIdsWithRecipe(conn, productIds);
            for (OrderItem it : items) {
                it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
                it.setRecipeMissing(!withRecipe.contains(it.getProductId()));   // cảnh báo món chưa có công thức
            }
            return items;
        }
    }

    /** Toàn bộ dữ liệu ba cột Quầy pha chế. */
    public List<OrderItem> getBaristaWorkbench(int branchId) throws SQLException {
        return getBaristaWorkbench(branchId, null);
    }

    /** Hàng chờ của ngày kinh doanh hiện tại (null = không cắt theo ngày). */
    public List<OrderItem> getBaristaWorkbench(int branchId, java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> items = itemDao.findBaristaWorkbench(conn, branchId, businessDayStartUtc);
            java.util.Set<Integer> productIds = new java.util.HashSet<>();
            for (OrderItem it : items) productIds.add(it.getProductId());
            java.util.Set<Integer> withRecipe = productRecipeDao.findProductIdsWithRecipe(conn, productIds);
            for (OrderItem it : items) {
                it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
                it.setRecipeMissing(!withRecipe.contains(it.getProductId()));
            }
            return items;
        }
    }

    /** Món dang dở thuộc ngày kinh doanh trước — khu "Đơn treo cần xử lý". */
    public List<OrderItem> getStaleItems(int branchId, java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> items = itemDao.findStaleItems(conn, branchId, businessDayStartUtc);
            for (OrderItem it : items) it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
            return items;
        }
    }

    public List<OrderItem> getReadyItems(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> items = itemDao.findReady(conn, branchId);
            for (OrderItem it : items) it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
            return items;
        }
    }

    /** B2 · Món vừa giao gần đây (SERVED trong {@code minutes} phút) để hoàn tác giao nhầm. */
    public List<OrderItem> getRecentlyServed(int branchId, int minutes) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> items = itemDao.findRecentlyServed(conn, branchId, minutes);
            for (OrderItem it : items) it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
            return items;
        }
    }

    public List<OrderItem> getPickedUpItems(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> items = itemDao.findPickedUp(conn, branchId);
            for (OrderItem it : items) it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
            return items;
        }
    }

    public List<OrderItem> getSessionItemStatuses(int sessionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return itemDao.findBySession(conn, sessionId);
        }
    }

    /** WAITING → MAKING, khóa món bằng BaristaId trong cùng transaction. */
    public boolean startItem(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return false;
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            int rows = itemDao.claim(conn, orderItemId, sessionBranchId, userId);
            if (rows == 0) return false;   // sai trạng thái / khác chi nhánh / đã bị đổi
            actionDao.insert(conn, orderItemId, sessionBranchId, "CLAIM", "WAITING", "MAKING", null, userId);
            publishStatus(conn, it, "MAKING");
            return true;
        });
    }

    /**
     * ★ MAKING → READY — chỉ người đã nhận mới được hoàn thành.
     * Đây là điểm auto-deduct (Contract #1, #2). Chốt trạng thái NGUYÊN TỬ (updateStatusIf) TRƯỚC khi
     * trừ kho: chỉ request "claim" được món (rows==1) mới trừ → chống double-deduct khi 2 barista bấm
     * song song; scope chi nhánh chặn thao tác chéo chi nhánh.
     */
    public boolean markItemReady(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        return markItemReady(orderItemId, userId, sessionBranchId, null);
    }

    public boolean markItemReady(int orderItemId, Integer userId, int sessionBranchId,
                                 String handoverLocation) throws SQLException {
        if (userId == null) return false;
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            int rows = itemDao.completeClaimed(conn, orderItemId, sessionBranchId, userId, handoverLocation);
            if (rows == 0) return false;   // đã READY/SERVED/CANCELLED / khác chi nhánh → không trừ kho
            int branchId = branchOf(it);
            if (!it.isRemakeInventoryReserved()) {
                inventoryService.deductForOrderItem(conn, branchId, orderItemId, it.getProductId(), it.getQuantity(), userId);
            }
            actionDao.insert(conn, orderItemId, branchId, "COMPLETE", "MAKING", "READY", null, userId);
            publishStatus(conn, it, "READY");
            EventPublisher.publish(conn, EventType.ITEM_READY, String.valueOf(orderItemId), branchId,
                    "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId + ",\"by\":" + userId + "}");
            return true;
        });
    }

    /** MAKING → WAITING, chỉ chủ món được trả lại. */
    public boolean returnItemToQueue(int orderItemId, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null || itemDao.returnToQueue(conn, orderItemId, branchId, userId) == 0) return false;
            actionDao.insert(conn, orderItemId, branchId, "RETURN_QUEUE", "MAKING", "WAITING", null, userId);
            publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    /** Báo sự cố không hủy món; WAITING ai cũng báo, MAKING chỉ chủ món báo. */
    public boolean reportItemIssue(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn lý do sự cố.");
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null || itemDao.reportIssue(conn, orderItemId, branchId, userId, clean) == 0) return false;
            actionDao.insert(conn, orderItemId, branchId, "ISSUE", it.getStatus(), it.getStatus(), clean, userId);
            EventPublisher.publish(conn, EventType.ITEM_ISSUE_REPORTED, String.valueOf(orderItemId), branchId,
                    "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId
                            + ",\"reason\":\"" + clean + "\",\"by\":" + userId + "}");
            return true;
        });
    }

    /**
     * Nhóm B — món không pha được (hỏng máy, ngừng bán): WAITING/MAKING → BLOCKED.
     * Khác {@link #reportItemIssue} ở chỗ món RỜI hàng chờ thay vì chỉ gắn cờ, nên
     * barista khác không thể bấm "Nhận pha" rồi lại phát hiện y hệt vấn đề đó.
     */
    public boolean blockItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn lý do không pha được.");
        return tx(conn -> blockInTx(conn, orderItemId, clean, userId, branchId));
    }

    /**
     * Nhóm A — hết nguyên liệu: kiểm kê nguyên liệu về 0 QUA SỔ CÁI rồi chặn món, trong CÙNG một transaction.
     * Sửa nguyên nhân gốc (sổ kho đang lạc quan hơn thực tế vì tồn chỉ bị trừ lúc markReady) thay vì
     * chỉ ghi cờ. Tồn về 0 → {@code findProductsWithDepletedIngredient} tự suy ra MỌI món dùng nguyên
     * liệu đó và gợi ý ở màn "Báo hết món" — việc khoá menu vẫn là thao tác có ý thức của người dùng,
     * KHÔNG tự động, vì một nguyên liệu nằm trong nhiều món và đó là quyết định doanh thu.
     */
    public boolean blockItemForDepletedIngredients(int orderItemId, List<Integer> ingredientIds,
                                                   String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        if (ingredientIds == null || ingredientIds.isEmpty())
            throw new IllegalArgumentException("Vui lòng chọn nguyên liệu đã hết.");
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) clean = "Hết nguyên liệu";
        final String finalReason = clean;
        return tx(conn -> {
            // Chặn món trước: thua race (món vừa bị người khác xử lý) thì KHÔNG đụng tới sổ kho.
            if (!blockInTx(conn, orderItemId, finalReason, userId, branchId)) return false;
            for (Integer ingredientId : ingredientIds) {
                if (ingredientId == null) continue;
                inventoryService.applyAdjustmentInTx(conn, branchId, ingredientId, BigDecimal.ZERO,
                        "Barista báo hết tại quầy pha chế", null, userId);
            }
            return true;
        });
    }

    /** Dùng chung cho nhóm A và B — gọi TRONG tx của caller. */
    private boolean blockInTx(Connection conn, int orderItemId, String reason, int userId, int branchId)
            throws SQLException {
        OrderItem it = itemDao.findById(conn, orderItemId);
        if (it == null) return false;
        String from = it.getStatus();
        if (itemDao.blockItem(conn, orderItemId, branchId, userId, reason) == 0) return false;
        actionDao.insert(conn, orderItemId, branchId, "BLOCK", from, "BLOCKED", reason, userId);
        publishStatus(conn, it, "BLOCKED");
        EventPublisher.publish(conn, EventType.ITEM_ISSUE_REPORTED, String.valueOf(orderItemId), branchId,
                "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId
                        + ",\"reason\":\"" + reason + "\",\"by\":" + userId + "}");
        return true;
    }

    /** BLOCKED → WAITING khi nguyên liệu/máy đã có lại. Đường thoát bắt buộc, nếu không món kẹt vĩnh viễn. */
    public boolean unblockItem(int orderItemId, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null || itemDao.unblockItem(conn, orderItemId, branchId) == 0) return false;
            actionDao.insert(conn, orderItemId, branchId, "UNBLOCK", "BLOCKED", "WAITING", null, userId);
            publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    /**
     * BLOCKED → WAITING kèm kiểm kê tồn thật cho nguyên liệu vừa có lại.
     * Chốt trạng thái trước; nếu món đã bị thao tác khác xử lý thì không ghi sổ kho.
     */
    public UnblockResult unblockItem(int orderItemId, List<StockAdjustment> recounts,
                                     Integer userId, int branchId) throws SQLException {
        if (userId == null) return new UnblockResult(false, 0);
        List<StockAdjustment> cleanRecounts = recounts == null ? List.of() : recounts;
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null || itemDao.unblockItem(conn, orderItemId, branchId) == 0) {
                return new UnblockResult(false, 0);
            }

            Map<Integer, String> unitByIngredient = new HashMap<>();
            for (ProductRecipe line : productRecipeDao.findByProduct(conn, it.getProductId())) {
                unitByIngredient.put(line.getIngredientId(), line.getIngredientUnit());
            }

            java.util.Set<Integer> recountedIds = new java.util.LinkedHashSet<>();
            for (StockAdjustment recount : cleanRecounts) {
                if (recount == null || recount.getActualQty() == null) continue;
                if (!unitByIngredient.containsKey(recount.getIngredientId())) {
                    throw new BusinessException("Nguyên liệu kiểm kê không thuộc công thức của món này.");
                }
                String unit = unitByIngredient.get(recount.getIngredientId());
                inventoryService.applyAdjustmentInTx(conn, branchId, recount.getIngredientId(),
                        recount.getActualQty(), "Barista kiểm lại khi bỏ chặn tại quầy pha chế", unit, userId);
                recountedIds.add(recount.getIngredientId());
            }

            actionDao.insert(conn, orderItemId, branchId, "UNBLOCK", "BLOCKED", "WAITING", null, userId);
            publishStatus(conn, it, "WAITING");
            int remaining = itemDao.countBlockedUsingIngredients(conn, branchId, recountedIds);
            return new UnblockResult(true, remaining);
        });
    }

    /** Nguyên liệu trong công thức của một món — dựng danh sách chọn ở modal "Hết nguyên liệu". */
    public List<com.cafe.model.ProductRecipe> getRecipeIngredients(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productRecipeDao.findByProduct(conn, productId);
        }
    }

    /** Nguyên liệu trong công thức đang cạn tại chi nhánh — dựng modal kiểm kê khi bỏ chặn. */
    public List<ProductRecipe> getDepletedRecipeIngredients(int branchId, int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return productRecipeDao.findDepletedByProduct(conn, branchId, productId);
        }
    }

    /** READY → REMAKE → WAITING; lưu waste/ledger + audit trong cùng transaction. */
    public boolean remakeItem(int orderItemId, String reason, Integer userId, int branchId) throws SQLException {
        if (userId == null) return false;
        String clean = sanitizeReason(reason);
        if (clean.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn lý do làm lại.");
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null || itemDao.beginRemake(conn, orderItemId, branchId) == 0) return false;
            inventoryService.reserveRemakeForOrderItem(conn, branchId, orderItemId, it.getProductId(), it.getQuantity(), clean, userId);
            itemDao.finishRemake(conn, orderItemId, branchId);
            actionDao.insert(conn, orderItemId, branchId, "REMAKE", "READY", "WAITING", clean, userId);
            EventPublisher.publish(conn, EventType.ITEM_REMAKE_REQUESTED, String.valueOf(orderItemId), branchId,
                    "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId
                            + ",\"reason\":\"" + clean + "\",\"by\":" + userId + "}");
            publishStatus(conn, it, "WAITING");
            return true;
        });
    }

    /** B1 · Bump — đẩy món quá giờ lên đầu hàng chờ KDS (scope chi nhánh). */
    public void bumpItem(int orderItemId, int sessionBranchId) throws SQLException {
        txVoid(conn -> itemDao.bump(conn, orderItemId, sessionBranchId));
    }

    /**
     * ★ B1 · "Xong cả đơn" — trừ tồn + chuyển READY cho MỌI món WAITING/MAKING của đơn trong CÙNG một
     * transaction (đối xứng serveAllReady của Pickup). Mỗi món "claim" nguyên tử qua updateStatusIf;
     * món đã bị người khác đổi / khác chi nhánh sẽ bị bỏ qua (không trừ kho). Trả số món đã xong.
     */
    public int markOrderReady(int orderId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return 0;
        return tx(conn -> {
            int done = 0;
            for (OrderItem it : itemDao.findByOrder(conn, orderId)) {
                String s = it.getStatus();
                // Chỉ món đã nhận pha mới hoàn thành được: completeClaimed khoá theo BaristaId,
                // nên không thể bấm xong hộ món của người khác hay bỏ qua bước nhận.
                if (!"MAKING".equals(s)) continue;
                int rows = itemDao.completeClaimed(conn, it.getOrderItemId(), sessionBranchId, userId);
                if (rows == 0) continue;   // đổi bởi người khác / khác chi nhánh
                int branchId = branchOf(it);
                if (!it.isRemakeInventoryReserved()) inventoryService.deductForOrderItem(conn, branchId, it.getOrderItemId(),
                            it.getProductId(), it.getQuantity(), userId);
                actionDao.insert(conn, it.getOrderItemId(), branchId, "COMPLETE", "MAKING", "READY", null, userId);
                publishStatus(conn, it, "READY");
                done++;
            }
            return done;
        });
    }

    /**
     * API huỷ dòng món cho luồng quản lý/thu ngân; Quầy pha chế không expose thao tác này.
     * Chặn nếu món đã lên hoá đơn (để Cashier xử lý). Scope chi nhánh.
     * Trả mã: OK · NOT_FOUND · ALREADY_BILLED (đã lên bill) · CONFLICT (đã/đang xong hoặc khác chi nhánh).
     */
    public String cancelItem(int orderItemId, String reason, Integer userId, int sessionBranchId) throws SQLException {
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null) return "NOT_FOUND";
            String s = it.getStatus();
            // BLOCKED nằm trong danh sách huỷ được: món bị chặn (hết nguyên liệu/hỏng máy) thường
            // kết thúc bằng việc Thu ngân huỷ và hoàn tiền — thiếu nó thì món kẹt không lối thoát.
            if (!"WAITING".equals(s) && !"MAKING".equals(s) && !"BLOCKED".equals(s)) return "CONFLICT";
            if (billItemDao.existsForOrderItem(conn, orderItemId)) return "ALREADY_BILLED";   // đã lên bill → Cashier xử lý
            int rows = itemDao.updateStatusIf(conn, orderItemId, "CANCELLED",
                    new String[]{"WAITING", "MAKING", "BLOCKED"}, sessionBranchId, false, false, false, false);
            if (rows == 0) return "CONFLICT";   // đã bị đổi / khác chi nhánh
            int branchId = branchOf(it);
            String r = sanitizeReason(reason);
            // Ghi audit như mọi chuyển trạng thái khác (trước đây cancelItem là ngoại lệ thiếu log).
            actionDao.insert(conn, orderItemId, branchId, "CANCEL", s, "CANCELLED", r.isEmpty() ? null : r, userId);
            EventPublisher.publish(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(it.getOrderId()), branchId,
                    "{\"orderItemId\":" + orderItemId + ",\"status\":\"CANCELLED\""
                    + (r.isEmpty() ? "" : ",\"reason\":\"" + r + "\"")
                    + (userId == null ? "" : ",\"by\":" + userId) + "}");
            // Huỷ món cuối còn dang dở có thể khiến cả đơn kết thúc (mọi món SERVED/CANCELLED).
            completeOrderIfDone(conn, it.getOrderId(), branchId);
            return "OK";
        });
    }

    /** Làm sạch lý do huỷ để nhúng an toàn vào JSON payload (bỏ ký tự điều khiển/nháy, giới hạn độ dài). */
    private static String sanitizeReason(String reason) {
        if (reason == null) return "";
        String r = reason.replaceAll("[\\\\\"\\p{Cntrl}]", " ").trim();
        return r.length() > 120 ? r.substring(0, 120) : r;
    }

    /** READY → PICKED_UP: Thu ngân/Phục vụ xác nhận đã nhận món khỏi quầy. */
    public boolean markItemPickedUp(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return false;
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null || itemDao.pickUp(conn, orderItemId, sessionBranchId, userId) == 0) return false;
            int branchId = branchOf(it);
            actionDao.insert(conn, orderItemId, branchId, "PICK_UP", "READY", "PICKED_UP", null, userId);
            publishStatus(conn, it, "PICKED_UP");
            EventPublisher.publish(conn, EventType.ITEM_PICKED_UP, String.valueOf(orderItemId), branchId,
                    "{\"orderId\":" + it.getOrderId() + ",\"orderItemId\":" + orderItemId + ",\"by\":" + userId + "}");
            return true;
        });
    }

    /** PICKED_UP → SERVED (đóng dấu ServedAt). Chỉ role Thu ngân/Phục vụ gọi qua route riêng. */
    public boolean markItemServed(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return false;
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            int rows = itemDao.updateStatusIf(conn, orderItemId, "SERVED",
                    new String[]{"PICKED_UP"}, sessionBranchId, false, false, true, false);
            if (rows == 0) return false;   // không còn READY / khác chi nhánh
            int branchId = branchOf(it);
            actionDao.insert(conn, orderItemId, branchId, "SERVE", "PICKED_UP", "SERVED", null, userId);
            publishStatus(conn, it, "SERVED");
            completeOrderIfDone(conn, it.getOrderId(), branchId);   // giao món cuối → đơn COMPLETED
            return true;
        });
    }

    /** Nhận tất cả món READY của một đơn, không xác nhận đã giao khách. */
    public int pickUpAllReady(int orderId, Integer userId, int sessionBranchId) throws SQLException {
        if (userId == null) return 0;
        return tx(conn -> {
            int count = 0;
            for (OrderItem it : itemDao.findByOrder(conn, orderId)) {
                if (!"READY".equals(it.getStatus())) continue;
                if (itemDao.pickUp(conn, it.getOrderItemId(), sessionBranchId, userId) == 0) continue;
                int branchId = branchOf(it);
                actionDao.insert(conn, it.getOrderItemId(), branchId, "PICK_UP", "READY", "PICKED_UP", null, userId);
                publishStatus(conn, it, "PICKED_UP");
                count++;
            }
            return count;
        });
    }

    /**
     * Giao tất cả món đã được nhân viên nhận (PICKED_UP) của một đơn.
     * Mỗi món PICKED_UP→SERVED nguyên tử; món đã bị đổi/khác chi nhánh bị bỏ qua.
     */
    public int serveAllReady(int orderId, Integer userId, int sessionBranchId) throws SQLException {
        return tx(conn -> {
            int done = 0;
            Integer branchId = null;
            for (OrderItem it : itemDao.findByOrder(conn, orderId)) {
                if (!"PICKED_UP".equals(it.getStatus())) continue;
                int rows = itemDao.updateStatusIf(conn, it.getOrderItemId(), "SERVED",
                        new String[]{"PICKED_UP"}, sessionBranchId, false, false, true, false);
                if (rows == 0) continue;   // đổi bởi người khác / khác chi nhánh
                branchId = branchOf(it);
                actionDao.insert(conn, it.getOrderItemId(), branchId, "SERVE", "PICKED_UP", "SERVED", null, userId);
                publishStatus(conn, it, "SERVED");
                done++;
            }
            if (branchId != null) completeOrderIfDone(conn, orderId, branchId);
            return done;
        });
    }

    /**
     * Hoàn tác giao nhầm — SERVED → PICKED_UP (xoá ServedAt), vẫn giữ bước đã nhận khỏi quầy.
     * KHÔNG đụng ledger (kho đã trừ ở bước READY, giữ nguyên). Nếu đơn đã COMPLETED thì mở lại ACTIVE.
     * Trả true nếu hoàn tác được.
     */
    public boolean unserveItem(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            int rows = itemDao.updateStatusIf(conn, orderItemId, "PICKED_UP",
                    new String[]{"SERVED"}, sessionBranchId, false, false, false, true);   // clearServed
            if (rows == 0) return false;   // không còn SERVED / khác chi nhánh
            int branchId = branchOf(it);
            if (orderDao.reopenIfCompleted(conn, it.getOrderId()) == 1) {
                EventPublisher.publish(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(it.getOrderId()), branchId,
                        "{\"orderId\":" + it.getOrderId() + ",\"status\":\"ACTIVE\"}");
            }
            actionDao.insert(conn, orderItemId, branchId, "UNDO_SERVE", "SERVED", "PICKED_UP", null, userId);
            publishStatus(conn, it, "PICKED_UP");
            return true;
        });
    }

    /**
     * Dữ liệu màn "Sẵn sàng bàn giao": gom món READY theo đơn + toàn bộ món của các đơn đó (đối chiếu
     * đủ/đúng) trong MỘT connection (tránh N+1 mở connection theo từng đơn). Modifier nạp 1 lần/món.
     */
    public List<PickupTicket> getPickupTickets(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> ready = itemDao.findReady(conn, branchId);
            if (ready.isEmpty()) return new ArrayList<>();

            Map<Integer, List<OrderItem>> readyByOrder = new LinkedHashMap<>();
            for (OrderItem it : ready) {
                readyByOrder.computeIfAbsent(it.getOrderId(), k -> new ArrayList<>()).add(it);
            }
            List<Integer> orderIds = new ArrayList<>(readyByOrder.keySet());

            // Toàn bộ món của các đơn liên quan (1 query) + modifier (1 lần/món).
            List<OrderItem> allItems = itemDao.findByOrders(conn, orderIds);
            Map<Integer, List<OrderItem>> allByOrder = new LinkedHashMap<>();
            Map<Integer, OrderItem> byItemId = new HashMap<>();
            for (OrderItem it : allItems) {
                it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
                allByOrder.computeIfAbsent(it.getOrderId(), k -> new ArrayList<>()).add(it);
                byItemId.put(it.getOrderItemId(), it);
            }

            List<PickupTicket> tickets = new ArrayList<>();
            for (Integer oid : orderIds) {
                List<OrderItem> readyRows = readyByOrder.get(oid);
                // Dùng bản đã nạp modifier cho món READY (thay vì query lại).
                List<OrderItem> readyEnriched = new ArrayList<>();
                for (OrderItem r : readyRows) {
                    OrderItem enriched = byItemId.get(r.getOrderItemId());
                    readyEnriched.add(enriched != null ? enriched : r);
                }
                String table = readyRows.get(0).getTableNumber();
                tickets.add(new PickupTicket(oid, table, readyEnriched,
                        allByOrder.getOrDefault(oid, new ArrayList<>())));
            }
            return tickets;
        }
    }

    public Order getOrder(int orderId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Order o = orderDao.findById(conn, orderId);
            if (o == null) return null;
            List<OrderItem> items = itemDao.findByOrder(conn, orderId);
            for (OrderItem it : items) it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
            o.setItems(items);
            return o;
        }
    }

    // ---------- Order Inbox (Cashier — monitor + void) ----------

    /**
     * C4 · Đơn đang xử lý của chi nhánh (gộp COUNTER + QR, cùng bảng sales.Orders — Contract #3).
     * Đây là màn GIÁM SÁT (đơn đã tự vào KDS), KHÔNG phải cổng chặn → không đổi luồng đặt đơn.
     */
    public List<Order> getIncomingOrders(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Order> orders = orderDao.findActiveByBranch(conn, branchId);
            for (Order o : orders) {
                List<OrderItem> items = itemDao.findByOrder(conn, o.getOrderId());
                for (OrderItem it : items) it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
                o.setItems(items);
                // R3 · trạng thái thanh toán tổng đơn (suy từ các bill của phiên bàn)
                o.setPaymentStatus(o.getTableSessionId() == null ? "PAYING"
                        : paymentStatusFor(billDao.findStatusesBySession(conn, o.getTableSessionId())));
            }
            return orders;
        }
    }

    /**
     * R3 · Suy trạng thái thanh toán cấp đơn từ status các bill của phiên (ưu tiên trên xuống):
     * PAID = có bill PAID & hết UNPAID · ERROR = có bill VOID/REFUND mà chưa thu được · còn lại PAYING.
     */
    private String paymentStatusFor(List<String> billStatuses) {
        boolean paid = false, unpaid = false, err = false;
        for (String s : billStatuses) {
            if ("PAID".equals(s)) paid = true;
            else if ("UNPAID".equals(s)) unpaid = true;
            else if ("VOID".equals(s) || "REFUND".equals(s)) err = true;
        }
        if (paid && !unpaid) return "PAID";
        if (err && !paid) return "ERROR";
        return "PAYING";
    }

    /**
     * C4 · Void đơn sai — CHỈ huỷ được khi đơn còn ở giai đoạn chưa pha (mọi món WAITING).
     * Nếu barista đã nhận pha (MAKING/READY/PICKED_UP/SERVED) → trả false, KHÔNG đổi gì.
     * Khi huỷ: chuyển các món WAITING → CANCELLED + đơn → CANCELLED. KHÔNG đụng ledger.
     * Trả true nếu huỷ thành công.
     */
    public boolean voidOrder(int orderId, Integer userId) throws SQLException {
        return tx(conn -> {
            Order o = orderDao.findById(conn, orderId);
            if (o == null || !"ACTIVE".equals(o.getStatus())) return false;
            List<OrderItem> items = itemDao.findByOrder(conn, orderId);
            // guard R5: có món đã/đang pha → chặn huỷ
            for (OrderItem it : items) {
                String s = it.getStatus();
                if ("MAKING".equals(s) || "READY".equals(s) || "PICKED_UP".equals(s) || "SERVED".equals(s)) return false;
            }
            for (OrderItem it : items) {
                if ("WAITING".equals(it.getStatus())) {
                    itemDao.updateStatus(conn, it.getOrderItemId(), "CANCELLED", false, false);
                    publishStatus(conn, it, "CANCELLED");
                }
            }
            orderDao.updateStatus(conn, orderId, "CANCELLED");
            EventPublisher.publish(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(orderId), o.getBranchId(),
                    "{\"orderId\":" + orderId + ",\"status\":\"CANCELLED\"}");
            return true;
        });
    }

    public List<Order> getSessionOrders(int sessionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Order> orders = orderDao.findBySession(conn, sessionId);
            for (Order o : orders) {
                List<OrderItem> items = itemDao.findByOrder(conn, o.getOrderId());
                for (OrderItem it : items) it.setModifiers(oimDao.findByItem(conn, it.getOrderItemId()));
                o.setItems(items);
            }
            return orders;
        }
    }

    private void publishStatus(Connection conn, OrderItem it, String status) throws SQLException {
        EventPublisher.publish(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(it.getOrderId()), branchOf(it),
                "{\"orderItemId\":" + it.getOrderItemId() + ",\"status\":\"" + status + "\"}");
    }

    private static int branchOf(OrderItem it) {
        return it.getOrderBranchId() == null ? 0 : it.getOrderBranchId();
    }

    /**
     * Nếu đơn vừa hoàn tất (mọi món SERVED/CANCELLED) → ACTIVE→COMPLETED nguyên tử + publish
     * order.status_changed cấp đơn. Gọi TRONG tx của caller, ngay sau transition kết thúc một món.
     */
    private void completeOrderIfDone(Connection conn, int orderId, int branchId) throws SQLException {
        if (orderDao.completeIfAllItemsFinal(conn, orderId) == 1) {
            EventPublisher.publish(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(orderId), branchId,
                    "{\"orderId\":" + orderId + ",\"status\":\"COMPLETED\"}");
        }
    }

    private interface Tx { void run(Connection conn) throws SQLException; }
    private void txVoid(Tx tx) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { tx.run(conn); conn.commit(); }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    private interface TxFn<T> { T run(Connection conn) throws SQLException; }
    private <T> T tx(TxFn<T> fn) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { T r = fn.run(conn); conn.commit(); return r; }
            catch (SQLException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
