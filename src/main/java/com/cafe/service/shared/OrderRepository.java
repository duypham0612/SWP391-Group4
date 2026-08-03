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
import java.util.concurrent.ThreadLocalRandom;

final class OrderRepository {

    /** Mã lỗi SQL Server cho "giao dịch bị chọn làm nạn nhân deadlock". */
    private static final int SQL_SERVER_DEADLOCK_VICTIM = 1205;
    private static final int TX_MAX_ATTEMPTS = 3;

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

    /** Như {@link #tx} nhưng không trả kết quả. */
    void txVoid(Tx tx) throws SQLException {
        tx(conn -> { tx.run(conn); return null; });
    }

    interface TxFn<T> { T run(Connection conn) throws SQLException; }

    /**
     * Chạy {@code fn} trong một giao dịch, commit nếu xuôi và rollback nếu ném lỗi.
     *
     * <p>BusinessException/IllegalArgumentException là RuntimeException nên PHẢI rollback cùng chỗ
     * với SQLException: bỏ sót nó thì {@code setAutoCommit(true)} ở finally lại commit phần đã ghi
     * dở (hợp đồng JDBC: đổi auto-commit mode giữa transaction sẽ commit transaction đó). Trước đây
     * món chưa có công thức vẫn sang READY rồi mới ném lỗi ở bước trừ kho → READY mà không trừ kho.
     *
     * <p>Riêng nạn nhân deadlock thì chạy lại — xem {@link #isDeadlockVictim}.
     */
    <T> T tx(TxFn<T> fn) throws SQLException {
        for (int attempt = 1; ; attempt++) {
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try { T r = fn.run(conn); conn.commit(); return r; }
                catch (SQLException | RuntimeException e) {
                    conn.rollback();
                    if (attempt >= TX_MAX_ATTEMPTS || !isDeadlockVictim(e)) throw e;
                }
                finally { conn.setAutoCommit(true); }
            }
            // Chỉ tới đây khi là nạn nhân deadlock và còn lượt: nghỉ ngắn rồi chạy lại TỪ ĐẦU
            // với connection mới.
            backOff(attempt);
        }
    }

    /**
     * Chạy lại cả giao dịch khi SQL Server chọn nó làm nạn nhân deadlock (lỗi 1205).
     *
     * <p>Phải chạy lại từ đầu bằng connection mới, KHÔNG thử lại trong cùng connection như vòng lặp
     * cấp mã pickup ở {@code OrderDao.insert}: 1205 huỷ nguyên giao dịch chứ không chỉ câu lệnh, nên
     * mọi thao tác đã ghi trước đó trong giao dịch cũng mất theo. Vòng lặp cũ chỉ bắt trùng khoá
     * (2601/2627) — lỗi cấp câu lệnh, giao dịch còn sống nên thử lại tại chỗ mới hợp lệ.
     *
     * <p>Chạy lại an toàn vì mọi {@code fn} đều dựng lại trạng thái của nó bên trong lambda và
     * rollback đã xoá sạch phần ghi dở. Đường sinh deadlock đã biết: cấp mã pickup quét dải
     * {@code SELECT MAX(...)} lấy khoá S rồi INSERT nâng lên X.
     */
    private static boolean isDeadlockVictim(Exception error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                for (SQLException s = sql; s != null; s = s.getNextException()) {
                    if (s.getErrorCode() == SQL_SERVER_DEADLOCK_VICTIM) return true;
                }
            }
        }
        return false;
    }

    /** Nghỉ lệch nhau giữa các luồng để lượt sau không va lại đúng như lượt trước. */
    private static void backOff(int attempt) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(10L * attempt, 40L * attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
