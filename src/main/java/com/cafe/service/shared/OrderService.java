package com.cafe.service.shared;

import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.cashier.BillItemDao;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.ModifierGroupDao;
import com.cafe.dao.shared.ModifierOptionDao;
import com.cafe.dao.shared.OrderDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.dao.shared.OrderItemModifierDao;
import com.cafe.dao.shared.ProductModifierGroupDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.ModifierGroup;
import com.cafe.model.ModifierOption;
import com.cafe.model.Order;
import com.cafe.model.OrderItem;
import com.cafe.model.ProductModifierGroup;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lõi Sales — Cashier SỞ HỮU mọi order entry (đơn quầy & QR cùng một service).
 * Contract #1: order.created + status enum chung; #3: cùng bảng sales.Orders.
 */
public class OrderService {

    private final OrderDao orderDao = new OrderDao();
    private final OrderItemDao itemDao = new OrderItemDao();
    private final OrderItemModifierDao oimDao = new OrderItemModifierDao();
    private final BranchMenuDao branchMenuDao = new BranchMenuDao();
    private final ModifierOptionDao optionDao = new ModifierOptionDao();
    private final ModifierGroupDao groupDao = new ModifierGroupDao();
    private final ProductModifierGroupDao pmgDao = new ProductModifierGroupDao();
    private final BillDao billDao = new BillDao();
    private final BillItemDao billItemDao = new BillItemDao();
    private final InventoryService inventoryService = new InventoryService();

    /** Một dòng giỏ hàng từ POS/QR. */
    public static class CartLine {
        public int productId;
        public int quantity;
        public String note;
        public List<Integer> optionIds = new ArrayList<>();
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

                    // gom priceDelta của các option đã chọn
                    BigDecimal unit = base;
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
                    it.setNote(line.note);
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

    private List<Integer> validateOptions(Connection conn, int productId, List<Integer> optionIds) throws SQLException {
        List<Integer> selected = optionIds == null ? List.of() : optionIds;
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

    public List<OrderItem> getSessionItemStatuses(int sessionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return itemDao.findBySession(conn, sessionId);
        }
    }

    /** WAITING → MAKING (đóng dấu StartedAt). Nguyên tử + scope chi nhánh. */
    public void startItem(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        txVoid(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null) return;
            int rows = itemDao.updateStatusIf(conn, orderItemId, "MAKING",
                    new String[]{"WAITING"}, sessionBranchId, true, false);
            if (rows == 0) return;   // sai trạng thái / khác chi nhánh / đã bị đổi
            publishStatus(conn, it, "MAKING");
        });
    }

    /**
     * ★ MAKING/WAITING → READY — TRỪ TỒN modifier-aware + đổi trạng thái trong CÙNG MỘT transaction.
     * Đây là điểm auto-deduct (Contract #1, #2). Chốt trạng thái NGUYÊN TỬ (updateStatusIf) TRƯỚC khi
     * trừ kho: chỉ request "claim" được món (rows==1) mới trừ → chống double-deduct khi 2 barista bấm
     * song song; scope chi nhánh chặn thao tác chéo chi nhánh.
     */
    public void markItemReady(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        txVoid(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null) return;
            int rows = itemDao.updateStatusIf(conn, orderItemId, "READY",
                    new String[]{"WAITING", "MAKING"}, sessionBranchId, false, true);
            if (rows == 0) return;   // đã READY/SERVED/CANCELLED / khác chi nhánh → không trừ kho
            int branchId = it.getOrderBranchId() == null ? 0 : it.getOrderBranchId();
            inventoryService.deductForOrderItem(conn, branchId, orderItemId, it.getProductId(), it.getQuantity(), userId);
            publishStatus(conn, it, "READY");
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
        return tx(conn -> {
            int done = 0;
            for (OrderItem it : itemDao.findByOrder(conn, orderId)) {
                String s = it.getStatus();
                if (!"WAITING".equals(s) && !"MAKING".equals(s)) continue;
                int rows = itemDao.updateStatusIf(conn, it.getOrderItemId(), "READY",
                        new String[]{"WAITING", "MAKING"}, sessionBranchId, false, true);
                if (rows == 0) continue;   // đổi bởi người khác / khác chi nhánh
                int branchId = it.getOrderBranchId() == null ? 0 : it.getOrderBranchId();
                inventoryService.deductForOrderItem(conn, branchId, it.getOrderItemId(),
                        it.getProductId(), it.getQuantity(), userId);
                publishStatus(conn, it, "READY");
                done++;
            }
            return done;
        });
    }

    /**
     * B1 · "Không pha được" — huỷ ĐÚNG một món còn WAITING/MAKING (Barista báo hết nguyên liệu).
     * Giống voidOrder nhưng cấp món: WAITING/MAKING → CANCELLED, KHÔNG đụng ledger.
     * Chặn nếu món đã lên hoá đơn (để Cashier xử lý). Scope chi nhánh. Trả true nếu huỷ được.
     */
    public boolean cancelItem(int orderItemId, String reason, Integer userId, int sessionBranchId) throws SQLException {
        return tx(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null) return false;
            String s = it.getStatus();
            if (!"WAITING".equals(s) && !"MAKING".equals(s)) return false;        // đã/đang xong → không huỷ lẻ
            if (billItemDao.existsForOrderItem(conn, orderItemId)) return false;   // đã lên bill → Cashier xử lý
            int rows = itemDao.updateStatusIf(conn, orderItemId, "CANCELLED",
                    new String[]{"WAITING", "MAKING"}, sessionBranchId, false, false);
            if (rows == 0) return false;   // đã bị đổi / khác chi nhánh
            int branchId = it.getOrderBranchId() == null ? 0 : it.getOrderBranchId();
            String r = sanitizeReason(reason);
            EventPublisher.publish(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(it.getOrderId()), branchId,
                    "{\"orderItemId\":" + orderItemId + ",\"status\":\"CANCELLED\""
                    + (r.isEmpty() ? "" : ",\"reason\":\"" + r + "\"")
                    + (userId == null ? "" : ",\"by\":" + userId) + "}");
            return true;
        });
    }

    /** Làm sạch lý do huỷ để nhúng an toàn vào JSON payload (bỏ ký tự điều khiển/nháy, giới hạn độ dài). */
    private static String sanitizeReason(String reason) {
        if (reason == null) return "";
        String r = reason.replaceAll("[\\\\\"\\p{Cntrl}]", " ").trim();
        return r.length() > 120 ? r.substring(0, 120) : r;
    }

    /** READY → SERVED. Nguyên tử + scope chi nhánh. */
    public void markItemServed(int orderItemId, Integer userId, int sessionBranchId) throws SQLException {
        txVoid(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null) return;
            int rows = itemDao.updateStatusIf(conn, orderItemId, "SERVED",
                    new String[]{"READY"}, sessionBranchId, false, false);
            if (rows == 0) return;   // không còn READY / khác chi nhánh
            publishStatus(conn, it, "SERVED");
        });
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
     * R5: nếu barista đã nhận pha (có món MAKING/READY/SERVED) → trả false, KHÔNG đổi gì.
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
                if ("MAKING".equals(s) || "READY".equals(s) || "SERVED".equals(s)) return false;
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
        int branchId = it.getOrderBranchId() == null ? 0 : it.getOrderBranchId();
        EventPublisher.publish(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(it.getOrderId()), branchId,
                "{\"orderItemId\":" + it.getOrderItemId() + ",\"status\":\"" + status + "\"}");
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
