package com.cafe.service.shared;

import com.cafe.common.*;
import com.cafe.config.DBConnection;
import com.cafe.dao.cashier.*;
import com.cafe.dao.shared.*;
import com.cafe.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

final class OrderRepository {
    final OrderDao orderDao;
    final OrderItemDao itemDao;
    final ActivityLogDao activityLogDao;
    final OrderItemModifierDao oimDao;
    final BranchMenuDao branchMenuDao;
    final ModifierOptionDao optionDao;
    final ModifierGroupDao groupDao;
    final ProductModifierGroupDao pmgDao;
    final BillDao billDao;
    final BillLineDao billLineDao;
    final RecipeDao productRecipeDao;
    final OutboxEventDao outboxEventDao;
    final BranchDao branchDao;
    final InventoryService inventoryService;

    OrderRepository() {
        this(new OrderDao(), new OrderItemDao(), new ActivityLogDao(), new OrderItemModifierDao(),
                new BranchMenuDao(), new ModifierOptionDao(), new ModifierGroupDao(),
                new ProductModifierGroupDao(), new BillDao(), new BillLineDao(), new RecipeDao(),
                new OutboxEventDao(), new BranchDao(), new InventoryService());
    }

    OrderRepository(OrderDao orderDao, OrderItemDao itemDao, ActivityLogDao activityLogDao,
                    OrderItemModifierDao oimDao, BranchMenuDao branchMenuDao, ModifierOptionDao optionDao,
                    ModifierGroupDao groupDao, ProductModifierGroupDao pmgDao, BillDao billDao,
                    BillLineDao billLineDao, RecipeDao productRecipeDao,
                    OutboxEventDao outboxEventDao, BranchDao branchDao, InventoryService inventoryService) {
        this.orderDao = Objects.requireNonNull(orderDao);
        this.itemDao = Objects.requireNonNull(itemDao);
        this.activityLogDao = Objects.requireNonNull(activityLogDao);
        this.oimDao = Objects.requireNonNull(oimDao);
        this.branchMenuDao = Objects.requireNonNull(branchMenuDao);
        this.optionDao = Objects.requireNonNull(optionDao);
        this.groupDao = Objects.requireNonNull(groupDao);
        this.pmgDao = Objects.requireNonNull(pmgDao);
        this.billDao = Objects.requireNonNull(billDao);
        this.billLineDao = Objects.requireNonNull(billLineDao);
        this.productRecipeDao = Objects.requireNonNull(productRecipeDao);
        this.outboxEventDao = Objects.requireNonNull(outboxEventDao);
        this.branchDao = Objects.requireNonNull(branchDao);
        this.inventoryService = Objects.requireNonNull(inventoryService);
    }

    void publishStatus(Connection conn, OrderItem it, String status) throws SQLException {
        outboxEventDao.insert(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(it.getOrderId()), branchOf(it),
                "{\"orderItemId\":" + it.getOrderItemId() + ",\"status\":\"" + status + "\"}");
    }

    static int branchOf(OrderItem it) {
        return it.getOrderBranchId() == null ? 0 : it.getOrderBranchId();
    }

    /**
     * Nếu đơn vừa hoàn tất (mọi món SERVED/CANCELLED) → ACTIVE→COMPLETED nguyên tử + publish
     * order.status_changed cấp đơn. Gọi TRONG tx của caller, ngay sau transition kết thúc một món.
     */
    void completeOrderIfDone(Connection conn, int orderId, int branchId) throws SQLException {
        if (orderDao.completeIfAllItemsFinal(conn, orderId) == 1) {
            outboxEventDao.insert(conn, EventType.ORDER_STATUS_CHANGED, String.valueOf(orderId), branchId,
                    "{\"orderId\":" + orderId + ",\"status\":\"COMPLETED\"}");
        }
    }

    interface Tx { void run(Connection conn) throws SQLException; }

    /**
     * BusinessException/IllegalArgumentException là RuntimeException nên PHẢI rollback cùng chỗ với
     * SQLException: bỏ sót nó thì {@code setAutoCommit(true)} ở finally lại commit phần đã ghi dở
     * (hợp đồng JDBC: đổi auto-commit mode giữa transaction sẽ commit transaction đó). Trước đây
     * món chưa có công thức vẫn sang READY rồi mới ném lỗi ở bước trừ kho → READY mà không trừ kho.
     */
    void txVoid(Tx tx) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { tx.run(conn); conn.commit(); }
            catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }

    interface TxFn<T> { T run(Connection conn) throws SQLException; }

    /** Như {@link #txVoid} nhưng trả kết quả — rollback cả RuntimeException, xem ghi chú ở đó. */
    <T> T tx(TxFn<T> fn) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try { T r = fn.run(conn); conn.commit(); return r; }
            catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        }
    }
}
