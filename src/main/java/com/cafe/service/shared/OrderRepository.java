package com.cafe.service.shared;

import com.cafe.dao.shared.ActivityLogDao;
import com.cafe.dao.cashier.BillDao;
import com.cafe.dao.cashier.BillLineDao;
import com.cafe.dao.admin.BranchDao;
import com.cafe.dao.admin.BranchMenuDao;
import com.cafe.dao.admin.ModifierGroupDao;
import com.cafe.dao.admin.ModifierOptionDao;
import com.cafe.dao.shared.OrderDao;
import com.cafe.dao.shared.OrderItemDao;
import com.cafe.dao.barista.OrderItemIssueDao;
import com.cafe.dao.shared.OrderItemModifierDao;
import com.cafe.dao.shared.OrderItemQueryDao;
import com.cafe.dao.barista.OrderItemWorkflowDao;
import com.cafe.dao.shared.OutboxEventDao;
import com.cafe.dao.admin.ProductModifierGroupDao;
import com.cafe.dao.admin.RecipeDao;
import com.cafe.common.*;
import com.cafe.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

final class OrderRepository {

    final OrderDao orderDao;
    final OrderItemDao itemDao;
    final OrderItemQueryDao itemQueryDao;
    final OrderItemWorkflowDao itemWorkflowDao;
    final OrderItemIssueDao itemIssueDao;
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
        this(new OrderDao(), new OrderItemDao(), new OrderItemQueryDao(), new OrderItemWorkflowDao(),
                new OrderItemIssueDao(), new ActivityLogDao(), new OrderItemModifierDao(),
                new BranchMenuDao(), new ModifierOptionDao(), new ModifierGroupDao(),
                new ProductModifierGroupDao(), new BillDao(), new BillLineDao(), new RecipeDao(),
                new OutboxEventDao(), new BranchDao(), new InventoryService());
    }

    OrderRepository(OrderDao orderDao, OrderItemDao itemDao, OrderItemQueryDao itemQueryDao,
                    OrderItemWorkflowDao itemWorkflowDao, OrderItemIssueDao itemIssueDao,
                    ActivityLogDao activityLogDao,
                    OrderItemModifierDao oimDao, BranchMenuDao branchMenuDao, ModifierOptionDao optionDao,
                    ModifierGroupDao groupDao, ProductModifierGroupDao pmgDao, BillDao billDao,
                    BillLineDao billLineDao, RecipeDao productRecipeDao,
                    OutboxEventDao outboxEventDao, BranchDao branchDao, InventoryService inventoryService) {
        this.orderDao = Objects.requireNonNull(orderDao);
        this.itemDao = Objects.requireNonNull(itemDao);
        this.itemQueryDao = Objects.requireNonNull(itemQueryDao);
        this.itemWorkflowDao = Objects.requireNonNull(itemWorkflowDao);
        this.itemIssueDao = Objects.requireNonNull(itemIssueDao);
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

    /** Như {@link #tx} nhưng không trả kết quả. */
    void txVoid(Tx tx) throws SQLException {
        com.cafe.config.Tx.run(tx::run);
    }

    interface TxFn<T> { T run(Connection conn) throws SQLException; }

    /**
     * Chạy {@code fn} trong một giao dịch — nay chỉ là lối vào tiện tay cho các service đã cầm sẵn
     * repository. Toàn bộ luật (commit/rollback, rollback cả RuntimeException, thử lại khi trúng
     * deadlock 1205) nằm ở {@link com.cafe.config.Tx} để mọi service dùng chung một bản.
     */
    <T> T tx(TxFn<T> fn) throws SQLException {
        return com.cafe.config.Tx.call(fn::run);
    }
}
