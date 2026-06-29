package com.cafe.service.shared;

import com.cafe.common.EventPublisher;
import com.cafe.common.EventType;
import com.cafe.config.DBConnection;
import com.cafe.dao.shared.BranchMenuDao;
import com.cafe.dao.shared.ModifierOptionDao;
import com.cafe.dao.shared.OrderDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.dao.shared.OrderItemModifierDao;
import com.cafe.model.BranchMenuItem;
import com.cafe.model.ModifierOption;
import com.cafe.model.Order;
import com.cafe.model.OrderItem;

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
                for (BranchMenuItem bm : branchMenuDao.listForBranch(conn, branchId)) {
                    priceByProduct.put(bm.getProductId(),
                            bm.getLocalPrice() != null ? bm.getLocalPrice() : bm.getBasePrice());
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

                    // gom priceDelta của các option đã chọn
                    BigDecimal unit = base;
                    Map<Integer, BigDecimal> deltaByOption = new HashMap<>();
                    for (Integer optId : line.optionIds) {
                        ModifierOption opt = optionDao.findById(conn, optId);
                        if (opt == null) continue;
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

                    for (Integer optId : line.optionIds) {
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

    /** WAITING → MAKING (đóng dấu StartedAt). */
    public void startItem(int orderItemId, Integer userId) throws SQLException {
        txVoid(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null || !"WAITING".equals(it.getStatus())) return;
            itemDao.updateStatus(conn, orderItemId, "MAKING", true, false);
            publishStatus(conn, it, "MAKING");
        });
    }

    /**
     * ★ MAKING/WAITING → READY — TRỪ TỒN modifier-aware + đổi trạng thái trong CÙNG MỘT transaction.
     * Đây là điểm auto-deduct (Contract #1, #2).
     */
    public void markItemReady(int orderItemId, Integer userId) throws SQLException {
        txVoid(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null || "READY".equals(it.getStatus()) || "SERVED".equals(it.getStatus())
                    || "CANCELLED".equals(it.getStatus())) return;
            int branchId = it.getOrderBranchId() == null ? 0 : it.getOrderBranchId();
            inventoryService.deductForOrderItem(conn, branchId, orderItemId, it.getProductId(), it.getQuantity(), userId);
            itemDao.updateStatus(conn, orderItemId, "READY", false, true);
            publishStatus(conn, it, "READY");
        });
    }

    /** B1 · Bump — đẩy món quá giờ lên đầu hàng chờ KDS. */
    public void bumpItem(int orderItemId) throws SQLException {
        txVoid(conn -> itemDao.bump(conn, orderItemId));
    }

    /** READY → SERVED. */
    public void markItemServed(int orderItemId, Integer userId) throws SQLException {
        txVoid(conn -> {
            OrderItem it = itemDao.findById(conn, orderItemId);
            if (it == null || !"READY".equals(it.getStatus())) return;
            itemDao.updateStatus(conn, orderItemId, "SERVED", false, false);
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
            }
            return orders;
        }
    }

    /**
     * C4 · Void đơn sai — huỷ đơn + huỷ các dòng CHƯA pha (WAITING/MAKING → CANCELLED).
     * KHÔNG đụng dòng đã READY/SERVED (đồ đã pha — tồn đã trừ thật, không hoàn). KHÔNG đụng ledger.
     */
    public void voidOrder(int orderId, Integer userId) throws SQLException {
        txVoid(conn -> {
            Order o = orderDao.findById(conn, orderId);
            if (o == null || !"ACTIVE".equals(o.getStatus())) return;
            for (OrderItem it : itemDao.findByOrder(conn, orderId)) {
                if ("WAITING".equals(it.getStatus()) || "MAKING".equals(it.getStatus())) {
                    itemDao.updateStatus(conn, it.getOrderItemId(), "CANCELLED", false, false);
                    publishStatus(conn, it, "CANCELLED");
                }
            }
            orderDao.updateStatus(conn, orderId, "CANCELLED");
            EventPublisher.publish(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(orderId), o.getBranchId(),
                    "{\"orderId\":" + orderId + ",\"status\":\"CANCELLED\"}");
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
}
