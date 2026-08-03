package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.*;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/** Truy vấn order/KDS/pickup; không thay đổi trạng thái nghiệp vụ. */
public final class OrderQueryService {
    private final OrderRepository repository;
    public OrderQueryService() { this(new OrderRepository()); }
    OrderQueryService(OrderRepository repository) { this.repository = Objects.requireNonNull(repository); }

    private void attachModifiers(Connection conn, List<OrderItem> items) throws SQLException {
        if (items == null || items.isEmpty()) return;
        java.util.Set<Integer> itemIds = new java.util.LinkedHashSet<>();
        for (OrderItem it : items) itemIds.add(it.getOrderItemId());
        Map<Integer, List<OrderItemModifier>> byItem = repository.oimDao.findByItems(conn, itemIds);
        for (OrderItem it : items) {
            it.setModifiers(byItem.getOrDefault(it.getOrderItemId(), List.of()));
        }
    }

    // ---------- KDS (Barista) ----------

    /** Toàn bộ dữ liệu ba cột Quầy pha chế. */
    public List<OrderItem> getBaristaWorkbench(int branchId) throws SQLException {
        return getBaristaWorkbench(branchId, null);
    }

    /** Hàng chờ của ngày kinh doanh hiện tại (null = không cắt theo ngày). */
    public List<OrderItem> getBaristaWorkbench(int branchId, java.time.LocalDateTime businessDayStartUtc)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> items = repository.itemDao.findBaristaWorkbench(conn, branchId, businessDayStartUtc);
            java.util.Set<Integer> productIds = new java.util.HashSet<>();
            for (OrderItem it : items) productIds.add(it.getProductId());
            java.util.Set<Integer> withRecipe = repository.productRecipeDao.findProductIdsWithRecipe(conn, productIds);
            attachModifiers(conn, items);
            for (OrderItem it : items) {
                it.setRecipeMissing(!withRecipe.contains(it.getProductId()));
            }
            return items;
        }
    }

    /** B2 · Món vừa giao gần đây (SERVED trong {@code minutes} phút) để hoàn tác giao nhầm. */
    public List<OrderItem> getRecentlyServed(int branchId, int minutes) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> items = repository.itemDao.findRecentlyServed(conn, branchId, minutes);
            attachModifiers(conn, items);
            return items;
        }
    }

    public List<OrderItem> getPickedUpItems(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> items = repository.itemDao.findPickedUp(conn, branchId);
            attachModifiers(conn, items);
            return items;
        }
    }

    public List<OrderItem> getTableItemStatuses(int tableId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.itemDao.findByTable(conn, tableId);
        }
    }

    /** WAITING → MAKING, khóa món bằng BaristaId trong cùng transaction. */
    public List<Recipe> getRecipeIngredients(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.productRecipeDao.findByProduct(conn, productId);
        }
    }

    /** Nguyên liệu trong công thức đang cạn tại chi nhánh — dựng modal kiểm kê khi bỏ chặn. */
    public List<Recipe> getDepletedRecipeIngredients(int branchId, int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return repository.productRecipeDao.findDepletedByProduct(conn, branchId, productId);
        }
    }

    /** READY → REMAKE → WAITING; lưu waste/ledger + audit trong cùng transaction. */
    /**
     * Dữ liệu màn "Sẵn sàng bàn giao": gom món READY theo đơn + toàn bộ món của các đơn đó (đối chiếu
     * đủ/đúng) trong MỘT connection (tránh N+1 mở connection theo từng đơn). Modifier nạp 1 lần/món.
     */
    public List<PickupTicket> getPickupTickets(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<OrderItem> ready = repository.itemDao.findReady(conn, branchId);
            if (ready.isEmpty()) return new ArrayList<>();

            Map<Integer, List<OrderItem>> readyByOrder = new LinkedHashMap<>();
            for (OrderItem it : ready) {
                readyByOrder.computeIfAbsent(it.getOrderId(), k -> new ArrayList<>()).add(it);
            }
            List<Integer> orderIds = new ArrayList<>(readyByOrder.keySet());

            // Toàn bộ món của các đơn liên quan (1 query) + modifier (1 query cho cả lô).
            List<OrderItem> allItems = repository.itemDao.findByOrders(conn, orderIds);
            attachModifiers(conn, allItems);
            Map<Integer, List<OrderItem>> allByOrder = new LinkedHashMap<>();
            Map<Integer, OrderItem> byItemId = new HashMap<>();
            for (OrderItem it : allItems) {
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

    // ---------- Order Inbox (Cashier — monitor + void) ----------

    /**
     * C4 · Đơn đang xử lý của chi nhánh (gộp COUNTER + QR, cùng bảng sales.SalesOrder — Contract #3).
     * Đây là màn GIÁM SÁT (đơn đã tự vào KDS), KHÔNG phải cổng chặn → không đổi luồng đặt đơn.
     */
    public List<Order> getIncomingOrders(int branchId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Mốc đầu ngày kinh doanh để tách đơn treo lên đầu. Quầy pha chế đã bỏ những đơn này
            // khỏi hàng chờ (khách đã về từ hôm trước), nên đây là màn duy nhất còn chốt được chúng.
            com.cafe.model.Branch branch = repository.branchDao.findById(conn, branchId);
            java.time.LocalDateTime dayStart = com.cafe.common.BusinessDay.startUtc(
                    branch == null ? null : branch.getOpenTime());
            List<Order> orders = repository.orderDao.findActiveByBranch(conn, branchId, dayStart);
            for (Order o : orders) {
                List<OrderItem> items = repository.itemDao.findByOrder(conn, o.getOrderId());
                attachModifiers(conn, items);
                o.setItems(items);
                o.setStale(o.getCreatedAt() != null && dayStart != null && o.getCreatedAt().isBefore(dayStart));
                // R3 · trạng thái thanh toán tổng đơn suy từ bill chứa các dòng của đơn.
                o.setPaymentStatus(paymentStatusFor(
                        repository.billDao.findStatusesByOrder(conn, o.getOrderId())));
            }
            return orders;
        }
    }

    /**
     * R3 · Suy trạng thái thanh toán cấp đơn từ status các bill chứa dòng đơn (ưu tiên trên xuống):
     * PAID = có bill PAID & hết UNPAID · ERROR = có bill VOID mà chưa thu được · còn lại PAYING.
     */
    private String paymentStatusFor(List<String> billStatuses) {
        boolean paid = false, unpaid = false, err = false;
        for (String s : billStatuses) {
            if ("PAID".equals(s)) paid = true;
            else if ("UNPAID".equals(s)) unpaid = true;
            else if ("VOID".equals(s)) err = true;
        }
        if (paid && !unpaid) return "PAID";
        if (err && !paid) return "ERROR";
        return "PAYING";
    }

    public List<Order> getTableOrders(int tableId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<Order> orders = repository.orderDao.findByTable(conn, tableId);
            for (Order o : orders) {
                List<OrderItem> items = repository.itemDao.findByOrder(conn, o.getOrderId());
                attachModifiers(conn, items);
                o.setItems(items);
            }
            return orders;
        }
    }


}
